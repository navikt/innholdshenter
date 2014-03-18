package no.nav.innholdshenter.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecoratorFilterUtils {

    private DecoratorFilterUtils() {
    }

    public static boolean isFragmentSubmenu(String fragmentName) {
        return "submenu".equals(fragmentName);
    }

    public static Matcher createMatcher(String regex, String content) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return pattern.matcher(content);
    }
}
