package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testklasse for EnonicContentRetriever.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Element element;
    @Mock
    private Cache cache;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpParams httpParams;
    @Mock
    private ClientConnectionManager connectionManager;

    private EnonicContentRetriever contentRetriever;

    private static final String SERVER = "http://localhost:9000";
    private static final String PATH = "systemsider/ApplicationFrame";
    private static final String URL = SERVER + "/" + PATH;
    private static final int REFRESH_INTERVAL = 300;
    private static final String CONTENT = "<html><body>Innhold</body></html>";
    private static final String CACHED_CONTENT = "<html><body>Cachet innhold</body></html>";

    private static final String PROPERTIES_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
          "<properties>" +
          "<entry key=\"cv.kontaktdetaljer.kontaktinfo.land\">Land</entry>" +
          "<entry key=\"kontaktinfo.overskrifter.maalform\">Ønsket målform</entry>" +
          "</properties>";

    private static final Properties PROPERTIES = new Properties();
    private static final Properties CACHED_PROPERTIES = new Properties();

    static {
        PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land");
        PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform");
        CACHED_PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land (cached)");
        CACHED_PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform (cached)");
    }

    @Before
    public void setUp() {
        when(httpClient.getParams()).thenReturn(httpParams);
        when(httpClient.getConnectionManager()).thenReturn(connectionManager);

        contentRetriever = new EnonicContentRetriever();
        contentRetriever.setCache(cacheManager);
        contentRetriever.setHttpClient(httpClient);
        contentRetriever.setBaseUrl(SERVER);
        contentRetriever.setRefreshIntervalSeconds(REFRESH_INTERVAL);
    }

    @Test
    public void skalHenteIkkeCachetInnholdFraUrl() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get(URL)).thenReturn(null);
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        verify(cache).put(any(Element.class));
        //verify(cache, never()).cancelUpdate(URL);
    }

    @Test
    public void skalHenteCachetInnholdFraCache() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);
        when(element.getObjectValue()).thenReturn(CACHED_CONTENT);
        when(element.getCreationTime()).thenReturn(System.currentTimeMillis());
        when(cache.get(URL)).thenReturn(element);

        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        verify(cache, never()).put(any(Element.class));
    }

    @Test
    public void skalHenteGammeltInnholdFraCacheHvisUrlFeiler() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get(URL)).thenReturn(element);
        when(element.getObjectValue()).thenReturn(CACHED_CONTENT);
        when(element.getCreationTime()).thenReturn(System.currentTimeMillis());
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenThrow(new IOException());

        String result = contentRetriever.getPageContent(PATH);

        assertEquals(CACHED_CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        verify(cache, never()).put(any(Element.class));
    }

    //@Test(expected = IllegalStateException.class)
    public void skalKasteIllegalStateExceptionHvisCacheOgUrlFeiler() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        //when(cacheManager.getFromCache(URL, REFRESH_INTERVAL)).thenThrow(new NeedsRefreshException(null));
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenThrow(new IOException());

        contentRetriever.getPageContent(PATH);
    }

    @Test
    public void skalReturnereInnholdFraUrlVedSamtidigAksess() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null);
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
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(cache.get(URL)).thenReturn(null);
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);

        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(PROPERTIES, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        verify(cache).put(any(Element.class));
        //verify(cacheManager, never()).cancelUpdate(URL);
    }

    @Test
    public void skalHenteCachedePropertiesFraCache() throws Exception {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(element.getObjectValue()).thenReturn(CACHED_PROPERTIES);
        when(cache.get(URL)).thenReturn(element);
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(PROPERTIES_CONTENT);

        Properties result = contentRetriever.getProperties(PATH);

        assertEquals(CACHED_PROPERTIES, result);
        verify(httpClient, never()).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        verify(cache, never()).put(any(Element.class));
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
