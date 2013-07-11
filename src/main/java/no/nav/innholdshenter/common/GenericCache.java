package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

abstract class GenericCache<T> {
    private static final Logger logger = LoggerFactory.getLogger(GenericCache.class);
    private static final String FEILMELDING_KLARTE_IKKE_HENTE_INNHOLD_FOR_CACHE_KEY = "Klarte ikke hente innhold for cache key {}. Bruker cache. Feilmelding: {}";
    private static final String FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE = "Henting fra url {} feilet og innholdet er ikke i cache.";
    private static final String FEILMELDING_CACHELINJE_ER_UTDATERT = "Cachelinjen er utdatert med key: {} TimeToLive: {} seconds";
    private static final String INFO_CACHELINJEN_FANTES_IKKE_I_CACHE = "Cachelinjen fantes ikke i cache.";
    private static final String WARN_FLUSHER_CACHEN = "Flusher cachen: {}";
    private static final String INFO_CACHEHIT = "Cachehit for: {} TTL: {} sec.";

    private CacheManager cacheManager;

    private int refreshIntervalSeconds;
    private String cacheKey;
    private String cacheName;

    private int maxElements = 1000;
    private boolean overflowToDisk = false;
    private boolean neverExpireCacheLines = true;



    public GenericCache(CacheManager cacheManager, int refreshIntervalSeconds, String cacheKey, String cacheName) {
        this.cacheManager = cacheManager;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        this.cacheKey = sanitizeUrlCacheKey(cacheKey);
        setCacheName(cacheName);
    }

    @SuppressWarnings("unchecked")
    public T fetch() {
        Cache c = cacheManager.getCache(cacheName);
        Element element = c.get(cacheKey);

        if (elementIsOutdatedOrMissing(element)) {
            element = fetchNewCacheContent(element, c);
        } else {
            logger.debug(INFO_CACHEHIT, cacheKey, refreshIntervalSeconds);
        }
        if (element == null) {
            logger.error(FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE, cacheKey);
            return null;
        }
        T cacheContent = (T) element.getObjectValue();
        return cacheContent;
    }

    private Element fetchNewCacheContent(Element element, Cache c) {
        T cacheContent;
        try {
            cacheContent = getContentFromSource();
            element = new Element(cacheKey, cacheContent);
            element.setEternal(true);

            c.put(element);
        } catch (IOException e) {
            logger.warn(FEILMELDING_KLARTE_IKKE_HENTE_INNHOLD_FOR_CACHE_KEY, cacheKey, e.getMessage(), e);
        }
        return element;
    }

    private boolean elementIsOutdatedOrMissing(Element element) {
        if (element == null || element.getObjectValue() == null) {
            logger.debug(INFO_CACHELINJEN_FANTES_IKKE_I_CACHE);
            return true;
        }
        return isExpired(element);
    }

    protected abstract T getContentFromSource() throws IOException;

    private void setCacheName(String cacheName) {
        this.cacheName = cacheName;
        if (!cacheManager.cacheExists(cacheName)) {
            cacheManager.addCache(new Cache(cacheName, maxElements, overflowToDisk, neverExpireCacheLines, 0, 0));
        }

    }

    private boolean isExpired(Element element) {
        long now = System.currentTimeMillis();
        long expirationTime = element.getCreationTime() + (this.refreshIntervalSeconds * 1000);
        if (now > expirationTime) {
            logger.debug(FEILMELDING_CACHELINJE_ER_UTDATERT, element.getObjectKey(), refreshIntervalSeconds);
            return true;
        }
        return false;
    }

    /**
    * Sanitize url before storage in cache. This part of the url tends to include session specific data,
    * so it is often unique, and thrashes the cache.
    * Use this return value as the index in the cache, and not the full url.
    *
    * @param url
    * @return returns a cleaner url, suitable for the cacheline.
    *
    */
    private String sanitizeUrlCacheKey(String url) {
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
            List<NameValuePair> params = uriBuilder.getQueryParams();
            for (NameValuePair nameValuePair : params) {
                if (nameValuePair.getName().startsWith("urlPath")) {
                    String urlpath = sanitizeUrlPath(nameValuePair.getValue());
                    uriBuilder.setParameter("urlPath", urlpath);
                }
            }
        } catch (URISyntaxException e) {
            logger.debug(e.getMessage());
            return url;
        }
        return uriBuilder.toString();
    }

    private String sanitizeUrlPath(String urlParam) {
        if (urlParam != null && !urlParam.isEmpty()) {
            return urlParam.split(",")[0];
        }
        return urlParam;
    }

}
