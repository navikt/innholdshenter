package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class GenericCache<T> {
    private static final Logger logger = LoggerFactory.getLogger(GenericCache.class);
    private static final String FEILMELDING_KLARTE_IKKE_HENTE_INNHOLD_FOR_CACHE_KEY = "Klarte ikke hente innhold for cache key %s. Bruker cache. Feilmelding: %s";
    private static final String FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE = "Henting fra url %s feilet og innholdet er ikke i cache.";
    private static final String FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG = "Henting fra url %s gikk gjennom, men innholdet var ikke som forventet. Cache ikke oppdatert.";

    private CacheManager cacheManager;

    private int refreshIntervalSeconds;
    private String cacheKey;
    private String cacheName;

    public GenericCache(CacheManager cacheManager, int refreshIntervalSeconds, String cacheKey) {
        this.cacheManager = cacheManager;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
        this.cacheKey = cacheKey;
    }

    public GenericCache(CacheManager cacheManager, int refreshIntervalSeconds, String cacheKey, String cacheName) {
        this(cacheManager, refreshIntervalSeconds, cacheKey);
        this.setCacheName(cacheName);
    }

    @SuppressWarnings("unchecked")
    public T fetch() {
        T cacheContent;
        Cache c = cacheManager.getCache(cacheName);
        Element element = c.get(cacheKey);

        if (elementIsOutdatedOrMissing(element)) {
            try {
                cacheContent = getContentFromSource();
                if(!isContentValid(cacheContent)) {
                    logger.warn(String.format(FEILMELDING_KLARTE_HENTE_INNHOLD_MEN_INNHOLDET_VAR_UGYLDIG, cacheKey));
                    throw new IOException();
                }
                Element newElement;
                if(element == null) {
                    newElement = new Element(cacheKey, cacheContent);
                    element = newElement;
                }
                element.setEternal(true);
                c.put(element);
            } catch (IOException e) {
                logger.warn(String.format(FEILMELDING_KLARTE_IKKE_HENTE_INNHOLD_FOR_CACHE_KEY, cacheKey, e.getMessage()), e);

            }
        }
        if(element == null) {
            logger.error(String.format(FEILMEDLING_KLARTE_IKKE_HENTE_INNHOLD_OG_INNHOLDET_FINNES_IKKE_I_CACHE, cacheKey));
            return null;
        }
        cacheContent = (T) element.getObjectValue();
        return cacheContent;
    }

    private boolean elementIsOutdatedOrMissing(Element element) {
        return element == null || isExpired(element);
    }

    protected abstract T getContentFromSource() throws IOException;

    protected boolean isContentValid(T content) throws IOException {
        // if we have content, is it valid?
        // TODO: actually test content
        return true;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
        if(!cacheManager.cacheExists(cacheName)) {
            int max_elements = 1000;
            boolean overflow_to_disk = false;
            boolean isEternal = true;

            cacheManager.addCache( new Cache(cacheName, max_elements, overflow_to_disk, isEternal, 0, 0) );
        }

    }
    private boolean isExpired(Element element) {
        long now = System.currentTimeMillis();
        long expirationTime = element.getCreationTime()+this.refreshIntervalSeconds*1000;
        return now > expirationTime;
    }

}
