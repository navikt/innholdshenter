package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.blocking.BlockingCache;
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
import java.util.*;


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
    private static final String HTTP_STATUS_FEIL = "Http-kall feilet, url: {} status: {} grunn: {}";
    private static final List<String> GYLDIG_RESPONS_INNHOLD = Arrays.asList("<html", "<xml", "<properties", "<?xml ", "<!DOCTYPE ");
    private static final int MIN_VALID_CONTENT_LENGTH = 60;
    private static boolean nodeSyncing = true;

    private List<CacheRefreshListener> cacheRefreshListeners;
    private InnholdshenterGroupCommunicator groupCommunicator;
    private Map feilmeldinger;

    private String baseUrl;
    private HttpClient httpClient;
    private int httpTimeoutMillis;

    private CacheManager cacheManager;
    private static String uniqueAppName;
    private int refreshIntervalSeconds;
    private final static String cacheName = "innholdshenter_cache";

    private int maxElements = 1000;
    private boolean overflowToDisk = false;
    private boolean neverExpireCacheLines = true;


    protected EnonicContentRetriever() {
        feilmeldinger = new HashMap<String, CacheStatusFeilmelding>();
        cacheRefreshListeners = new LinkedList<CacheRefreshListener>();
        if (cacheManager == null) {
            cacheManager = CacheManager.create();
        }
        httpClient = new DefaultHttpClient();
        setHttpTimeoutMillis(3000);
        setupCache();
    }
    public EnonicContentRetriever(String uniqueAppName) {
        this();
        this.uniqueAppName = uniqueAppName;
    }

    public EnonicContentRetriever(String uniqueAppName, boolean nodeSyncing) throws Exception {
        this(uniqueAppName);
        this.nodeSyncing = nodeSyncing;
        if(nodeSyncing) {
            this.groupCommunicator = new InnholdshenterGroupCommunicator(uniqueAppName, this);
        }
    }

    private String createUrl(String path) {
        return InnholdshenterTools.sanitizeUrlCacheKey(baseUrl + path);
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url, getRefreshIntervalSeconds());
    }

    private String getPageContentFullUrl(final String url, int timeToLiveSeconds) {
        GenericCache<String> genericCache = new GenericCache<String>(cacheManager, timeToLiveSeconds, url, cacheName) {
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
        GenericCache<Properties> genericCache = new GenericCache<Properties>(cacheManager, timeToLiveSeconds, url, cacheName) {
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
            CacheStatusFeilmelding c = new CacheStatusFeilmelding(200, "OK", System.currentTimeMillis());
            feilmeldinger.put(url, c);
        } catch(HttpResponseException exception) {
            logger.error(HTTP_STATUS_FEIL, url, exception.getStatusCode(), exception.getMessage());
            CacheStatusFeilmelding c = new CacheStatusFeilmelding(exception.getStatusCode(), exception.getMessage(), System.currentTimeMillis());
            feilmeldinger.put(url, c);
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

    public Map getFeilmeldinger() {
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

    private synchronized void setupCache() {
        if (!cacheManager.cacheExists(this.cacheName)) {
            cacheManager.addCache(new Cache(this.cacheName, maxElements, overflowToDisk, neverExpireCacheLines, 0, 0));
            Ehcache oldcache = cacheManager.getEhcache(cacheName);
            BlockingCache blockingCache = new BlockingCache(oldcache);

            logger.debug("Creating cache: {}", cacheName);
            cacheManager.replaceCacheWithDecoratedCache(oldcache, blockingCache);
        }
    }

    protected void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.setHttpTimeoutMillis(httpTimeoutMillis);
    }

    public void flushCache() {
        if(cacheManager.cacheExists(cacheName)) {
            logger.warn( WARN_MELDING_FLUSHER_CACHEN, cacheName);
            cacheManager.getEhcache(cacheName).removeAll();
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
        if (!cacheManager.cacheExists(cacheName)) {
            logger.warn("refreshCache: ingen cache med navnet {} ble funnet!", cacheName);
            return;
        }
        logger.warn( WARN_MELDING_REFRESH_CACHE, cacheName);
        Ehcache c = cacheManager.getEhcache(cacheName);
        for (Object key : c.getKeys()) {
            final String url = (String) key;
            if(c.getQuiet(key).getObjectValue() instanceof Properties) {
                getPropertiesFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
            } else {
                getPageContentFullUrl(url, hardcodeTTLtoEnsureCacheIsUpdated);
            }
        }
        if(broadcastRefresh) {
            broadcastRefresh();
        } else {
            for(CacheRefreshListener listener : cacheRefreshListeners) {
                listener.refreshReceived();
            }
        }
    }

    public void addCacheRefreshListener(CacheRefreshListener cacheRefreshListener) {
        if(cacheRefreshListeners.contains(cacheRefreshListener))
            return;
        cacheRefreshListeners.add(cacheRefreshListener);
    }

    public void removeCacheRefreshListener(CacheRefreshListener cacheRefreshListener) {
        if(cacheRefreshListeners.contains(cacheRefreshListener)) {
            cacheRefreshListeners.remove(cacheRefreshListener);
        }
    }

    public synchronized List getAllElements () {
        if(!cacheManager.cacheExists(cacheName)) {
            return new ArrayList();
        }
        List liste = new LinkedList();
        Ehcache c = cacheManager.getEhcache(cacheName);
        List keys = c.getKeys();
        for (Object o: keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }

    public void setGroupCommunicator(InnholdshenterGroupCommunicator groupCommunicator) {
        this.groupCommunicator = groupCommunicator;
    }

    public String getCacheName() {
        return this.cacheName;
    }

    private HttpClient getHttpClient() {
        return httpClient;
    }
}
