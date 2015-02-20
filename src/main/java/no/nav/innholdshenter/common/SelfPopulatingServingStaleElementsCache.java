package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

import java.util.Map;

public class SelfPopulatingServingStaleElementsCache extends SelfPopulatingCache {
    private int timeToLiveSeconds;
    private int fetchTimeoutIntervalMillis;
    private Map<String, CacheStatusMelding> statusMeldinger;

    public SelfPopulatingServingStaleElementsCache(Ehcache cache, CacheEntryFactory factory, int timeToLiveSeconds) throws CacheException {
        super(cache, factory);
        this.timeToLiveSeconds = timeToLiveSeconds;
        fetchTimeoutIntervalMillis = (timeToLiveSeconds * 1000) / 4;
    }

    @Override
    public Element get(Object key) throws LockTimeoutException {
        Element element = super.get(key);
        if (isElementExpired(element)) {
            element = getUpdatedElement(element);
        }
        return element;
    }

    private Element getUpdatedElement(Element oldElement) {
        if (statusMeldinger.containsKey(oldElement.getObjectKey())) {
            long lastRefresh = statusMeldinger.get(oldElement.getObjectKey()).getTimestamp();
            if (!isTimeForRefresh(lastRefresh)) {
                return oldElement;
            }
        }
        try {
            refreshElement(oldElement, this.getCache());
        } catch (Exception e) {
            return oldElement;
        }
        return super.get(oldElement.getObjectKey());
    }

    private boolean isTimeForRefresh(long lastRefresh) {
        long now = System.currentTimeMillis();
        return now > (lastRefresh + this.fetchTimeoutIntervalMillis);
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
        fetchTimeoutIntervalMillis = (timeToLiveSeconds * 1000) / 4;
    }

    public int getFetchTimeoutIntervalMillis() {
        return fetchTimeoutIntervalMillis;
    }

    public void setFetchTimeoutIntervalMillis(int fetchTimeoutIntervalMillis) {
        this.fetchTimeoutIntervalMillis = fetchTimeoutIntervalMillis;
    }

    public void setStatusMeldinger(Map<String, CacheStatusMelding> statusMeldinger) {
        this.statusMeldinger = statusMeldinger;
    }

}
