package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import no.nav.innholdshenter.common.EhcacheTestListener.ListenerStatus;
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

@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverCacheFlushTest {

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
        public void flush_cache_should_give_an_empty_cache_and_adding_a_new_element_should_return_this() throws Exception {
            when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class))).thenReturn(CONTENT);

            testListener.resetStatus();
            String result = contentRetriever.getPageContent(PATH);
            assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
            assertEquals(CONTENT, result);
            verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));


            testListener.resetStatus();
            result = contentRetriever.getPageContent(PATH);
            assertEquals(ListenerStatus.RESET, testListener.getLastStatus());
            assertEquals(CONTENT, result);

            testListener.resetStatus();
            contentRetriever.flushCache();
            assertEquals(ListenerStatus.REMOVED_ALL, testListener.getLastStatus());

            testListener.resetStatus();
            result = contentRetriever.getPageContent(PATH);
            assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
            assertEquals(CONTENT, result);

        }
}
