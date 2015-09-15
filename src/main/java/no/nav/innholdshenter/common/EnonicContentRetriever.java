package no.nav.innholdshenter.common;

import net.sf.ehcache.*;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
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
    private boolean nodeSyncing = false;

    private InnholdshenterGroupCommunicator groupCommunicator;
    private Map<String, CacheStatusMelding> cacheStatusMeldinger;

    private String baseUrl;

    private CacheManager cacheManager;
    private SelfPopulatingServingStaleElementsCache cache;
    private EnonicCacheEntryFactory enonicCacheEntryFactory;
    private String uniqueAppName;
    private int refreshIntervalSeconds;
    private static final int DEFAULT_HTTP_TIMEOUT = 3000;

    private String cacheName = "innholdshenterCache";

    protected EnonicContentRetriever() {
        cacheStatusMeldinger = new ConcurrentHashMap<String, CacheStatusMelding>();
        cacheManager = CacheManager.create();
    }

    public EnonicContentRetriever(String uniqueAppName) {
        this();
        this.uniqueAppName = uniqueAppName;
        setupCache(DEFAULT_HTTP_TIMEOUT);
    }


    public EnonicContentRetriever(String uniqueAppName, int httpTimeoutMillis) {
        this();
        this.uniqueAppName = uniqueAppName;
        setupCache(httpTimeoutMillis);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public EnonicContentRetriever(String uniqueAppName, boolean nodeSyncing, String jGroupsHosts, int jGroupsBindPort) throws Exception {
        this();
        this.uniqueAppName = uniqueAppName;
        setupCache(DEFAULT_HTTP_TIMEOUT);
        configureJGroups(nodeSyncing, jGroupsHosts, jGroupsBindPort);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public EnonicContentRetriever(String uniqueAppName, String cacheName, boolean nodeSyncing, String jGroupsHosts, int jGroupsBindPort) throws Exception {
        this();
        this.uniqueAppName = uniqueAppName;
        this.cacheName = cacheName;
        setupCache(DEFAULT_HTTP_TIMEOUT);
        configureJGroups(nodeSyncing, jGroupsHosts, jGroupsBindPort);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void configureJGroups(boolean nodeSyncing, String jGroupsHosts, int jGroupsBindPort) throws Exception {
        this.nodeSyncing = nodeSyncing;
        if (this.nodeSyncing) {
            this.groupCommunicator = new InnholdshenterGroupCommunicator(this.uniqueAppName, jGroupsHosts, jGroupsBindPort, this);
        }
    }

    private String createUrl(String path) {
        return InnholdshenterTools.sanitizeUrlCacheKey(baseUrl + path);
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url);
    }

    public String getPageContentFullUrl(final String url) {
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
            logger.warn("Feil i konvertering fra xml til Properties objekt.", ex.getMessage());
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

    public synchronized void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private synchronized void setupCache(int httpTimeoutMillis) {
        if (cacheManager.cacheExists(this.cacheName)) {
            return;
        }
        Cache oldCache = new Cache(this.cacheName, 1000, false, true, 0, 0);
        cacheManager.addCache(oldCache);
        enonicCacheEntryFactory = new EnonicCacheEntryFactory(cacheStatusMeldinger, httpTimeoutMillis);

        Ehcache ehcache = cacheManager.getEhcache(cacheName);
        cache = new SelfPopulatingServingStaleElementsCache(ehcache, enonicCacheEntryFactory, getRefreshIntervalSeconds());

        logger.debug("Creating cache: {}", cacheName);
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
        logger.warn(WARN_MELDING_REFRESH_CACHE, cacheName);
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

    public void setGroupCommunicator(InnholdshenterGroupCommunicator groupCommunicator) {
        this.groupCommunicator = groupCommunicator;
    }

    public String getCacheName() {
        return this.cacheName;
    }

    public SelfPopulatingServingStaleElementsCache getCache() {
        return cache;
    }

    /* brukes av cachestatuspaneler */
    public boolean isElementExpired(Element element) {
        return cache.isElementExpired(element);
    }

    /* brukes av cachestatuspaneler */
    public List<Address> getClusterMembers() {
        if (!nodeSyncing) {
            return new LinkedList<Address>();
        }
        return groupCommunicator.getMembers();
    }

    /* brukes av cachestatuspaneler */
    public Element getContentIfExists(Object key) {
        if (cache.isKeyInCache(key)) {
            return cache.getQuiet(key);
        }
        return new Element(key, "");
    }

    /* brukes av cachestatuspaneler */
    public synchronized List<Element> getAllElements() {
        if (!cacheManager.cacheExists(cacheName)) {
            return new LinkedList<Element>();
        }
        List<Element> liste = new LinkedList<Element>();
        Ehcache c = cacheManager.getEhcache(cacheName);
        List keys = c.getKeys();
        for (Object o : keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }
    //used for test purposes
    public void setHttpClient(HttpClient client) {
        enonicCacheEntryFactory.setHttpClient(client);
    }
}
