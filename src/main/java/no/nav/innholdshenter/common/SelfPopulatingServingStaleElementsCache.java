package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;

public class SelfPopulatingServingStaleElementsCache extends SelfPopulatingCache {
    Logger logger = LoggerFactory.getLogger(SelfPopulatingServingStaleElementsCache.class);
    private int timeToLiveSeconds;

    public SelfPopulatingServingStaleElementsCache(Ehcache cache, CacheEntryFactory factory, int timeToLiveSeconds) throws CacheException {
        super(cache, factory);
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    @Override
    public Element get(Object key) throws LockTimeoutException {
        Element oldElement;
        try {
            oldElement = super.get(key);
        } catch (RuntimeException e) {
            logger.info("Feil ved henting av key {}", key, e);
            throw e;
        }
        Element newElement = doElementUpdate(oldElement);
        return newElement;
    }

    private Element doElementUpdate(Element oldElement) {
        if(isElementExpired(oldElement)) {
            try {
                refreshElement(oldElement, this.getCache());
            } catch (Exception e) {
                return oldElement;
            }
            return super.get(oldElement.getObjectKey());
        }
        return oldElement;
    }

    private boolean isElementExpired(Element element) {
        long now = System.currentTimeMillis();
        long expirationTime = element.getLastUpdateTime() + (getTimeToLiveSeconds() * 1000);
        return now > expirationTime;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }
}
