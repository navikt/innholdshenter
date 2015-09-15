package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.junit.Before;
import org.mockito.Mock;

import java.util.Properties;

import static org.mockito.Mockito.when;

public class EnonicContentRetrieverTestSetup {

    protected static final String PATH = "systemsider/ApplicationFrame";
    protected static final String CACHE_NAME = "innholdshenterCache";
    protected static final String SERVER = "http://localhost:9000";
    protected static final String URL = SERVER + "/" + PATH;
    protected static final int REFRESH_INTERVAL = 5;

    protected static final String CONTENT = "<html><body>Innhold</body></html>";
    protected static final String CACHED_CONTENT = "<html><body>Cachet innhold</body></html>";

    protected static final String PROPERTIES_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.land\">Land</entry>" +
            "<entry key=\"kontaktinfo.overskrifter.maalform\">Ønsket målform</entry>" +
            "</properties>";
    protected static final String PROPERTIES_CONTENT_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" +
            "<properties>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.tlf\">Telefon</entry>" +
            "<entry key=\"cv.kontaktdetaljer.kontaktinfo.epost\">Epost</entry>" +
            "</properties>";


    @Mock
    private HttpParams httpParams;
    @Mock
    private ClientConnectionManager connectionManager;
    @Mock
    protected HttpClient httpClient;

    protected SelfPopulatingServingStaleElementsCache cache;
    protected EhcacheTestListener testListener;
    protected EnonicContentRetriever contentRetriever;
    protected CacheManager cacheManager;

    protected Element element;

    protected static final Properties PROPERTIES = new Properties();
    protected static final Properties PROPERTIES_2 = new Properties();
    protected static final Properties CACHED_PROPERTIES = new Properties();
    protected static final Properties CACHED_PROPERTIES_2 = new Properties();

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
    public void setUp() throws Exception {
        when(httpClient.getParams()).thenReturn(httpParams);
        when(httpClient.getConnectionManager()).thenReturn(connectionManager);

        testListener = new EhcacheTestListener();
        cacheManager = CacheManager.create();
        if (cacheManager.cacheExists(CACHE_NAME)) {
            cacheManager.removeCache(CACHE_NAME);
        }
        contentRetriever = new EnonicContentRetriever();
        contentRetriever.setCacheManager(cacheManager);
        contentRetriever.setBaseUrl(SERVER);
        contentRetriever.setRefreshIntervalSeconds(REFRESH_INTERVAL);
        contentRetriever.setHttpClient(httpClient);

        cache = contentRetriever.getCache();
        cache.getCacheEventNotificationService().registerListener(testListener);
    }
}
