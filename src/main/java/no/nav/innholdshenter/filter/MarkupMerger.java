package no.nav.innholdshenter.filter;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkupMerger {

    private static final String PLACEHOLDER_START = "{{";
    private static final String PLACEHOLDER_END = "}}";
    private static final String PLACEHOLDER_PREFIX = "fragment.";
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
            if (isFragmentSubmenu(fragmentName)) {
                responseString = mergeSubmenuFragment(request, responseString, fragmentName, element);
            } else {
                responseString = mergeFragment(responseString, fragmentName, element.html());
            }
        }

        return responseString;
    }

    public String removePlaceholders(String originalResponseString) {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            responseString = mergeFragment(responseString, fragmentName, "");
        }
        return responseString;
    }

    public static boolean isFragmentSubmenu(String fragmentName) {
        return "submenu".equals(fragmentName);
    }

    public static Matcher createMatcher(String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return pattern.matcher(content);
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
