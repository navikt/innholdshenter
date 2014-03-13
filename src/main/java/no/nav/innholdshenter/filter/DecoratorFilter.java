package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class DecoratorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFilter.class);

    private EnonicContentRetriever contentRetriever;
    private List<String> fragmentNames;
    private String fragmentsUrl;
    private List<String> includeContentTypes;
    private String applicationName;
    private String subMenuPath;
    private boolean shouldIncludeActiveItemInUrl;
    private List<String> noDecoratePatterns;
    private List<String> noSubmenuPatterns;
    private Map<String, String> excludeHeaders;

    public DecoratorFilter() {
        fragmentNames = new ArrayList<String>();
        noDecoratePatterns = new ArrayList<String>();
        noSubmenuPatterns = new ArrayList<String>();
        setDefaultIncludeContentTypes();
        setDefaultExcludeHeaders();
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

        if (!shouldDecorateRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        DecoratorResponseWrapper responseWrapper = new DecoratorResponseWrapper(response);
        chain.doFilter(request, responseWrapper);
        responseWrapper.flushBuffer();

        String originalResponse = responseWrapper.getOutputAsString();
        if (shouldHandleContentType(responseWrapper.getContentType())) {
            String result = mergeWithFragments(originalResponse, request);
            try {
                response.getWriter().write(result);
            } catch (IllegalStateException getOutputStreamAlreadyCalled) {
                response.getOutputStream().write(result.getBytes());
            }
        } else {
            response.getWriter().write(originalResponse);
        }
    }

    private boolean shouldDecorateRequest(HttpServletRequest request) {
        return !(requestUriMatchesNoDecoratePattern(request) || requestHeaderHasExcludeValue(request));
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
        String responseString = originalResponseString;
        Document htmlFragments = getHtmlFragments(request, originalResponseString);

        for (String fragmentName : fragmentNames) {
            Element element = htmlFragments.getElementById(fragmentName);
            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(request, responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }
        return responseString;
    }

    private boolean isFragmentSubmenu(String fragmentName) {
        return "submenu".equals(fragmentName);
    }

    private String mergeSubmenuFragment(HttpServletRequest request, String responseString, String fragmentName, Element element) {
        if (!requestUriMatchesNoSubmenuPattern(request.getRequestURI())) {
            responseString = mergeFragment(responseString, fragmentName, element.html());
        } else {
            responseString = mergeFragment(responseString, fragmentName, "");
        }
        return responseString;
    }

    private String mergeFragment(String responseString, String fragmentName, String elementMarkup) {
        return responseString.replace(String.format("${%s}", fragmentName), elementMarkup);
    }

    private Document getHtmlFragments(HttpServletRequest request, String originalResponse) {
        String url = null;
        try {
            url = buildUrl(request, originalResponse);
        } catch (URISyntaxException e) {
            logger.warn("Exception when building URL", e);
        }

        String pageContent = contentRetriever.getPageContent(url);
        return Jsoup.parse(pageContent);
    }

    private String buildUrl(HttpServletRequest request, String originalResponse) throws URISyntaxException {
        URIBuilder urlBuilder = new URIBuilder(fragmentsUrl);

        if (applicationName != null) {
            urlBuilder.addParameter("appname", applicationName);
        }

        if (shouldIncludeActiveItemInUrl) {
            urlBuilder.addParameter("activeitem", request.getRequestURI());
        }

        String role = extractMetaTag(originalResponse, "Brukerstatus");
        if (!isEmpty(role)) {
            urlBuilder.addParameter("userrole", role);
        }

        for (String fragmentName : fragmentNames) {
            if (isFragmentSubmenu(fragmentName)) {
                urlBuilder.addParameter("submenu", subMenuPath);
            } else {
                urlBuilder.addParameter(fragmentName, "true");
            }
        }

        return urlBuilder.build().toString();
    }

    private String extractMetaTag(String originalResponse, String tag) {
        Document doc = Jsoup.parse(originalResponse);
        Elements brukerStatus = doc.select(String.format("meta[name=%s]", tag));

        if (!brukerStatus.isEmpty()) {
            return brukerStatus.attr("content");
        } else {
            return null;
        }
    }

    private boolean requestUriMatchesNoSubmenuPattern(String requestUri) {
        for (String noSubmenuPattern : noSubmenuPatterns) {
            Matcher matcher = createMatcher(noSubmenuPattern, requestUri);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    private Matcher createMatcher(String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return pattern.matcher(content);
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

    public void setShouldIncludeActiveItemInUrl(boolean shouldIncludeActiveItemInUrl) {
        this.shouldIncludeActiveItemInUrl = shouldIncludeActiveItemInUrl;
    }

    public void setNoDecoratePatterns(List<String> noDecoratePatterns) {
        this.noDecoratePatterns = noDecoratePatterns;
    }

    public void setNoSubmenuPatterns(List<String> noSubmenuPatterns) {
        this.noSubmenuPatterns = noSubmenuPatterns;
    }
}
