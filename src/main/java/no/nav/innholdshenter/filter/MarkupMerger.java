package no.nav.innholdshenter.filter;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;

import static no.nav.innholdshenter.filter.DecoratorFilterUtils.createMatcher;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.isFragmentSubmenu;

public class MarkupMerger {

    private static final String PLACEHOLDER_START = "{{";
    private static final String PLACEHOLDER_END = "}}";
    private static final String PLACEHOLDER_PREFIX = "fragment.";

    private static final String PLACEHOLDER_START_REGEX = "\\{\\{";
    private static final String PLACEHOLDER_END_REGEX = "\\}\\}";
    private static final String PLACEHOLDER_PREFIX_REGEX = "fragment\\.";
    private static final String PLACEHOLDER_REGEX = ".*(" + PLACEHOLDER_START_REGEX + PLACEHOLDER_PREFIX_REGEX + ".*" + PLACEHOLDER_END_REGEX + ").*";

    private final List<String> noSubmenuPatterns;
    private List<String> fragmentNames;

    public MarkupMerger(List<String> fragmentNames, List<String> noSubmenuPatterns) {
        this.fragmentNames = fragmentNames;
        this.noSubmenuPatterns = noSubmenuPatterns;
    }

    public String merge(String originalResponseString, Document htmlFragments, HttpServletRequest request) {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            Element element = htmlFragments.getElementById(fragmentName);

            if (element == null) {
                throw new RuntimeException("Element [ " + fragmentName + " ] ikke funnet i responsen fra Enonic.");
            }

            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(request, responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }

        checkForUnresolvedPlaceholders(responseString);
        return responseString;
    }

    public String removePlaceholders(String originalResponseString) {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            responseString = mergeFragment(responseString, fragmentName, "");
        }

        checkForUnresolvedPlaceholders(responseString);
        return responseString;
    }

    private void checkForUnresolvedPlaceholders(String responseString) {
        Matcher matcher = createMatcher(PLACEHOLDER_REGEX, responseString);
        if (matcher.matches()) {
            throw new RuntimeException("Fant unresolved placeholder " + matcher.group(1) + " i applikasjonens markup.");
        }
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
        String placeholder = PLACEHOLDER_START + PLACEHOLDER_PREFIX + fragmentName + PLACEHOLDER_END;
        if (!responseString.contains(placeholder)) {
            throw new RuntimeException("Fant ikke placeholder " + placeholder + " i applikasjonens markup.");
        }
        return responseString.replace(placeholder, elementMarkup);
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

}
