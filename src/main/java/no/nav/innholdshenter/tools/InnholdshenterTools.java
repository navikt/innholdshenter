package no.nav.innholdshenter.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnholdshenterTools {
    private static final Logger logger = LoggerFactory.getLogger(InnholdshenterTools.class);

    private static final String INFO_LAGE_NY_UNIK_URL_FEILET = "Feilet å lage ny unik url, url: {}.";

    public static String makeRandomUrl(String url) {
        String sidToAvoidServerCache = RandomStringUtils.randomAlphanumeric(15);
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("sid", sidToAvoidServerCache);
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            logger.warn(INFO_LAGE_NY_UNIK_URL_FEILET, url, e);
        }
        return url;
    }

    public static boolean urlMatchesPatternInList(String innerUrl, List<String> list) {
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
}
