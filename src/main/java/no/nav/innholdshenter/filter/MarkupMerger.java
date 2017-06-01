package no.nav.innholdshenter.filter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;

import static no.nav.innholdshenter.filter.DecoratorFilterUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public class MarkupMerger {

    private final List<String> noSubmenuPatterns;
    private List<String> fragmentNames;
    private final String originalResponseString;
    private Document htmlFragments;
    private String applicationName;
    private final HttpServletRequest request;
    private static final Logger logger = getLogger(MarkupMerger.class);

    public MarkupMerger(List<String> fragmentNames, List<String> noSubmenuPatterns, String originalResponseString, Document htmlFragments, HttpServletRequest request, String applicationName) {
        this.fragmentNames = fragmentNames;
        this.noSubmenuPatterns = noSubmenuPatterns;
        this.originalResponseString = originalResponseString;
        this.applicationName = applicationName;
        this.htmlFragments = htmlFragments;
        this.request = request;
    }

    public String merge() {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            Element element = htmlFragments.getElementById(fragmentName);
            if (elementIsNotFoundInResponseFromEnonic(element)) {
                logger.error(fragmentName + " ble ikke funnet i responsen fra Enonic. Undersøk om noe er fjernet fra ressursen i enonic (Appressurser / common-html).");
                responseString = responseString.replace(createPlaceholder(fragmentName), "");
                continue;
            }

            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }

        responseString = extractAndInjectTitle(responseString);
        responseString = extractAndInjectApplicationName(responseString);
        checkForUnresolvedPlaceholders(responseString);
        return responseString;
    }

    private boolean elementIsNotFoundInResponseFromEnonic(Element element) {
        return element == null;
    }

    private String extractAndInjectTitle(String responseString) {
        Document document = Jsoup.parse(responseString);
        String title = document.title();
        return responseString.replace(createPlaceholder("title"), title);
    }

    private String extractAndInjectApplicationName(String responseString) {
        if (applicationName == null || applicationName.isEmpty()) {
            return responseString;
        }
        return responseString.replace("{{applicationName}}", applicationName);
    }

    private void checkForUnresolvedPlaceholders(String responseString) {
        Matcher matcher = createMatcher(PLACEHOLDER_REGEX, responseString);
        if (matcher.matches()) {
            logger.error("Fant unresolved placeholder " + matcher.group(1) + " i applikasjonens markup.");
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
        Element maincontent = document.getElementById("maincontent");
        Element row = maincontent.getElementsByClass("row").first();

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
        String uriToMatch = getRequestUriOrAlternativePathBasedOnMetaTag(originalResponseString, request);
        for (String noSubmenuPattern : noSubmenuPatterns) {
            Matcher matcher = createMatcher(noSubmenuPattern, uriToMatch);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

}
