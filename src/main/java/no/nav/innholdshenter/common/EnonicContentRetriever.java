package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


/**
 * Henter innholdet for en gitt URL. Hvis ferskt innhold finnes i cacheManager returneres det derfra.
 */
public class EnonicContentRetriever {
    private static final Logger logger = LoggerFactory.getLogger(EnonicContentRetriever.class);
    private static final String SLASH = "/";
    private static final String LOCALE_UTF_8 = "UTF-8";
    private static final String DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL = "Retrieving page content from url {}";
    private static final String WARN_MELDING_FLUSHER_CACHEN = "Flusher cachen: {}";
    private static final String WARN_MELDING_REFRESH_CACHE = "Refresh cachen: {}";
    private static final String FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG = "Henting fra url {} gikk gjennom, men innholdet var ikke som forventet. Cache ikke oppdatert.";
    private static final String HTTP_STATUS_FEIL = "Http-kall feilet, status: {}, grunn: {}";
    public static final List<String> GYLDIG_RESPONS_INNHOLD = Arrays.asList("<html", "<xml", "<properties", "<?xml ", "<!DOCTYPE ");
    private static final int MIN_VALID_CONTENT_LENGTH = 60;

    private List feilmeldinger;

    private String baseUrl;
    private HttpClient httpClient;
    private int httpTimeoutMillis;

    private CacheManager cacheManager;
    private String cachename;
    private int refreshIntervalSeconds;


    public EnonicContentRetriever() {
        feilmeldinger = new ArrayList<String>();
        if (cacheManager == null) {
            cacheManager = CacheManager.create();
        }
        httpClient = new DefaultHttpClient();
        setHttpTimeoutMillis(3000);
    }
    public EnonicContentRetriever(String cachename) {
        this();
        this.cachename = cachename;
        this.setCacheName(cachename);
    }

    private String createUrl(String path) {
        return baseUrl + path;
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url, getRefreshIntervalSeconds());
    }

    private String getPageContentFullUrl(final String url, int timeToLiveSeconds) {
        GenericCache<String> genericCache = new GenericCache<String>(cacheManager, timeToLiveSeconds, url, cachename) {
            protected String getContentFromSource() throws IOException {
                return getPageContentFromUrl(url);
            }
        };
        return genericCache.fetch();
    }

    public Properties getProperties(String path) {
        final String url = createUrl(path);
        return getPropertiesFullUrl(url, getRefreshIntervalSeconds());
    }

    public Properties getPropertiesFullUrl(final String url, int timeToLiveSeconds) {
        GenericCache<Properties> genericCache = new GenericCache<Properties>(cacheManager, timeToLiveSeconds, url, cachename) {
            @Override
            protected Properties getContentFromSource() throws IOException {
                String content = getPageContentFromUrl(url);
                Properties properties = new Properties();
                try {
                    ByteArrayInputStream propertiesStream = new ByteArrayInputStream(content.getBytes(LOCALE_UTF_8));
                    properties.loadFromXML(propertiesStream);
                } catch (IOException e) {
                    logger.error("Feil i konvertering fra xml til Properties objekt.", e);
                    throw new RuntimeException("Feil: Kunne ikke hente data.", e);
                }
                return properties;
            }
        };
        return genericCache.fetch();
    }

    private synchronized String getPageContentFromUrl(String url) throws IOException {
        String randomUrl = InnholdshenterTools.makeRandomUrl(url);
        logger.debug(DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL, randomUrl);
        HttpGet httpGet = new HttpGet(randomUrl);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String innhold;
        try {
            innhold = httpClient.execute(httpGet, responseHandler);
        } catch(HttpResponseException exception) {
            logger.error(HTTP_STATUS_FEIL, exception.getStatusCode(), exception.getMessage());
            feilmeldinger.add(new CacheStatusFeilmelding(exception.getStatusCode(), exception.getMessage(), System.currentTimeMillis()));
            throw new IOException(exception);
        }

        if(!isContentValid(innhold)) {
            logger.warn(FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG, url);
            throw new IOException(String.format("Fikk ugyldig innhold på url: %s" , url));
        }
        return innhold;
    }

    protected boolean isContentValid(String innhold) throws IOException {
        if(innhold == null || innhold.isEmpty()) {
            return false;
        }
        for(String streng : GYLDIG_RESPONS_INNHOLD) {
            if(innhold.startsWith(streng)) {
                return true;
            }
        }
        if(innhold.length() > MIN_VALID_CONTENT_LENGTH) {
            return true;
        }
        return false;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = appendSlashIfNotPresent(baseUrl);
    }

    private String appendSlashIfNotPresent(String inputBaseUrl) {
        if (!SLASH.equals(inputBaseUrl.substring(inputBaseUrl.length() - 1))) {
            inputBaseUrl += SLASH;
        }
        return inputBaseUrl;
    }

    public int getHttpTimeoutMillis() {
        return httpTimeoutMillis;
    }

    public void setHttpTimeoutMillis(int httpTimeout) {
        this.httpTimeoutMillis = httpTimeout;
        HttpParams httpParams = getHttpClient().getParams();
        HttpConnectionParams.setSoTimeout(httpParams, httpTimeoutMillis);
        HttpConnectionParams.setConnectionTimeout(httpParams, httpTimeoutMillis);

    }

    public List getFeilmeldinger() {
        return this.feilmeldinger;
    }
    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void setCacheName(String cacheName) {
        if (cacheManager.cacheExists(cachename)) {
            logger.debug("Removing cache: {}", cachename);
            cacheManager.removeCache(cachename);
        }
        cachename = cacheName;
        if (!cacheManager.cacheExists(cachename)) {
            logger.debug( "Creating cache: {}", cachename );
            cacheManager.addCacheIfAbsent(cacheName);
        }
    }

    protected void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.setHttpTimeoutMillis(httpTimeoutMillis);
    }

    public void flushCache() {
        if(cacheManager.cacheExists(cachename)) {
            logger.warn( WARN_MELDING_FLUSHER_CACHEN, cachename );
            cacheManager.getCache(cachename).removeAll();
        }
    }

    public void refreshCache() {
        int hardcodeTTLtoEnsureCacheIsUpdated = -1;
        if (cacheManager.cacheExists(cachename)) {
            logger.warn( WARN_MELDING_REFRESH_CACHE, cachename );
            Cache c = cacheManager.getCache(cachename);
            for (Object key : c.getKeys()) {
                final String url = (String) key;
                if(c.getQuiet(key).getObjectValue() instanceof Properties) {
                    getPropertiesFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
                } else {
                    getPageContentFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
                }
            }
        }
    }

    public synchronized List getAllElements () {
        if(!cacheManager.cacheExists(this.cachename)) {
            return new ArrayList();
        }
        List liste = new LinkedList();
        Cache c = cacheManager.getCache(this.cachename);
        List keys = c.getKeys();
        for (Object o: keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }

    private HttpClient getHttpClient() {
        return httpClient;
    }
}
