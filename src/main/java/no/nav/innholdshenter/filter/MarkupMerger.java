package no.nav.innholdshenter.filter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;

import static no.nav.innholdshenter.filter.DecoratorFilterUtils.PLACEHOLDER_REGEX;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.createMatcher;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.createPlaceholder;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.isFragmentSubmenu;

public class MarkupMerger {

    private final List<String> noSubmenuPatterns;
    private List<String> fragmentNames;
    private final String originalResponseString;
    private Document htmlFragments;
    private final HttpServletRequest request;

    public MarkupMerger(List<String> fragmentNames, List<String> noSubmenuPatterns, String originalResponseString, Document htmlFragments, HttpServletRequest request) {
        this.fragmentNames = fragmentNames;
        this.noSubmenuPatterns = noSubmenuPatterns;
        this.originalResponseString = originalResponseString;
        this.htmlFragments = htmlFragments;
        this.request = request;
    }

    public String merge() {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            Element element = htmlFragments.getElementById(fragmentName);
            validateFragmentAndPlaceholder(fragmentName, element);

            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }

        responseString = extractAndInjectTitle(responseString);
        checkForUnresolvedPlaceholders(responseString);
        return responseString;
    }

    private void validateFragmentAndPlaceholder(String fragmentName, Element element) {
        if (element == null) {
            throw new RuntimeException("Element [ " + fragmentName + " ] ikke funnet i responsen fra Enonic.");
        }

        String placeholder = createPlaceholder(fragmentName);
        if (!originalResponseString.contains(placeholder) && !isFragmentSubmenu(fragmentName)) {
            throw new RuntimeException("Fant ikke placeholder " + placeholder + " i applikasjonens markup.");
        }
    }

    private String extractAndInjectTitle(String responseString) {
        Document document = Jsoup.parse(responseString);
        String title = document.title();
        return responseString.replace(createPlaceholder("title"), title);
    }

    private void checkForUnresolvedPlaceholders(String responseString) {
        Matcher matcher = createMatcher(PLACEHOLDER_REGEX, responseString);
        if (matcher.matches()) {
            throw new RuntimeException("Fant unresolved placeholder " + matcher.group(1) + " i applikasjonens markup.");
        }
    }

    private String mergeSubmenuFragment(String responseString, String fragmentName, Element element) {
        String mergedResponseString = responseString;
        if (requestUriMatchesNoSubmenuPattern()) {
            mergedResponseString = removeSubmenuAndExpandGrid(mergedResponseString);
        } else {
            mergedResponseString = mergeFragment(mergedResponseString, fragmentName, element.html());
        }
        return mergedResponseString;
    }

    private String removeSubmenuAndExpandGrid(String mergedResponseString) {
        Document document = Jsoup.parse(mergedResponseString);
        Element main = document.getElementById("main");
        Element row = main.getElementsByClass("row").first();

        Element subMenu = row.child(0);
        Element application = row.child(1);

        subMenu.remove();
        application.removeClass(application.className());
        application.addClass("col-md-12");

        return document.html();
    }

    private String mergeFragment(String responseString, String fragmentName, String elementMarkup) {
        return responseString.replace(createPlaceholder(fragmentName), elementMarkup);
    }

    private boolean requestUriMatchesNoSubmenuPattern() {
        for (String noSubmenuPattern : noSubmenuPatterns) {
            Matcher matcher = createMatcher(noSubmenuPattern, request.getRequestURI());
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
