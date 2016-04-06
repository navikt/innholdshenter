package no.nav.innholdshenter.filter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class DecoratorFilterUtils {

    static final String PLACEHOLDER_START = "{{";
    static final String PLACEHOLDER_END = "}}";
    static final String PLACEHOLDER_PREFIX = "fragment.";

    static final String PLACEHOLDER_START_REGEX = "\\{\\{";
    static final String PLACEHOLDER_END_REGEX = "\\}\\}";
    static final String PLACEHOLDER_PREFIX_REGEX = "fragment\\.";
    static final String PLACEHOLDER_REGEX = ".*(" + PLACEHOLDER_START_REGEX + PLACEHOLDER_PREFIX_REGEX + ".*" + PLACEHOLDER_END_REGEX + ").*";

    private DecoratorFilterUtils() {
    }

    public static boolean isFragmentSubmenu(String fragmentName) {
        return "submenu".equals(fragmentName);
    }

    public static Matcher createMatcher(String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return pattern.matcher(content);
    }

    public static String removePlaceholders(String originalResponseString, List<String> fragmentNames) {
        String responseString = originalResponseString;
        for (String fragmentName : fragmentNames) {
            responseString = removePlaceholderForFragment(responseString, fragmentName);
        }
        return responseString;
    }

    private static String removePlaceholderForFragment(String responseString, String fragmentName) {
        return responseString.replace(createPlaceholder(fragmentName), "");
    }

    static String createPlaceholder(String fragmentName) {
        return PLACEHOLDER_START + PLACEHOLDER_PREFIX + fragmentName + PLACEHOLDER_END;
    }

    public static String getRequestUriOrAlternativePathBasedOnMetaTag(String originalResponseString, HttpServletRequest request) {
        String alternativeRequestUri = extractMetaTag(originalResponseString, "hodeFotKey");

        if (!isEmpty(alternativeRequestUri)) {
            return alternativeRequestUri;
        } else {
            return request.getRequestURI();
        }
    }

    public static String extractMetaTag(String originalResponseString, String tag) {
        Document doc = Jsoup.parse(originalResponseString);
        Element metaTag = doc.select(String.format("meta[name=%s]", tag)).first();

        if (metaTag != null) {
            return metaTag.attr("content");
        } else {
            return null;
        }
    }
}
