package no.nav.innholdshenter.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InnholdshenterTools {
    private static final Logger logger = LoggerFactory.getLogger(InnholdshenterTools.class);

    private static final String INFO_LAGE_NY_UNIK_URL_FEILET = "Feilet å lage ny unik url, url: {}.";

    public static String makeUniqueRandomUrl(String url) {
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

    /**
     * Sanitize url before storage in cache. This part of the url tends to include session specific data,
     * so it is often unique, and thrashes the cache.
     * Use this return value as the index in the cache, and not the full url.
     *
     * @param url
     * @return returns a cleaner url, suitable for the cacheline.
     */
    public static String sanitizeUrlCacheKey(String url) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
            List<NameValuePair> params = uriBuilder.getQueryParams();
            for (NameValuePair nameValuePair : params) {
                if (nameValuePair.getName().startsWith("urlPath")) {
                    String urlpath = sanitizeUrlPath(nameValuePair.getValue());
                    uriBuilder.setParameter("urlPath", urlpath);
                }
            }
        } catch (URISyntaxException e) {
            logger.debug(e.getMessage());
            return url;
        }
        return uriBuilder.toString();
    }

    private static String sanitizeUrlPath(String urlParam) {
        if (urlParam != null && !urlParam.isEmpty()) {
            return urlParam.split(",")[0];
        }
        return urlParam;
    }

    /* brukes av cachestatuspaneler */
    public static String getHumanDateFromTimestamp(long timestampmillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(timestampmillis);

    }
}
