package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import no.nav.innholdshenter.common.EhcacheTestListener.ListenerStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverFullTest extends EnonicContentRetrieverTestSetup {

    @Test
    public void skal_Oppdatere_Utdaterte_Cachede_Properties_I_Cache_fra_URL() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);
        cache.put(new Element(URL, CACHED_PROPERTIES_2));
        testListener.resetStatus();

        Thread.sleep((REFRESH_INTERVAL + 2) * 1000);
        Properties result = contentRetriever.getProperties(PATH);

        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(PROPERTIES, result);
        assertEquals(ListenerStatus.ELEMENT_UPDATED, testListener.getLastStatus());
    }

    @Test
    public void skalHenteIkkeCachetInnholdFraUrl() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

        testListener.resetStatus();
        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
    }

    @Test
    public void skalHenteCachetInnholdFraCache() throws Exception {
        cache.put(new Element(URL, CACHED_CONTENT));
        testListener.resetStatus();
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }

    @Test
    public void skalHenteGammeltInnholdFraCacheHvisUrlFeiler() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenThrow(new IOException());
        cache.put(new Element(URL, CACHED_CONTENT));
        testListener.resetStatus();

        Thread.sleep((REFRESH_INTERVAL + 2) * 1000);
        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.RESET, testListener.getLastStatus());
    }

    @Test
    public void shouldNotCallHttpURLIfCacheNotOutdated() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

        cache.put(new Element(URL, CACHED_CONTENT));
        testListener.resetStatus();

        Thread.sleep((REFRESH_INTERVAL - 1) * 1000);
        String result = contentRetriever.getPageContent(PATH);

        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(CACHED_CONTENT, result);
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }

    @Test(expected = CacheException.class)
    public void shouldReturnNullIfNoCachedCopyAndNoResponseOnURL() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenThrow(new IOException());
        testListener.resetStatus();

        String result;
        result = contentRetriever.getPageContent(PATH);

        assertNull(result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.ELEMENT_REMOVED, testListener.getLastStatus());
    }

    @Test
    public void skalHenteIkkeCachedePropertiesFraUrl() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);

        testListener.resetStatus();
        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(PROPERTIES, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.ELEMENT_UPDATED, testListener.getLastStatus());
    }

    @Test
    public void skalHenteCachedePropertiesFraCache() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT_2);
        cache.put(new Element(URL, CACHED_PROPERTIES));
        testListener.resetStatus();

        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(CACHED_PROPERTIES, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }
}
