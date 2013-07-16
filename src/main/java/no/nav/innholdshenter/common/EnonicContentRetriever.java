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
    private static final List<String> GYLDIG_RESPONS_INNHOLD = Arrays.asList("<html", "<xml", "<properties", "<?xml ", "<!DOCTYPE ");
    private static final int MIN_VALID_CONTENT_LENGTH = 60;
    private static boolean nodeSyncing = true;

    private InnholdshenterGroupCommunicator groupCommunicator;
    private List feilmeldinger;

    private String baseUrl;
    private HttpClient httpClient;
    private int httpTimeoutMillis;

    private CacheManager cacheManager;
    private static String uniqueAppName;
    private int refreshIntervalSeconds;


    public EnonicContentRetriever() {
        feilmeldinger = new ArrayList<String>();
        if (cacheManager == null) {
            cacheManager = CacheManager.create();
        }
        httpClient = new DefaultHttpClient();
        setHttpTimeoutMillis(3000);
    }
    public EnonicContentRetriever(String uniqueAppName) {
        this();
        this.uniqueAppName = uniqueAppName;
        this.setAppName(uniqueAppName);
    }

    public EnonicContentRetriever(String uniqueAppName, boolean nodeSyncing) throws Exception {
        this(uniqueAppName);
        this.nodeSyncing = nodeSyncing;
        if(nodeSyncing) {
            setGroupCommunicator(new InnholdshenterGroupCommunicator(uniqueAppName, this));
        }
    }

    private String createUrl(String path) {
        return baseUrl + path;
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url, getRefreshIntervalSeconds());
    }

    private String getPageContentFullUrl(final String url, int timeToLiveSeconds) {
        GenericCache<String> genericCache = new GenericCache<String>(cacheManager, timeToLiveSeconds, url, uniqueAppName) {
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
        GenericCache<Properties> genericCache = new GenericCache<Properties>(cacheManager, timeToLiveSeconds, url, uniqueAppName) {
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

    private void setAppName(String uniqueAppName) {
        if (cacheManager.cacheExists(this.uniqueAppName)) {
            logger.debug("Removing cache: {}", this.uniqueAppName);
            cacheManager.removeCache(this.uniqueAppName);
        }
        this.uniqueAppName = uniqueAppName;
        if (!cacheManager.cacheExists(this.uniqueAppName)) {
            logger.debug( "Creating cache: {}", this.uniqueAppName);
            cacheManager.addCacheIfAbsent(this.uniqueAppName);
        }
    }

    protected void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.setHttpTimeoutMillis(httpTimeoutMillis);
    }

    public void flushCache() {
        if(cacheManager.cacheExists(uniqueAppName)) {
            logger.warn( WARN_MELDING_FLUSHER_CACHEN, uniqueAppName);
            cacheManager.getCache(uniqueAppName).removeAll();
        }
    }

    protected void broadcastRefresh() {
        if(groupCommunicator != null) {
            try {
                this.groupCommunicator.sendUpdateToNodes();
            } catch (Exception e) {
                logger.error("Syncing cache refresh with nodes failed: {}", e);
            }
        }
    }

    public void refreshCache(boolean broadcastRefresh) {
        int hardcodeTTLtoEnsureCacheIsUpdated = -1;
        if (!cacheManager.cacheExists(uniqueAppName)) {
            logger.warn("refreshCache: ingen cache med navnet {} ble funnet!", uniqueAppName);
            return;
        }
        logger.warn( WARN_MELDING_REFRESH_CACHE, uniqueAppName);
        Cache c = cacheManager.getCache(uniqueAppName);
        for (Object key : c.getKeys()) {
            final String url = (String) key;
            if(c.getQuiet(key).getObjectValue() instanceof Properties) {
                getPropertiesFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
            } else {
                getPageContentFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
            }
        }
        if(broadcastRefresh)
            broadcastRefresh();
    }

    public void refreshCache() {
        refreshCache(true);
    }

    public synchronized List getAllElements () {
        if(!cacheManager.cacheExists(this.uniqueAppName)) {
            return new ArrayList();
        }
        List liste = new LinkedList();
        Cache c = cacheManager.getCache(this.uniqueAppName);
        List keys = c.getKeys();
        for (Object o: keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }

    public void setGroupCommunicator(InnholdshenterGroupCommunicator groupCommunicator) {
        this.groupCommunicator = groupCommunicator;
    }

    private HttpClient getHttpClient() {
        return httpClient;
    }
}
