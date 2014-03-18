package no.nav.innholdshenter.filter;

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
            Element element = this.htmlFragments.getElementById(fragmentName);

            if (element == null) {
                throw new RuntimeException("Element [ " + fragmentName + " ] ikke funnet i responsen fra Enonic.");
            }

            String placeholder = createPlaceholder(fragmentName);
            if (!responseString.contains(placeholder)) {
                throw new RuntimeException("Fant ikke placeholder " + placeholder + " i applikasjonens markup.");
            }

            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(request, responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }

        Matcher matcher = createMatcher(PLACEHOLDER_REGEX, responseString);
        if (matcher.matches()) {
            throw new RuntimeException("Fant unresolved placeholder " + matcher.group(1) + " i applikasjonens markup.");
        }

        return responseString;
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
        return responseString.replace(createPlaceholder(fragmentName), elementMarkup);
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
