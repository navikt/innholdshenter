package no.nav.innholdshenter.common;

import net.sf.ehcache.Element;
import no.nav.innholdshenter.common.EhcacheTestListener.ListenerStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverFullTest extends EnonicContentRetrieverTestSetup {

    @Test
    public void skal_ikke_legge_inn_ugyldig_innhold() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(CONTENT)
                .thenReturn(UGYLDIG_INNHOLD);

        String result = contentRetriever.getPageContent(PATH);
        assertEquals(CONTENT, result);

        Thread.sleep((REFRESH_INTERVAL+1)*1000);

        testListener.resetStatus();
        result = contentRetriever.getPageContent(PATH);

        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(CONTENT, result);
        assertEquals(ListenerStatus.RESET, testListener.getLastStatus());
    }
    @Test
    public void skal_Oppdatere_Utdaterte_Cachede_Properties_I_Cache_fra_URL() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);
        cache.put(new Element(URL, CACHED_PROPERTIES_2));
        testListener.resetStatus();

        Thread.sleep((REFRESH_INTERVAL+1)*1000);
        Properties result = contentRetriever.getProperties(PATH);

        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(PROPERTIES, result);
        assertEquals(testListener.getLastStatus(), ListenerStatus.ELEMENT_UPDATED);
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

        Thread.sleep((REFRESH_INTERVAL+1)*1000);
        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }

    @Test
    public void shouldNotCallHttpURLIfCacheNotOutdated() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

        cache.put(new Element(URL, CACHED_CONTENT));
        testListener.resetStatus();

        Thread.sleep((REFRESH_INTERVAL-1)*1000);
        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }

    @Test
    public void shouldReturnNullIfNoCachedCopyAndNoResponseOnURL() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenThrow(new IOException());
        testListener.resetStatus();

        String result = contentRetriever.getPageContent(PATH);

        assertNull(result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
    }

    @Test
    public void skalReturnereInnholdFraUrlVedSamtidigAksess() throws Exception {
        contentRetriever.setHttpClient(new DefaultHttpClient());
        contentRetriever.setBaseUrl("http://maven.adeo.no:80");
        contentRetriever.setHttpTimeoutMillis(5000);

        Thread thread1 = new Thread(new RetrieverWorker("nexus/content/repositories/central/org/apache/solr/"));
        Thread thread2 = new Thread(new RetrieverWorker("nexus/content/repositories/central/org/apache/wicket/"));

        thread1.start();
        thread2.start();

        contentRetriever.getPageContent("nexus/content/repositories/central/org/apache/tomcat/");

        thread1.join();
        thread2.join();
    }

    @Test
    public void skalHenteIkkeCachedePropertiesFraUrl() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);

        testListener.resetStatus();
        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(PROPERTIES, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.ELEMENT_ADDED);
    }


    @Test
    public void skalHenteCachedePropertiesFraCache() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT_2);
        cache.put(new Element(URL, PROPERTIES_CONTENT_CACHED));
        testListener.resetStatus();

        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(CACHED_PROPERTIES, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(testListener.getLastStatus(), ListenerStatus.RESET);
        //verify(cache, never()).cancelUpdate(URL);
    }

    /**
     * Worker thread for getPageContent.
     */
    class RetrieverWorker implements Runnable {

        private String path;

        public RetrieverWorker(String path) {
            this.path = path;
        }

        public void run() {
            contentRetriever.getPageContent(path);
        }
    }
}
