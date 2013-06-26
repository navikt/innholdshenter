package no.nav.innholdshenter.filter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * Legger head og body inn i "rammen"
 */
public final class MarkupMerger {

   private static final String EMPTY_STRING = "";
   private static final String ENCODING_PATTERN_MATCHER = "(<\\?xml\\s*version=\"(.*)\"\\s*encoding=\"([^\"]*)?\">?\\s*\\?>)";

    private static final String HEAD_PLACEHOLDER = "<!-- ${head} -->";

   private static final String BODY_PLACEHOLDER = "<!-- ${body} -->";
   private static final String TITLE_PLACEHOLDER = "<!-- ${title} -->";
   private static final String HEADERBAR_COMPONENT_PLACEHOLDER = "<!-- ${headerbarcomponent} -->";
   private static final String LEFT_MENU_COMPONENT_PLACEHOLDER = "<!-- ${leftmenucomponent} -->";
    private static final String BREADCRUMB_FROM_VS_START = "<!--breadcrumbs_start-->";

   private static final String BREADCRUMB_FROM_VS_END = "<!--breadcrumbs_end-->";
   private static final String BREADCRUMB_PLACEHOLDER = "<!-- ${breadcrumb} -->";

   private MarkupMerger() {
   }

    public static HtmlPage mergeHeaderBarComponent(HtmlPage originalPageFromapplication, HtmlPage mergedPage, String headerBarStartTag, String headerBarEndTag) {
        return mergeComponent(originalPageFromapplication, headerBarStartTag, headerBarEndTag, mergedPage, HEADERBAR_COMPONENT_PLACEHOLDER);
    }

    public static HtmlPage mergeLeftMenuComponent(HtmlPage page, HtmlPage mergedPage, String menuComponentStartTag, String menuCompoentEndTag) {
       return mergeComponent(page, menuComponentStartTag, menuCompoentEndTag, mergedPage, LEFT_MENU_COMPONENT_PLACEHOLDER);
    }

    public static HtmlPage mergeBreadcrumbComponent(HtmlPage originalPageFromApplication, HtmlPage mergedPage, String breadcrumbComponentStartTag,
                                                    String breadcrumbComponentEndTag, String breadbrumbComponentMergePoint) {
        String tmpMarkup;
        String breadcrumbsFromVerticalSite = mergedPage.getAreaWithinTagsFromHtml(BREADCRUMB_FROM_VS_START, BREADCRUMB_FROM_VS_END);
        String breadcrumbsFromApplication = originalPageFromApplication.getAreaWithinTagsFromHtml(breadcrumbComponentStartTag, breadcrumbComponentEndTag);
        String newBreadCrumb = mergeApplicationBreadcrumbWithVSBreadcrumb(breadcrumbsFromVerticalSite, breadcrumbsFromApplication,
                breadcrumbComponentStartTag, breadcrumbComponentEndTag, breadbrumbComponentMergePoint);

        tmpMarkup = mergedPage.removeAreaWithinTagsFromHtml(BREADCRUMB_FROM_VS_START, BREADCRUMB_FROM_VS_END);
        tmpMarkup = tmpMarkup.replace(BREADCRUMB_PLACEHOLDER, newBreadCrumb);

        return new HtmlPage(tmpMarkup);
    }

    private static String mergeApplicationBreadcrumbWithVSBreadcrumb(String breadcrumbsFromVerticalSite, String breadcrumbsFromApplication,
                                                                     String breadcrumbComponentStartTag, String breadcrumbComponentEndTag,
                                                                     String breadbrumbComponentMergePoint) {

        String breadcrumbsFromApplicationStrippedTags = stripTags(breadcrumbsFromApplication, breadcrumbComponentStartTag, breadcrumbComponentEndTag);
        String breadcrumbsFromVerticalSiteStrippedTags = stripTags(breadcrumbsFromVerticalSite, BREADCRUMB_FROM_VS_START, BREADCRUMB_FROM_VS_END);
        String newBreadCrumb = breadcrumbsFromVerticalSiteStrippedTags;

        if (mergePointIsFoundInBothDecorationTemplateAndPageToBeRewritten(breadbrumbComponentMergePoint, breadcrumbsFromApplicationStrippedTags, breadcrumbsFromVerticalSiteStrippedTags)) {
            newBreadCrumb = newBreadCrumb.substring(0, breadcrumbsFromVerticalSiteStrippedTags.indexOf(breadbrumbComponentMergePoint));
            newBreadCrumb += breadcrumbsFromApplicationStrippedTags.substring(breadcrumbsFromApplicationStrippedTags.indexOf(breadbrumbComponentMergePoint), breadcrumbsFromApplicationStrippedTags.length());
            return newBreadCrumb;
        } else {
           return breadcrumbsFromApplicationStrippedTags;
        }
    }

   private static boolean mergePointIsFoundInBothDecorationTemplateAndPageToBeRewritten(String breadbrumbComponentMergePoint, String breadcrumbsFromApplicationStrippedTags, String breadcrumbsFromVerticalSiteStrippedTags) {
      return breadbrumbComponentMergePoint != null && breadcrumbsFromApplicationStrippedTags.indexOf(breadbrumbComponentMergePoint) >= 0 && breadcrumbsFromVerticalSiteStrippedTags.indexOf(breadbrumbComponentMergePoint) >=0;
   }

   private static String stripTags(String stringToStrip, String startTag, String endTag)
   {
      return stringToStrip.replace(startTag, EMPTY_STRING).replace(endTag, EMPTY_STRING);
   }

    private static HtmlPage mergeComponent(HtmlPage originalPageFromApplication, String startTag, String endTag, HtmlPage mergedPage, String placeHolder) {
        String tmpMarkup;
        tmpMarkup = mergedPage.removeAreaWithinTagsFromHtml(startTag, endTag);
        String areaWithinTagsFromHtml = originalPageFromApplication.getAreaWithinTagsFromHtml(startTag, endTag);
        tmpMarkup = tmpMarkup.replace(placeHolder, areaWithinTagsFromHtml);

        return new HtmlPage(tmpMarkup);
    }


    public static HtmlPage mergeMarkup(String verticalSiteFrame, HtmlPage originalPageFromApplication) {
        String tmpMarkup = verticalSiteFrame;

        String head = originalPageFromApplication.getHeadPartOfHtml();
        String body = originalPageFromApplication.getBodyPartOfHtml();
        String title = originalPageFromApplication.getTitlePartOfHtml();

        tmpMarkup = tmpMarkup.replace(HEAD_PLACEHOLDER, head);
        tmpMarkup = tmpMarkup.replace(BODY_PLACEHOLDER, body);
        tmpMarkup = tmpMarkup.replace(TITLE_PLACEHOLDER, title);

        tmpMarkup = getEncodingTag(originalPageFromApplication.getHtml()) + tmpMarkup;

        if (noChangesHasBeenMadeToPage(head, body, title)) {
            return originalPageFromApplication;
        }
        return new HtmlPage(tmpMarkup);
    }

   private static boolean noChangesHasBeenMadeToPage(String head, String body, String title) {
      return head.equals(EMPTY_STRING) && body.equals(EMPTY_STRING) && title.equals(EMPTY_STRING);
   }

   public static String getEncodingTag(String html) {
        Pattern pattern = Pattern.compile(ENCODING_PATTERN_MATCHER, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return EMPTY_STRING;
        }
    }
}
