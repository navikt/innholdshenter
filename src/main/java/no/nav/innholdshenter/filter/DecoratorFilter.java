package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import java.util.List;

public class DecoratorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFilter.class);

    private EnonicContentRetriever contentRetriever;
    private List<String> fragmentNames;
    private String baseUrl;
    private List<String> includeContentTypes;
    private String applicationName;
    private String subMenuPath;

    public DecoratorFilter() {
        fragmentNames = new ArrayList<String>();
        setDefaultIncludeContentTypes();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    private void setDefaultIncludeContentTypes() {
        includeContentTypes = new ArrayList<String>();
        includeContentTypes.add("text/html");
        includeContentTypes.add("text/html; charset=UTF-8");
        includeContentTypes.add("application/xhtml+xml");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        DecoratorResponseWrapper responseWrapper = new DecoratorResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);
        responseWrapper.flushBuffer();

        String originalResponseString = responseWrapper.getOutputAsString();
        if (shouldHandleContentType(responseWrapper.getContentType())) {
            response.getWriter().write(mergeWithFragments(originalResponseString));
        } else {
            response.getWriter().write(originalResponseString);
        }
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

    private String mergeWithFragments(String originalResponseString) {
        String responseString = originalResponseString;
        Document htmlFragments = getHtmlFragments();

        for (String fragmentName : fragmentNames) {
            Element element = htmlFragments.getElementById(fragmentName);
            responseString = responseString.replace(String.format("${%s}", fragmentName), element.toString());
        }

        return responseString;
    }

    private Document getHtmlFragments() {
        String url = null;
        try {
            url = buildUrl();
        } catch (URISyntaxException e) {
            logger.warn("Exception when building URL", e);
        }

        String pageContent = contentRetriever.getPageContent(url);
        return Jsoup.parse(pageContent);
    }

    private String buildUrl() throws URISyntaxException {
        URIBuilder urlBuilder = new URIBuilder(baseUrl);

        if (applicationName != null) {
            urlBuilder.addParameter("appname", applicationName);
        }

        if (subMenuPath != null) {
            urlBuilder.addParameter("submenu", subMenuPath);
        }

        for (String fragmentName : fragmentNames) {
            urlBuilder.addParameter(fragmentName, "true");
        }

        return urlBuilder.build().toString();
    }

    @Override
    public void destroy() {
    }

    @Required
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
}
