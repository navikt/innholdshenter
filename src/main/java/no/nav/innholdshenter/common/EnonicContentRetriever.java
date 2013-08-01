package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


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
    private Map<String, CacheStatusMelding> cacheStatusMeldinger;

    private String baseUrl;

    private CacheManager cacheManager;
    private SelfPopulatingServingStaleElementsCache cache;
    private EnonicCacheEntryFactory enonicCacheEntryFactory;
    private String uniqueAppName;
    private int refreshIntervalSeconds;

    private final static String CACHE_NAME = "innholdshenterCache";
    private int maxElements = 1000;
    private boolean overflowToDisk = false;
    private boolean neverExpireCacheLines = true;
    private int httpTimeoutMillis;
    private HttpClient httpClient;


    protected EnonicContentRetriever() {
        cacheStatusMeldinger = new ConcurrentHashMap<String, CacheStatusMelding>();
        cacheManager = CacheManager.create();
        httpClient = new DefaultHttpClient();
        setHttpTimeoutMillis(3000);
        setupCache();
    }

    public EnonicContentRetriever(String uniqueAppName) {
        this();
        this.uniqueAppName = uniqueAppName;
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public EnonicContentRetriever(String uniqueAppName, boolean nodeSyncing) throws Exception {
        this(uniqueAppName);
        this.nodeSyncing = nodeSyncing;
        if (this.nodeSyncing) {
            this.groupCommunicator = new InnholdshenterGroupCommunicator(this.uniqueAppName, this);
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
        Element element = cache.get(url);
        return (String) element.getObjectValue();
    }

    public Properties getProperties(String path) {
        final String url = createUrl(path);
        return getPropertiesFullUrl(url);
    }

    public Properties getPropertiesFullUrl(final String url) {
        Element element = cache.get(url);
        return getPropertiesOrConvertIfNeeded(element);
    }

    private Properties getPropertiesOrConvertIfNeeded(Element element) {
        if (element.getObjectValue() instanceof Properties) {
            return (Properties) element.getObjectValue();
        }
        Properties properties = convertElementToProperties(element);
        Element convertedElement = storeConvertedObject((String) element.getObjectKey(), properties);
        return (Properties) convertedElement.getObjectValue();
    }

    private Element storeConvertedObject(String key, Object object) {
        Element newElement = new Element(key, object);
        cache.put(newElement);
        return newElement;
    }

    private Properties convertElementToProperties(Element e) {
        Properties properties = new Properties();
        String content = (String) e.getObjectValue();
        try {
            ByteArrayInputStream propertiesStream = new ByteArrayInputStream(content.getBytes(LOCALE_UTF_8));
            properties.loadFromXML(propertiesStream);
        } catch (IOException ex) {
            logger.error("Feil i konvertering fra xml til Properties objekt.", ex);
            throw new RuntimeException("Feil: Kunne ikke hente data.", ex);
        }
        return properties;
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

    public Map<String, CacheStatusMelding> getCacheStatusMeldinger() {
        return this.cacheStatusMeldinger;
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
        if (cacheManager.cacheExists(this.CACHE_NAME)) {
            return;
        }
        Cache oldCache = new Cache(this.CACHE_NAME, maxElements, overflowToDisk, neverExpireCacheLines, 0, 0);
        cacheManager.addCache(oldCache);
        enonicCacheEntryFactory =
                new EnonicCacheEntryFactory(getHttpClient(), cacheStatusMeldinger);

        Ehcache ehcache = cacheManager.getEhcache(CACHE_NAME);
        cache = new SelfPopulatingServingStaleElementsCache(ehcache, enonicCacheEntryFactory, getRefreshIntervalSeconds());

        logger.debug("Creating cache: {}", CACHE_NAME);
        cacheManager.replaceCacheWithDecoratedCache(ehcache, cache);
        cache.setStatusMeldinger(cacheStatusMeldinger);
    }

    protected void broadcastRefresh() {
        if (nodeSyncing) {
            logger.info("Sending refresh sync broadcast.");

            try {
                groupCommunicator.sendUpdateToNodes();
            } catch (Exception e) {
                logger.error("Syncing cache refresh with nodes failed: ", e);
            }
        }
    }

    public void refreshCache(boolean broadcastRefresh) {
        logger.warn(WARN_MELDING_REFRESH_CACHE, CACHE_NAME);
        if (broadcastRefresh && nodeSyncing) {
            broadcastRefresh();
        } else {
            try {
                cache.refresh(false);
            } catch (CacheException ce) {
                logger.error("feil under refresh av cache", ce);
            }
        }
    }

    public synchronized List<Element> getAllElements() {
        if (!cacheManager.cacheExists(CACHE_NAME)) {
            return new LinkedList<Element>();
        }
        List liste = new LinkedList<Element>();
        Ehcache c = cacheManager.getEhcache(CACHE_NAME);
        List keys = c.getKeys();
        for (Object o : keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }

    public void setGroupCommunicator(InnholdshenterGroupCommunicator groupCommunicator) {
        this.groupCommunicator = groupCommunicator;
    }

    public String getCacheName() {
        return this.CACHE_NAME;
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

    public boolean isElementExpired(Element element) {
        return cache.isElementExpired(element);
    }

    public List<Address> getClusterMembers() {
        if(!nodeSyncing) {
            return new LinkedList<Address>();
        }
        return groupCommunicator.getMembers();
    }

    public Element getContentIfExists(Object key) {
        if(cache.isKeyInCache(key)) {
            return cache.getQuiet(key);
        }
        return new Element(key, null);
    }

}
