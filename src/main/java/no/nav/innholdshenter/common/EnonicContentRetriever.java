package no.nav.innholdshenter.common;

import net.sf.ehcache.*;
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
    private static final String WARN_MELDING_REFRESH_CACHE = "Refresh cachen: {}";
    private boolean nodeSyncing = true;

    private InnholdshenterGroupCommunicator groupCommunicator;
    private Map<String, CacheStatusFeilmelding> feilmeldinger;

    private String baseUrl;

    private CacheManager cacheManager;
    private SelfPopulatingServingStaleElementsCache cache;
    private EnonicCacheEntryFactory enonicCacheEntryFactory;
    private String uniqueAppName;
    private int refreshIntervalSeconds;

    private final static String cacheName = "innholdshenter_cache";
    private int maxElements = 1000;
    private boolean overflowToDisk = false;
    private boolean neverExpireCacheLines = true;
    private int httpTimeoutMillis;
    private HttpClient httpClient;


    protected EnonicContentRetriever() {
        feilmeldinger = new HashMap<String, CacheStatusFeilmelding>();
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
        if(this.nodeSyncing) {
            this.groupCommunicator = new InnholdshenterGroupCommunicator(uniqueAppName, this);
        }
    }

    private String createUrl(String path) {
        return InnholdshenterTools.sanitizeUrlCacheKey(baseUrl + path);
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url);
    }

    private String getPageContentFullUrl(final String url) {
        Element element;
        try {
            element = cache.get(url);
        } catch (RuntimeException e) {
            return null;
        }
        return element.getObjectValue().toString();
    }

    public Properties getProperties(String path) {
        final String url = createUrl(path);
        return getPropertiesFullUrl(url);
    }

    public Properties getPropertiesFullUrl(final String url) {
        Element e = cache.get(url);
        if(e.getObjectValue() instanceof Properties) {
            return (Properties) e.getObjectValue();
        }
        Properties properties = new Properties();
        String content = e.getObjectValue().toString();
        try {
            ByteArrayInputStream propertiesStream = new ByteArrayInputStream(content.getBytes(LOCALE_UTF_8));
            properties.loadFromXML(propertiesStream);
        } catch (IOException ex) {
            logger.error("Feil i konvertering fra xml til Properties objekt.", ex);
            throw new RuntimeException("Feil: Kunne ikke hente data.", ex);
        }
        Element element = new Element(url, properties);
        cache.put(element);
        return (Properties)element.getObjectValue();
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

    public Map<String, CacheStatusFeilmelding> getFeilmeldinger() {
        return this.feilmeldinger;
    }
    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        cache.setTimeToLiveSeconds(refreshIntervalSeconds);
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private synchronized void setupCache() {
        if (cacheManager.cacheExists(this.cacheName)) {
            return;
        }
        Cache oldCache = new Cache(this.cacheName, maxElements, overflowToDisk, neverExpireCacheLines, 0, 0);
        cacheManager.addCache(oldCache);
        enonicCacheEntryFactory =
                new EnonicCacheEntryFactory(getHttpClient(), feilmeldinger);

        Ehcache ehcache = cacheManager.getEhcache(cacheName);
        cache = new SelfPopulatingServingStaleElementsCache(ehcache, enonicCacheEntryFactory, getRefreshIntervalSeconds());

        logger.debug("Creating cache: {}", cacheName);
        cacheManager.replaceCacheWithDecoratedCache(ehcache, cache);
    }

    protected void broadcastRefresh() {
        if(groupCommunicator != null) {
            try {
                groupCommunicator.sendUpdateToNodes();
            } catch (Exception e) {
                logger.error("Syncing cache refresh with nodes failed: {}", e);
            }
        }
    }

    public void refreshCache(boolean broadcastRefresh) {
        int hardcodeTTLtoEnsureCacheIsUpdated = -1;
        if (cache == null) {
            logger.warn("refreshCache: ingen cache med navnet {} ble funnet!", cacheName);
            return;
        }
        logger.warn( WARN_MELDING_REFRESH_CACHE, cacheName);
        try {
            cache.refresh(false);
        } catch (CacheException ce) {
            logger.error("feil under refresh av cache", ce);
        }
        if(broadcastRefresh) {
            broadcastRefresh();
        }
    }

    public synchronized List<Element> getAllElements () {
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

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        setHttpTimeoutMillis(httpTimeoutMillis);
        enonicCacheEntryFactory.setHttpClient(httpClient);
    }

    public SelfPopulatingServingStaleElementsCache getCache() {
        return cache;
    }
}
