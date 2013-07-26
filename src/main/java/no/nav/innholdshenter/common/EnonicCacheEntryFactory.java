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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnonicCacheEntryFactory implements CacheEntryFactory {
    private static final Logger logger = LoggerFactory.getLogger(EnonicCacheEntryFactory.class);

    private static final String FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG = "Henting fra url {} gikk gjennom, men innholdet var ikke som forventet. Cache ikke oppdatert.";
    private static final String DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL = "Retrieving page content from url {}";
    private static final String HTTP_STATUS_FEIL = "Http-kall feilet, url: {} status: {} grunn: {}";

    private static final List<String> GYLDIG_RESPONS_INNHOLD = Arrays.asList("<html", "<xml", "<properties", "<?xml ", "<!DOCTYPE ");
    private static final int MIN_VALID_CONTENT_LENGTH = 60;

    private HttpClient httpClient;

    private Map<String, CacheStatusMelding> statusMeldinger;

    public EnonicCacheEntryFactory(HttpClient httpClient, Map<String, CacheStatusMelding> statusMeldinger) {
        this.httpClient = httpClient;
        this.statusMeldinger = statusMeldinger;
    }

    @Override
    public Object createEntry(Object key) throws IOException {
        String url = key.toString();
        String randomUrl = InnholdshenterTools.makeRandomUrl(url);
        logger.debug(DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL, randomUrl);

        String innhold = doFetch(url, randomUrl);

        if (!isContentValid(innhold)) {
            logger.warn(FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG, url);
            throw new IOException(String.format("Fikk ugyldig innhold på url: %s", url));
        }
        return innhold;
    }

    private synchronized String doFetch(String url, String randomUrl) throws IOException {
        HttpGet httpGet = new HttpGet(randomUrl);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String innhold;

        try {
            innhold = httpClient.execute(httpGet, responseHandler);
            CacheStatusMelding c = new CacheStatusMelding(200, "OK", System.currentTimeMillis());
            statusMeldinger.put(url, c);
        } catch (HttpResponseException exception) {
            logger.error(HTTP_STATUS_FEIL, url, exception.getStatusCode(), exception.getMessage());
            CacheStatusMelding c = new CacheStatusMelding(exception.getStatusCode(), exception.getMessage(), System.currentTimeMillis());
            statusMeldinger.put(url, c);
            throw new IOException(exception);
        }

        return innhold;
    }

    protected boolean isContentValid(String innhold) throws IOException {
        if (innhold == null || innhold.isEmpty()) {
            return false;
        }

        for (String streng : GYLDIG_RESPONS_INNHOLD) {
            if(innhold.startsWith(streng)) {
                return true;
            }
        }

        if(innhold.length() > MIN_VALID_CONTENT_LENGTH) {
            return true;
        }

        return false;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

}
