package no.nav.innholdshenter.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HtmlPage {

    private static final String BODY_TAG = "body";
    private static final String TITLE_TAG = "title";
    private static final String HEAD_TAG = "head";
    private static final String EMPTY_STRING = "";
    private static final String META_TAG_MATCHER_TEMPLATE = "<meta name=\"%s\"\\s*content=\"([^\"]*)?";
    private static final String TAG_MATCHTER_TEMPLATE = "<%s[^>]*?>(.*)</%s[^>]*>";
    private String html;
    private boolean errorPage;
    private static final String PAGE_CONTENT_WAS_NULL = "Page content was null.";
    private static Pattern findTitleTagInHead = Pattern.compile("<title[^>]*?>(.*)</title[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    public HtmlPage(String html) {
        this.html = html;
    }

    public String getBodyPartOfHtml() {
        return extractTag(html, BODY_TAG);
    }

    public String getHeadPartOfHtml() {
        return extractHead();
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getTitlePartOfHtml() {
        return extractTag(html, TITLE_TAG);
    }

    private String extractHead() {
        String localHead = extractTag(html, HEAD_TAG);

        Matcher matcher = findTitleTagInHead.matcher(localHead);

        if (matcher.find()) {
            return matcher.replaceAll(EMPTY_STRING);
        }

        return localHead;
    }

    public String getAreaWithinTagsFromHtml(String startTag, String endTag) {
        return extractAreaWithinTags(html, startTag, endTag);
    }

    public String removeAreaWithinTagsFromHtml(String startTag, String endTag) {
        return removeAreaWithinTags(html, startTag, endTag);
    }

    private String removeAreaWithinTags(String htmlContent, String startTag, String endTag) {
        if (htmlContent == null) {
            throw new IllegalStateException(PAGE_CONTENT_WAS_NULL);
        }

        Matcher matcher = createTagMatcher(startTag, endTag, htmlContent);

        if (matcher.find()) {
            return matcher.replaceAll(EMPTY_STRING);
        } else {
            return htmlContent;
        }
    }

    private String extractAreaWithinTags(String htmlContent, String startTag, String endTag) {
        if (htmlContent == null) {
            throw new IllegalStateException(PAGE_CONTENT_WAS_NULL);
        }

        Matcher matcher = createTagMatcher(startTag, endTag, htmlContent);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return EMPTY_STRING;
        }
    }

    public String extractMetaTagInformation(String key) {

        Matcher matcher = createMatcher(String.format(META_TAG_MATCHER_TEMPLATE, key), html);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private String extractTag(String htmlContent, String tagName) {
        if (htmlContent == null) {
            throw new IllegalStateException(PAGE_CONTENT_WAS_NULL);
        }

        Matcher matcher = createMatcher(String.format(TAG_MATCHTER_TEMPLATE, tagName, tagName), htmlContent);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return EMPTY_STRING;
        }
    }

    private Matcher createTagMatcher(String startTag, String endTag, String htmlContent) {
        return createMatcher(String.format("(%s.*?%s)", startTag, endTag), htmlContent);
    }

    private Matcher createMatcher(String regex, String htmlContent) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
        return pattern.matcher(htmlContent);
    }

    public String getHtml() {
        return html;
    }

    public boolean isErrorPage() {
        return errorPage;
    }

    public void setErrorPage(boolean errorPage) {
        this.errorPage = errorPage;
    }
}
