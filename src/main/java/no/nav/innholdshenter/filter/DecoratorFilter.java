package no.nav.innholdshenter.filter;

import net.sf.ehcache.CacheException;
import no.nav.innholdshenter.common.ContentRetriever;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static java.util.Arrays.asList;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class DecoratorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFilter.class);

    public static final String ALREADY_DECORATED_HEADER = "X-NAV-decorator";
    private static final List<String> DEFAULT_NO_DECORATE_PATTERNS = asList(".*isAlive.*");

    private ContentRetriever contentRetriever;
    private List<String> fragmentNames;
    private String fragmentsUrl;
    private List<String> includeContentTypes;
    private String applicationName;
    private String subMenuPath;
    private boolean shouldIncludeActiveItem;
    private List<String> noDecoratePatterns;
    private List<String> noSubmenuPatterns;
    private Map<String, String> excludeHeaders;
    private ExtendedConfiguration extendedConfiguration;
    private Map<String, String> additionalOptions;

    @Deprecated
    /* Vi tar bort denne for A tvinge at feltene settes i konstruktoeren. Da kan vi dra ut hele Spring fra prosjeketet */
    public DecoratorFilter() {
        fragmentNames = new ArrayList<>();
        noDecoratePatterns = new ArrayList<>(DEFAULT_NO_DECORATE_PATTERNS);
        noSubmenuPatterns = new ArrayList<>();
        additionalOptions = new HashMap<>();
        setDefaultIncludeContentTypes();
        setDefaultExcludeHeaders();
    }

    public DecoratorFilter(String fragmentsUrl, ContentRetriever contentRetriever, List<String> fragmentNames, String applicationName) {
        if (fragmentsUrl == null || contentRetriever == null || fragmentNames == null || applicationName == null) {
            throw new IllegalArgumentException("Alle argumentene er paakrevd!");
        }

        noDecoratePatterns = new ArrayList<>(DEFAULT_NO_DECORATE_PATTERNS);
        noSubmenuPatterns = new ArrayList<>();
        additionalOptions = new HashMap<>();

        this.fragmentNames = fragmentNames;
        this.fragmentsUrl = fragmentsUrl;
        this.contentRetriever = contentRetriever;
        this.applicationName = applicationName;
        setDefaultIncludeContentTypes();
        setDefaultExcludeHeaders();
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void validateConfiguration() {
        if (isSubmenuFragmentDefined() && subMenuPath == null && extendedConfiguration == null) {
            throw new IllegalArgumentException("subMenuPath kan ikke være null når submenu er definert som fragment");
        }
    }

    private boolean isSubmenuFragmentDefined() {
        for (String fragmentName : fragmentNames) {
            if (isFragmentSubmenu(fragmentName)) {
                return true;
            }
        }
        return false;
    }

    private void setDefaultIncludeContentTypes() {
        includeContentTypes = new ArrayList<>();
        includeContentTypes.add("text/html");
        includeContentTypes.add("text/html; charset=UTF-8");
        includeContentTypes.add("application/xhtml+xml");
    }

    private void setDefaultExcludeHeaders() {
        excludeHeaders = new HashMap<>();
        excludeHeaders.put("X-Requested-With", "XMLHttpRequest");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        DecoratorResponseWrapper responseWrapper = new DecoratorResponseWrapper(response);
        chain.doFilter(request, responseWrapper);
        responseWrapper.flushBuffer();
        String originalResponseString = responseWrapper.getOutputAsString();

        if (!shouldHandleContentType(responseWrapper.getContentType()) || !hasAppropriateStatusCode(response.getStatus()) || isEmpty(originalResponseString)) {
            logger.debug("Should not handle content type: {}, status code: {}, or original response string is empty.", responseWrapper.getContentType(), response.getStatus());
            writeOriginalOutputToResponse(responseWrapper, response);
        } else if (!shouldDecorateRequest(request)) {
            logger.debug("Should not decorate response for request: {}", request.getRequestURI());
            writeToResponse(removePlaceholders(originalResponseString, fragmentNames), response);
        } else {
            logger.debug("Merging response with fragments for request: {}", request.getRequestURI());
            String mergedResponseString = mergeWithFragments(originalResponseString, request);
            markRequestAsDecorated(request);
            writeToResponse(mergedResponseString, response);
        }
    }

    private void writeToResponse(String transformedOutput, HttpServletResponse response) throws IOException {
        String characterEncoding = response.getCharacterEncoding();
        try {
            byte[] transformedOutputAsBytes = transformedOutput.getBytes(characterEncoding);
            response.setContentLength(transformedOutputAsBytes.length);
            response.getOutputStream().write(transformedOutputAsBytes);
        } catch (IllegalStateException getWriterAlreadyCalled) {
            response.getWriter().write(transformedOutput);
        }
    }

    private void writeOriginalOutputToResponse(DecoratorResponseWrapper responseWrapper, HttpServletResponse response) throws IOException {
        if (response.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
            return;
        }

        try {
            response.getOutputStream().write(responseWrapper.getOutputAsByteArray());
        } catch (IllegalStateException getWriterHasAlreadyBeenCalled) {
            response.getWriter().print(responseWrapper.getOutputAsString());
        }
    }

    private boolean shouldDecorateRequest(HttpServletRequest request) {
        return !(requestUriMatchesNoDecoratePattern(request) || requestHeaderHasExcludeValue(request) || filterAlreadyAppliedForRequest(request));
    }

    private boolean requestUriMatchesNoDecoratePattern(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        for (String noDecoratePattern : noDecoratePatterns) {
            Matcher matcher = createMatcher(noDecoratePattern, requestUri);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean requestHeaderHasExcludeValue(HttpServletRequest request) {
        for (Map.Entry<String, String> entry : excludeHeaders.entrySet()) {
            if (requestHeaderHasValue(request, entry.getKey(), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean requestHeaderHasValue(HttpServletRequest request, String header, String value) {
        return (request.getHeader(header) != null) && request.getHeader(header).equalsIgnoreCase(value);
    }

    private boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
        return request.getAttribute(ALREADY_DECORATED_HEADER) == Boolean.TRUE;
    }

    private void markRequestAsDecorated(HttpServletRequest request) {
        request.setAttribute(ALREADY_DECORATED_HEADER, Boolean.TRUE);
    }

    private boolean shouldHandleContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        for (String includeContentType : includeContentTypes) {
            if (contentType.toLowerCase().contains(includeContentType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAppropriateStatusCode(int statusCode) {
        int errorZone = statusCode / 100;

        return statusCode >= 0 && errorZone != 3;
    }

    private String mergeWithFragments(String originalResponseString, HttpServletRequest request) {
        FragmentFetcher fragmentFetcher = new FragmentFetcher(contentRetriever, fragmentsUrl, applicationName, shouldIncludeActiveItem, subMenuPath, fragmentNames, additionalOptions,
                request, originalResponseString, extendedConfiguration);
        Document htmlFragments;
        try {
            htmlFragments = fragmentFetcher.fetchHtmlFragments();
        } catch (CacheException e) {
            logger.warn("Klarte ikke å hente HTML fragment. Returnerer tom streng", e);
            htmlFragments = Jsoup.parse(StringUtils.EMPTY);
        }
        MarkupMerger markupMerger = new MarkupMerger(fragmentNames, noSubmenuPatterns, originalResponseString, htmlFragments, request, applicationName);
        return markupMerger.merge();
    }

    @Override
    public void destroy() {
    }

    public void setFragmentsUrl(String fragmentsUrl) {
        this.fragmentsUrl = fragmentsUrl;
    }

    public void setContentRetriever(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    public void setFragmentNames(List<String> fragmentNames) {
        this.fragmentNames = fragmentNames;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setSubMenuPath(String subMenuPath) {
        this.subMenuPath = subMenuPath;
    }

    public void setShouldIncludeActiveItem() {
        this.shouldIncludeActiveItem = true;
    }

    public void setNoDecoratePatterns(List<String> noDecoratePatterns) {
        this.noDecoratePatterns = noDecoratePatterns;
        this.noDecoratePatterns.addAll(DEFAULT_NO_DECORATE_PATTERNS);
    }

    public void setNoSubmenuPatterns(List<String> noSubmenuPatterns) {
        this.noSubmenuPatterns = noSubmenuPatterns;
    }

    public List<String> getNoDecoratePatterns() {
        return noDecoratePatterns;
    }

    public void setExtendedConfiguration(ExtendedConfiguration extendedConfiguration) {
        this.extendedConfiguration = extendedConfiguration;
    }

    public void setAdditionalOptions(Map<String, String> additionalOptions) {
        this.additionalOptions = additionalOptions;
    }
}
