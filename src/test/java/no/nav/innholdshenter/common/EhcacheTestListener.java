package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/*
 * Listener for aa lytte paa hva som skjer i ehcachen.
 */
public class EhcacheTestListener implements CacheEventListener {
    public static enum ListenerStatus {
        RESET, DISPOSED, ELEMENT_EVICTED, ELEMENT_EXPIRED, ELEMENT_ADDED,
        ELEMENT_REMOVED, ELEMENT_UPDATED, REMOVED_ALL, ELEMENT_RETURNED,
    }

    public ListenerStatus lastStatus;

    public ListenerStatus getLastStatus() {
        return lastStatus;
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        this.lastStatus = ListenerStatus.ELEMENT_REMOVED;
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        this.lastStatus = ListenerStatus.ELEMENT_ADDED;
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        this.lastStatus = ListenerStatus.ELEMENT_UPDATED;
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {
        this.lastStatus = ListenerStatus.ELEMENT_EXPIRED;
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
        this.lastStatus = ListenerStatus.ELEMENT_EVICTED;
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {
        this.lastStatus = ListenerStatus.REMOVED_ALL;
    }

    @Override
    public void dispose() {
        this.lastStatus = ListenerStatus.DISPOSED;
    }

    @Override
    public Object clone() {
        return new EhcacheTestListener();
    }

    public void resetStatus() {
        this.lastStatus = ListenerStatus.RESET;
    }
}
