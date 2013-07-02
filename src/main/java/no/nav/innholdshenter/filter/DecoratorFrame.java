package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;
/**
 * Henter rammen som skal brukes til dekorering og omskriver lenkene denne inneholder.
 */
public class DecoratorFrame {

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
    private static final String FEIL_VED_HENTING_AV_DEKORERINGSRAMME = "Feil ved henting av dekoreringsramme: ";

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

    public void setIncludeQueryStringInDecoration(boolean includeQueryStringInDecoration) {
        this.includeQueryStringInDecoration = includeQueryStringInDecoration;
    }

    public String getBreadcrumbComponentMergePoint() {
        return breadcrumbComponentMergePoint;
    }

    public void setBreadcrumbComponentMergePoint(String breadcrumbComponentMergePoint) {
        this.breadcrumbComponentMergePoint = breadcrumbComponentMergePoint;
    }

    public String getBreadcrumbComponentStartTag() {
        return breadcrumbComponentStartTag;
    }

    public void setBreadcrumbComponentStartTag(String breadcrumbComponentStartTag) {
        this.breadcrumbComponentStartTag = breadcrumbComponentStartTag;
    }

    public String getBreadcrumbComponentEndTag() {
        return breadcrumbComponentEndTag;
    }

    public void setBreadcrumbComponentEndTag(String breadcrumbComponentEndTag) {
        this.breadcrumbComponentEndTag = breadcrumbComponentEndTag;
    }


    public String getLeftMenuComponentStartTag() {
        return leftMenuComponentStartTag;
    }

    public String getLeftMenuComponentEndTag() {
        return leftMenuComponentEndTag;
    }

    public void setLeftMenuComponentEndTag(String leftMenuComponentEndTag) {
        this.leftMenuComponentEndTag = leftMenuComponentEndTag;
    }

    public void setLeftMenuComponentStartTag(String leftMenuComponentStartTag) {
        this.leftMenuComponentStartTag = leftMenuComponentStartTag;
    }

    public String getHeaderBarComponentEndTag() {
        return headerBarComponentEndTag;
    }

    public void setHeaderBarComponentEndTag(String headerBarComponentEndTag) {
        this.headerBarComponentEndTag = headerBarComponentEndTag;
    }

    public String getHeaderBarComponentStartTag() {
        return headerBarComponentStartTag;
    }

    public void setHeaderBarComponentStartTag(String headerBarComponentStartTag) {
        this.headerBarComponentStartTag = headerBarComponentStartTag;
    }

    public DecoratorFrame() {
    }

    public HtmlPage getHtmlFrame(HttpServletRequest request, String alternativUrlBasedOnHtmlMetaTag, String role) {
        String urlContext = getUrlContextAsParametrizedQueryString(request, alternativUrlBasedOnHtmlMetaTag, role);

        String pageUrl = templateUrl + "?" + urlContext;

        try {
            logger.debug(RETRIEVING_CONTENT_WITH_URL + pageUrl);
            String pageFrame = contentRetriever.getPageContent(pageUrl);
            return new HtmlPage(pageFrame);
        } catch (IllegalStateException e) {
            logger.error(FEIL_VED_HENTING_AV_DEKORERINGSRAMME + e.getMessage());
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
            urlToCall += "&role="+role;
        }
        return urlToCall;
    }

    private String addQueryStringToDecoration(HttpServletRequest request) {
        String query = request.getQueryString();
        query = query.replaceAll(AMPERSAND, COMMA);
        return COMMA + query;
    }

    private boolean shouldIncludeQueryStringInDecoration(HttpServletRequest request) {

        if (request.getQueryString() != null) {
            String url = getFullRequestUrlFromRequest(request);


            if (includeQueryStringInDecoration) {
                if (hasExcludePatterns()) {
                    if (!(urlMatchesPatternInList(url, excludeQueryStringFromDecorationPatterns))) {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                if (hasIncludePatterns()) {
                    if (urlMatchesPatternInList(url, includeQueryStringInDecorationPatterns)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String getFullRequestUrlFromRequest(HttpServletRequest request) {
        return request.getRequestURI() + "?" + request.getQueryString();
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


    private boolean urlMatchesPatternInList(String innerUrl, List<String> list) {
        for (String patternAsString : list) {
            if (patternAsString != null && patternAsString.length() > 0) {
                Pattern pattern = Pattern.compile(patternAsString);

                Matcher matcher = pattern.matcher(innerUrl);

                if (matcher.find()) {
                    return true;
                }
            }
        }
        return false;
    }

    private HtmlPage getErrorPage() {
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

    public void setContentRetriever(EnonicContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    public void setTemplateUrl(String templateUrl) {
        this.templateUrl = templateUrl;
    }

}
