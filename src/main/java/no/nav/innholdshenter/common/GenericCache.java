package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

abstract class GenericCache<T> {
    private static final Logger logger = LoggerFactory.getLogger(GenericCache.class);
    private static final String FEILMELDING_KLARTE_IKKE_HENTE_INNHOLD_FOR_CACHE_KEY = "Klarte ikke hente innhold for cache key {}. Bruker cache. Feilmelding: {}";
    private static final String FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE = "Henting fra url {} feilet og innholdet er ikke i cache.";
    private static final String FEILMELDING_CACHELINJE_ER_UTDATERT = "Cachelinjen er utdatert med key: {} TimeToLive: {} seconds";
    private static final String INFO_CACHELINJEN_FANTES_IKKE_I_CACHE = "Cachelinjen fantes ikke i cache.";
    private static final String INFO_CACHEHIT = "Cachehit for: {} TTL: {} sec.";

    private CacheManager cacheManager;

    private int refreshIntervalSeconds;
    private String cacheKey;
    private String cacheName;

    public GenericCache(CacheManager cacheManager, int refreshIntervalSeconds, String cacheKey, String cacheName) {
        this.cacheManager = cacheManager;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        this.cacheKey = cacheKey;
        this.cacheName = cacheName;
    }

    @SuppressWarnings("unchecked")
    public T fetch() {
        Ehcache c = cacheManager.getEhcache(cacheName);
        Element element = c.get(cacheKey);

        if (elementIsOutdatedOrMissing(element)) {
            element = fetchNewCacheContent(element, c);
        } else {
            logger.debug(INFO_CACHEHIT, cacheKey, refreshIntervalSeconds);
        }
        if (element == null) {
            logger.error(FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE, cacheKey);
            throw new IllegalStateException(FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE);
        }
        T cacheContent = (T) element.getObjectValue();
        return cacheContent;
    }

    private Element fetchNewCacheContent(Element element, Ehcache c) {
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

    private boolean isExpired(Element element) {
        long now = System.currentTimeMillis();
        long expirationTime = element.getCreationTime() + (this.refreshIntervalSeconds * 1000);
        if (now > expirationTime) {
            logger.debug(FEILMELDING_CACHELINJE_ER_UTDATERT, element.getObjectKey(), refreshIntervalSeconds);
            return true;
        }
        return false;
    }

}
