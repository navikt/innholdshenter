package no.nav.innholdshenter.common;

import net.sf.ehcache.*;
import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Henter innholdet for en gitt URL. Hvis ferskt innhold finnes i cacheManager returneres det derfra.
 */
public class EnonicContentRetriever implements ContentRetriever {
    private static final Logger logger = LoggerFactory.getLogger(EnonicContentRetriever.class);
    private static final String SLASH = "/";
    private static final String LOCALE_UTF_8 = "UTF-8";
    private static final String WARN_MELDING_REFRESH_CACHE = "Refresh cachen: {}";
    private static final int DEFAULT_HTTP_TIMEOUT = 3000;

    private Map<String, CacheStatusMelding> cacheStatusMeldinger;
    private String baseUrl;
    private CacheManager cacheManager;
    private SelfPopulatingServingStaleElementsCache cache;
    private EnonicCacheEntryFactory enonicCacheEntryFactory;
    private int refreshIntervalSeconds;
    private String cacheName = "innholdshenterCache";

    public EnonicContentRetriever() {
        this(DEFAULT_HTTP_TIMEOUT);
    }

    public EnonicContentRetriever(int httpTimeoutMillis) {
        cacheStatusMeldinger = new ConcurrentHashMap<>();
        cacheManager = CacheManager.create();
        setupCache(httpTimeoutMillis);
    }

    public EnonicContentRetriever(int httpTimeoutMillis, String baseUrl, int refreshIntervalSeconds) {
        cacheStatusMeldinger = new ConcurrentHashMap<>();
        cacheManager = CacheManager.create();
        setupCache(httpTimeoutMillis);
        setBaseUrl(baseUrl);
        setRefreshIntervalSeconds(refreshIntervalSeconds);
    }

    private String createUrl(String path) {
        return InnholdshenterTools.sanitizeUrlCacheKey(baseUrl + path);
    }

    @Override
    public String getPageContent(String path) {
        final String url = createUrl(path);
        return getPageContentFullUrl(url);
    }

    @Override
    public String getPageContentFullUrl(final String url) {
        Element element = cache.get(url);
        return (String) element.getObjectValue();
    }

    @Override
    public Properties getProperties(String path) {
        final String url = createUrl(path);
        return getPropertiesFullUrl(url);
    }

    @Override
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

    @Override
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

    public void refreshCache() {
        logger.warn(WARN_MELDING_REFRESH_CACHE, cacheName);

        try {
            cache.refresh(false);
        } catch (CacheException ce) {
            logger.error("feil under refresh av cache", ce);
        }

    }
    public SelfPopulatingServingStaleElementsCache getCache() {
        return cache;
    }

    //used for test purposes
    public void setHttpClient(HttpClient client) {
        enonicCacheEntryFactory.setHttpClient(client);
    }
}
