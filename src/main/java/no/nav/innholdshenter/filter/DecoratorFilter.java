package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legger ramme rundt applikasjonen som bruker filteret.
 */
public class DecoratorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFilter.class);
    private static final String LOCALE_UTF_8 = "UTF-8";

    private DecoratorFrame decoratorFrame;
    private EnonicContentRetriever contentRetriever;
    private String templateUrl;
    private boolean decorateOnlyOnce = true;
    private Map<String, String> excludeHeaders;
    private List<String> includeContentTypes;

    private static final String VS_HODEFOT_KEY = "hodeFotKey";
    private static final String VS_BRUKERSTATUS_KEY = "Brukerstatus";
    private static final String ERROR_MSG_MARKUPFILTER_UNABLE_TO_DECORATE_PAGE = "Markupfilter: Unable to decorate page. ";
    private static final String ALREADY_DECORATED_HEADER = "X-NAV-innholdshenter";
    private static final String DEBUG_MSG_SHOULD_NOT_DECORATE_RESPONSE = "Markupfilter: No decoration - Should not decorate response. URI:  ";
    private static final String DEBUG_MSG_DECORATION_APPLIED = "Markupfilter: Decoration applied. URI: ";
    private static final String DEBUG_MSG_CAN_NOT_HANDLE_CONTENT_TYPE = "Markupfilter: No decoration - Can not handle content type. ";
    private static final String CONTENT_TYPE = " Content-type: ";


    public DecoratorFilter(EnonicContentRetriever contentRetriever, String templateUrl) {
        decoratorFrame = new DecoratorFrame();
        setContentRetriever(contentRetriever);
        setTemplateUrl(templateUrl);
        setDefaultExcludeHeaders();
        setDefaultIncludeContentTypes();
        setDefaultHeaderBarComponents();
    }

    private void setDefaultHeaderBarComponents() {
        setHeaderBarComponentStartTag("&lt;!--HEADERBAR_START--&gt;");
        setHeaderBarComponentEndTag("&lt;!--HEADERBAR_END--&gt;");
    }

    private void setDefaultExcludeHeaders() {
        excludeHeaders = new HashMap<String, String>();
        excludeHeaders.put("X-Requested-With", "XMLHttpRequest");
        setExcludeHeaders(excludeHeaders);
    }

    private void setDefaultIncludeContentTypes() {
        includeContentTypes = new ArrayList<String>();
        includeContentTypes.add("text/html");
        includeContentTypes.add("text/html; charset=UTF-8");
        includeContentTypes.add("application/xhtml+xml");
    }

    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) rq;
        HttpServletResponse response = (HttpServletResponse) rs;
        DecoratorResponseWrapper responseWrapper = new DecoratorResponseWrapper(response);

        if (!shouldDecorateResponseContent(request)) {
            chain.doFilter(request, response);
            logger.debug(DEBUG_MSG_SHOULD_NOT_DECORATE_RESPONSE + request.getRequestURI());
            return;
        }

        try {
            chain.doFilter(request, responseWrapper);
            responseWrapper.flushBuffer();
            if (canHandleContentType(responseWrapper.getContentType())) {
                transformResponseContent(responseWrapper, request);
                logger.debug(DEBUG_MSG_DECORATION_APPLIED + request.getRequestURI());
            } else {
                writeOriginalOutputToResponse(responseWrapper);
                logger.debug(DEBUG_MSG_CAN_NOT_HANDLE_CONTENT_TYPE + CONTENT_TYPE + responseWrapper.getContentType()
                        + " URI: " + request.getRequestURI());
            }
        } catch (IllegalStateException e) {
            logger.error(ERROR_MSG_MARKUPFILTER_UNABLE_TO_DECORATE_PAGE, e);
        }
    }

    private void writeOriginalOutputToResponse(DecoratorResponseWrapper responseWrapper) throws IOException {
        ServletResponse response = responseWrapper.getResponse();
        try {
            response.getOutputStream().write(responseWrapper.getOutputAsByteArray());
        } catch (IllegalStateException getWriterHasAlreadyBeenCalled) {
            response.getWriter().print(responseWrapper.getOutputAsString());
        }
    }

    private void transformResponseContent(DecoratorResponseWrapper responseWrapper, ServletRequest request) throws IOException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HtmlPage originalPage = new HtmlPage(responseWrapper.getOutputAsString());

        String alternativUrlBasedOnHtmlMetaTag = extractAlternativUrlBasedOnHtmlMetaTag(originalPage);
        String role = extractRoleBasedOnHtmlMetaTag(originalPage);
        HtmlPage pageFrame = decoratorFrame.getHtmlFrame(httpServletRequest, alternativUrlBasedOnHtmlMetaTag, role);
        HtmlPage mergedPage = decorateResponseContent(pageFrame.getHtml(), originalPage);
        writeTransformedResponse(responseWrapper, mergedPage);
    }

    private String extractRoleBasedOnHtmlMetaTag(HtmlPage originalPage) {
        return originalPage.extractMetaTagInformation(VS_BRUKERSTATUS_KEY);
    }

    private String extractAlternativUrlBasedOnHtmlMetaTag(HtmlPage originalPage) {
        return originalPage.extractMetaTagInformation(VS_HODEFOT_KEY);
    }

    private boolean shouldDecorateResponseContent(HttpServletRequest request) {
        if (decorateOnlyOnce && filterAlreadyAppliedForRequest(request)) {
            return false;
        }
        for (Map.Entry<String, String> entry : excludeHeaders.entrySet()) {
            if (requestHeaderHasValue(request, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean filterAlreadyAppliedForRequest(HttpServletRequest request) {
        if (request.getAttribute(ALREADY_DECORATED_HEADER) == Boolean.TRUE) {
            return true;
        } else {
            request.setAttribute(ALREADY_DECORATED_HEADER, Boolean.TRUE);
            return false;
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void destroy() {
    }

    protected HtmlPage decorateResponseContent(String verticalSiteFrame, HtmlPage originalPageFromApplication) {
        String headerBarComponentStartTag = decoratorFrame.getHeaderBarComponentStartTag();
        String headerBarComponentEndTag = decoratorFrame.getHeaderBarComponentEndTag();
        String leftMenuComponentStartTag = decoratorFrame.getLeftMenuComponentStartTag();
        String leftMenuBarComponentEndTag = decoratorFrame.getLeftMenuComponentEndTag();
        String breadbrumbComponentStartTag = decoratorFrame.getBreadcrumbComponentStartTag();
        String breadbrumbComponentEndTag = decoratorFrame.getBreadcrumbComponentEndTag();
        String breadbrumbComponentMergePoint = decoratorFrame.getBreadcrumbComponentMergePoint();

        HtmlPage mergedPage = MarkupMerger.mergeMarkup(verticalSiteFrame, originalPageFromApplication);

        if (headerBarComponentStartTag != null && headerBarComponentEndTag != null) {
            mergedPage = MarkupMerger.mergeHeaderBarComponent(mergedPage, headerBarComponentStartTag, headerBarComponentEndTag);
        }

        if (leftMenuComponentStartTag != null && leftMenuBarComponentEndTag != null) {
            mergedPage = MarkupMerger.mergeLeftMenuComponent(mergedPage, leftMenuComponentStartTag, leftMenuBarComponentEndTag);
        }

        if (breadbrumbComponentStartTag != null && breadbrumbComponentEndTag != null) {
            mergedPage = MarkupMerger.mergeBreadcrumbComponent(originalPageFromApplication, mergedPage, breadbrumbComponentStartTag, breadbrumbComponentEndTag, breadbrumbComponentMergePoint);
        }

        return mergedPage;
    }

    protected void writeTransformedResponse(DecoratorResponseWrapper responseWrapper, HtmlPage page) throws IOException {
        byte[] pageAsBytes = page.getHtml().getBytes(LOCALE_UTF_8);
        ServletResponse response = responseWrapper.getResponse();
        ServletOutputStream stream = null;
        try {
            response.setContentLength(pageAsBytes.length);
            stream = response.getOutputStream();
            stream.write(pageAsBytes);
        } catch (IllegalStateException getWriterHasAlreadyBeenCalled) {
            response.getWriter().print(page.getHtml());
        } finally {
            response.flushBuffer();
            if (stream != null) {
                stream.close();
            }
        }
    }

    public boolean canHandleContentType(String contentType) {
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

    public static boolean requestHeaderHasValue(HttpServletRequest request, String header, String value) {
        return (request.getHeader(header) != null) && request.getHeader(header).equalsIgnoreCase(value);
    }

    public void setExcludeHeaders(Map<String, String> excludeHeaders) {
        this.excludeHeaders = excludeHeaders;
    }

    public void setIncludeContentTypes(List<String> includeContentTypes) {
        this.includeContentTypes = includeContentTypes;
    }

    public void setDecorateOnlyOnce(boolean decorateOnlyOnce) {
        this.decorateOnlyOnce = decorateOnlyOnce;
    }

    public void setDecoratorFrame(DecoratorFrame decoratorFrame) {
        this.decoratorFrame = decoratorFrame;
        decoratorFrame.setContentRetriever(contentRetriever);
        decoratorFrame.setTemplateUrl(templateUrl);
    }

    public DecoratorFrame getDecoratorFrame() {
        return decoratorFrame;
    }

    public void setContentRetriever(EnonicContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
        decoratorFrame.setContentRetriever(this.contentRetriever);
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
        decoratorFrame.setTemplateUrl(this.templateUrl);
    }

    public void setLeftMenuComponentEndTag(String leftMenuComponentEndTag) {
        decoratorFrame.setLeftMenuComponentEndTag(leftMenuComponentEndTag);
    }

    public void setLeftMenuComponentStartTag(String leftMenuComponentStartTag) {
        decoratorFrame.setLeftMenuComponentStartTag(leftMenuComponentStartTag);
    }

    public void setHeaderBarComponentEndTag(String headerBarComponentEndTag) {
        decoratorFrame.setHeaderBarComponentEndTag(headerBarComponentEndTag);
    }

    public void setHeaderBarComponentStartTag(String headerBarComponentStartTag) {
        decoratorFrame.setHeaderBarComponentStartTag(headerBarComponentStartTag);
    }

    public void setIncludeQueryStringInDecoration(boolean includeQueryStringInDecoration) {
        decoratorFrame.setIncludeQueryStringInDecoration(includeQueryStringInDecoration);
    }

    public void setExcludeQueryStringFromDecorationPatterns(List<String> excludeQueryStringFromDecorationPatterns) {
        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(excludeQueryStringFromDecorationPatterns);
    }

    public void setBreadcrumbComponentStartTag(String breadcrumbComponentStartTag) {
        decoratorFrame.setBreadcrumbComponentStartTag(breadcrumbComponentStartTag);
    }

    public void setBreadcrumbComponentEndTag(String breadcrumbComponentEndTag) {
        decoratorFrame.setBreadcrumbComponentEndTag(breadcrumbComponentEndTag);
    }

    public void setBreadcrumbComponentMergePoint(String breadcrumbComponentMergePoint) {
        decoratorFrame.setBreadcrumbComponentMergePoint(breadcrumbComponentMergePoint);
    }

}
