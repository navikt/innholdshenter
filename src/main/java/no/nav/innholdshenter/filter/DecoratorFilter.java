package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static java.util.Arrays.asList;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.createMatcher;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.isFragmentSubmenu;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.removePlaceholders;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class DecoratorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFilter.class);

    public static final String ALREADY_DECORATED_HEADER = "X-NAV-decorator";
    private static final List<String> DEFAULT_NO_DECORATE_PATTERNS = asList(".*isAlive.*");

    private EnonicContentRetriever contentRetriever;
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

    public DecoratorFilter() {
        fragmentNames = new ArrayList<String>();
        noDecoratePatterns = new ArrayList<String>(DEFAULT_NO_DECORATE_PATTERNS);
        noSubmenuPatterns = new ArrayList<String>();
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
        includeContentTypes = new ArrayList<String>();
        includeContentTypes.add("text/html");
        includeContentTypes.add("text/html; charset=UTF-8");
        includeContentTypes.add("application/xhtml+xml");
    }

    private void setDefaultExcludeHeaders() {
        excludeHeaders = new HashMap<String, String>();
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

        if (!shouldHandleContentType(responseWrapper.getContentType()) || isEmpty(originalResponseString)) {
            logger.debug("Should not handle content type, or original response string is empty: {}", responseWrapper.getContentType());
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
            response.getOutputStream().write(transformedOutput.getBytes(characterEncoding));
        } catch (IllegalStateException getWriterAlreadyCalled) {
            response.getWriter().write(transformedOutput);
        }
    }

    private void writeOriginalOutputToResponse(DecoratorResponseWrapper responseWrapper, HttpServletResponse response) throws IOException {
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

    private String mergeWithFragments(String originalResponseString, HttpServletRequest request) {
        FragmentFetcher fragmentFetcher = new FragmentFetcher(contentRetriever, fragmentsUrl, applicationName, shouldIncludeActiveItem, subMenuPath, fragmentNames, request, originalResponseString, extendedConfiguration);
        Document htmlFragments = fragmentFetcher.fetchHtmlFragments();
        MarkupMerger markupMerger = new MarkupMerger(fragmentNames, noSubmenuPatterns, originalResponseString, htmlFragments, request);
        return markupMerger.merge();
    }

    @Override
    public void destroy() {
    }

    @Required
    public void setFragmentsUrl(String fragmentsUrl) {
        this.fragmentsUrl = fragmentsUrl;
    }

    @Required
    public void setContentRetriever(EnonicContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Required
    public void setFragmentNames(List<String> fragmentNames) {
        this.fragmentNames = fragmentNames;
    }

    @Required
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setSubMenuPath(String subMenuPath) {
        this.subMenuPath = subMenuPath;
    }

    public void setShouldIncludeActiveItem(boolean shouldIncludeActiveItem) {
        this.shouldIncludeActiveItem = shouldIncludeActiveItem;
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
}
