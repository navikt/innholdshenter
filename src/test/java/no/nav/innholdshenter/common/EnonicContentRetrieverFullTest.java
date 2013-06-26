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
import static junit.framework.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import no.nav.innholdshenter.common.EhcacheTestListener.ListenerStatus;

/**
 * Minst mulig Mock Testklasse for EnonicContentRetriever.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverFullTest {

    private String cacheName = "TestEhcacheCacheName";

    private CacheManager cacheManager;
    private Cache cache;
    private Element element;
    private EhcacheTestListener testListener;
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
    private static final int REFRESH_INTERVAL = 5;
    private static final String CONTENT = "<html><body>Innhold</body></html>";
    private static final String CACHED_CONTENT = "<html><body>Cachet innhold</body></html>";

    private static final String PROPERTIES_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.land\">Land</entry>" +
            "<entry key=\"kontaktinfo.overskrifter.maalform\">Ønsket målform</entry>" +
            "</properties>";

    private static final String PROPERTIES_CONTENT_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.tlf\">Telefon</entry>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.epost\">Epost</entry>" +
            "</properties>";

    private static final Properties PROPERTIES = new Properties();
    private static final Properties PROPERTIES_2 = new Properties();
    private static final Properties CACHED_PROPERTIES = new Properties();
    private static final Properties CACHED_PROPERTIES_2 = new Properties();

    static {
        PROPERTIES_2.setProperty("cv.kontaktdetaljer.kontaktinfo.tlf", "Telefon");
        PROPERTIES_2.setProperty("cv.kontaktdetaljer.kontaktinfo.epost", "Epost");
        PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land");
        PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform");
        CACHED_PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land (cached)");
        CACHED_PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform (cached)");
        CACHED_PROPERTIES_2.setProperty("cv.kontaktdetaljer.kontaktinfo.tlf", "Telefon  (cached)");
        CACHED_PROPERTIES_2.setProperty("cv.kontaktdetaljer.kontaktinfo.epost", "Epost (cached)");

    }

    @Before
    public void setUp() {
        when(httpClient.getParams()).thenReturn(httpParams);
        when(httpClient.getConnectionManager()).thenReturn(connectionManager);

        testListener = new EhcacheTestListener();
        cacheManager = CacheManager.create();
        if(cacheManager.cacheExists(cacheName)) {
            cacheManager.getCache(cacheName).removeAll();
            cacheManager.removeCache(cacheName);
        }
        cacheManager.addCache(cacheName);
        cache = cacheManager.getCache(cacheName);
        cache.getCacheEventNotificationService().registerListener(testListener);

        contentRetriever = new EnonicContentRetriever(cacheName);
        contentRetriever.setCacheManager(cacheManager);
        contentRetriever.setHttpClient(httpClient);
        contentRetriever.setBaseUrl(SERVER);
        contentRetriever.setRefreshIntervalSeconds(REFRESH_INTERVAL);
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
        cache.put(new Element(URL, CACHED_PROPERTIES));
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
