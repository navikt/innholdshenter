package no.nav.innholdshenter.common;

import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class EnonicCacheEntryFactory implements CacheEntryFactory {
    private static final Logger logger = LoggerFactory.getLogger(EnonicCacheEntryFactory.class);

    private static final String DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL = "Retrieving page content from url {}";
    private static final String HTTP_STATUS_FEIL = "Http-kall feilet, url: {} status: {} grunn: {}";

    private HttpClient httpClient;

    private Map<String, CacheStatusMelding> statusMeldinger;

    public EnonicCacheEntryFactory(HttpClient httpClient, Map<String, CacheStatusMelding> statusMeldinger) {
        this.httpClient = httpClient;
        this.statusMeldinger = statusMeldinger;
    }

    @Override
    public Object createEntry(Object key) throws IOException {
        String url = key.toString();
        String uniqueRandomUrl = InnholdshenterTools.makeUniqueRandomUrl(url);
        logger.debug(DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL, uniqueRandomUrl);

        return getNewContent(url, uniqueRandomUrl);
    }

    private void logStatus(int statusCode, String statusMessage, String key) {
        CacheStatusMelding c = new CacheStatusMelding(statusCode, statusMessage, System.currentTimeMillis());
        statusMeldinger.put(key, c);
    }

    private synchronized String getNewContent(String key, String uniqueRandomUrl) throws IOException {
        HttpGet httpGet = new HttpGet(uniqueRandomUrl);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String content;

        try {
            content = httpClient.execute(httpGet, responseHandler);
            logStatus(200, "OK", key);

        } catch (HttpResponseException exception) {
            logger.error(HTTP_STATUS_FEIL, key, exception.getStatusCode(), exception.getMessage());
            logStatus(exception.getStatusCode(), exception.getMessage(), key);
            throw new IOException(exception);
        }

        return content;
    }

    public synchronized void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
