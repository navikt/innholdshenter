package no.nav.innholdshenter.tools;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

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

}
