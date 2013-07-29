package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfPopulatingServingStaleElementsCache extends SelfPopulatingCache {
    private int timeToLiveSeconds;

    public SelfPopulatingServingStaleElementsCache(Ehcache cache, CacheEntryFactory factory, int timeToLiveSeconds) throws CacheException {
        super(cache, factory);
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    @Override
    public Element get(Object key) throws LockTimeoutException {
        Element element = super.get(key);
        if(isElementExpired(element)) {
             element = getUpdatedElement(element);
        }
        return element;
    }

    private Element getUpdatedElement(Element oldElement) {
        try {
            refreshElement(oldElement, this.getCache());
        } catch (Exception e) {
            return oldElement;
        }
        return super.get(oldElement.getObjectKey());
    }

    public boolean isElementExpired(Element element) {
        long now = System.currentTimeMillis();
        long expirationTime = element.getCreationTime() + (timeToLiveSeconds * 1000);
        return now > expirationTime;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }
}
