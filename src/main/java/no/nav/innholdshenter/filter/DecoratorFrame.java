package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Henter rammen som skal brukes til dekorering og omskriver lenkene denne inneholder.
 */
class DecoratorFrame {

    private static final Logger logger = LoggerFactory.getLogger(DecoratorFrame.class);

    private static final String AMPERSAND = "&";
    private static final String COMMA = ",";
    private EnonicContentRetriever contentRetriever;
    private String templateUrl;
    private String headerBarComponentStartTag;
    private String headerBarComponentEndTag;
    private String leftMenuComponentStartTag;
    private String leftMenuComponentEndTag;
    private String breadcrumbComponentStartTag;
    private String breadcrumbComponentEndTag;
    private String breadcrumbComponentMergePoint;

    private boolean includeQueryStringInDecoration = true;
    private List<String> includeQueryStringInDecorationPatterns;
    private List<String> excludeQueryStringFromDecorationPatterns;

    private static final String URL_PATH = "urlPath";
    private static final String RETRIEVING_CONTENT_WITH_URL = "Retrieving content with url: ";
    private static final String FEIL_VED_HENTING_AV_DEKORERINGSRAMME = "Feil ved henting av dekoreringsramme, url: \"{}\" feil: ";


    public HtmlPage getHtmlFrame(HttpServletRequest request, String alternativUrlBasedOnHtmlMetaTag, String role) {
        String urlContext = getUrlContextAsParametrizedQueryString(request, alternativUrlBasedOnHtmlMetaTag, role);

        String pageUrl = templateUrl + "?" + urlContext;

        try {
            String pageFrame = contentRetriever.getPageContent(pageUrl);
            return new HtmlPage(pageFrame);
        } catch (IllegalStateException e) {
            logger.error(FEIL_VED_HENTING_AV_DEKORERINGSRAMME, pageUrl, e);
            return getErrorPage();
        }
    }

    private String getUrlContextAsParametrizedQueryString(HttpServletRequest request, String alternativUrlBasedOnHtmlMetaTag, String role) {
        String urlToCall = URL_PATH + "=";
        String innerUrl = "";
        if (alternativUrlBasedOnHtmlMetaTag != null) {
            innerUrl = alternativUrlBasedOnHtmlMetaTag;
        } else {
            innerUrl = request.getRequestURI();
        }

        urlToCall += innerUrl;

        if (shouldIncludeQueryStringInDecoration(request)) {
            urlToCall += addQueryStringToDecoration(request);
        }

        if (!isEmpty(role)) {
            urlToCall += "&role=" + role;
        }
        return urlToCall;
    }

    private String addQueryStringToDecoration(HttpServletRequest request) {
        String query = request.getQueryString();
        query = query.replaceAll(AMPERSAND, COMMA);
        return COMMA + query;
    }

    private boolean shouldIncludeQueryStringInDecoration(HttpServletRequest request) {
        if (request.getQueryString() == null) {
            return false;
        }
        String url = request.getRequestURI() + "?" + request.getQueryString();

        if (includeQueryStringInDecoration && hasExcludePatterns() && !(InnholdshenterTools.urlMatchesPatternInList(url, excludeQueryStringFromDecorationPatterns))) {
            return true;

        } else if (includeQueryStringInDecoration && !hasExcludePatterns()) {
            return true;

        } else if (hasIncludePatterns() && InnholdshenterTools.urlMatchesPatternInList(url, includeQueryStringInDecorationPatterns)) {
            return true;
        }
        return false;
    }

    private boolean hasExcludePatterns() {
        if (excludeQueryStringFromDecorationPatterns != null && !excludeQueryStringFromDecorationPatterns.isEmpty()) {
            if (hasContent(excludeQueryStringFromDecorationPatterns)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasContent(List<String> list) {
        for (String pattern : list) {
            if (pattern != null && pattern.length() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIncludePatterns() {
        if (includeQueryStringInDecorationPatterns != null && !includeQueryStringInDecorationPatterns.isEmpty()) {
            if (hasContent(includeQueryStringInDecorationPatterns)) {
                return true;
            }
        }
        return false;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

    public void setContentRetriever(EnonicContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }


    public List<String> getIncludeQueryStringInDecorationPatterns() {
        return includeQueryStringInDecorationPatterns;
    }

    public void setIncludeQueryStringInDecorationPatterns(List<String> includeQueryStringInDecorationPatterns) {
        this.includeQueryStringInDecorationPatterns = includeQueryStringInDecorationPatterns;
    }

    public List<String> getExcludeQueryStringFromDecorationPatterns() {
        return excludeQueryStringFromDecorationPatterns;
    }

    public void setExcludeQueryStringFromDecorationPatterns(List<String> excludeQueryStringFromDecorationPatterns) {
        this.excludeQueryStringFromDecorationPatterns = excludeQueryStringFromDecorationPatterns;
    }

    public boolean isIncludeQueryStringInDecoration() {
        return includeQueryStringInDecoration;
    }

    public String getBreadcrumbComponentMergePoint() {
        return breadcrumbComponentMergePoint;
    }

    public String getBreadcrumbComponentStartTag() {
        return breadcrumbComponentStartTag;
    }

    public String getBreadcrumbComponentEndTag() {
        return breadcrumbComponentEndTag;
    }

    public String getLeftMenuComponentStartTag() {
        return leftMenuComponentStartTag;
    }

    public String getLeftMenuComponentEndTag() {
        return leftMenuComponentEndTag;
    }

    public String getHeaderBarComponentEndTag() {
        return headerBarComponentEndTag;
    }

    public String getHeaderBarComponentStartTag() {
        return headerBarComponentStartTag;
    }

    public void setLeftMenuComponentEndTag(String leftMenuComponentEndTag) {
        this.leftMenuComponentEndTag = leftMenuComponentEndTag;
    }

    public void setLeftMenuComponentStartTag(String leftMenuComponentStartTag) {
        this.leftMenuComponentStartTag = leftMenuComponentStartTag;
    }

    public void setHeaderBarComponentEndTag(String headerBarComponentEndTag) {
        this.headerBarComponentEndTag = headerBarComponentEndTag;
    }

    public void setHeaderBarComponentStartTag(String headerBarComponentStartTag) {
        this.headerBarComponentStartTag = headerBarComponentStartTag;
    }

    public void setIncludeQueryStringInDecoration(boolean includeQueryStringInDecoration) {
        this.includeQueryStringInDecoration = includeQueryStringInDecoration;
    }

    public void setBreadcrumbComponentStartTag(String breadcrumbComponentStartTag) {
        this.breadcrumbComponentStartTag = breadcrumbComponentStartTag;
    }

    public void setBreadcrumbComponentEndTag(String breadcrumbComponentEndTag) {
        this.breadcrumbComponentEndTag = breadcrumbComponentEndTag;
    }

    public void setBreadcrumbComponentMergePoint(String breadcrumbComponentMergePoint) {
        this.breadcrumbComponentMergePoint = breadcrumbComponentMergePoint;
    }

    protected static HtmlPage getErrorPage() {
        StringBuilder pageContent = new StringBuilder();
        pageContent.append("<html>");
        pageContent.append("<head><title>NAV - Feilside</title></head>");
        pageContent.append("<body><h2>Tjenesten er utilgjengelig på grunn av teknisk feil.</h2>");
        pageContent.append("</body>");
        pageContent.append("</html>");
        HtmlPage errorPage = new HtmlPage(pageContent.toString());
        errorPage.setErrorPage(true);
        return errorPage;
    }
}
