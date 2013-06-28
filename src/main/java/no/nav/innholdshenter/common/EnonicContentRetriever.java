package no.nav.innholdshenter.common;

import net.sf.ehcache.CacheManager;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Henter innholdet for en gitt URL. Hvis ferskt innhold finnes i cacheManager returneres det derfra.
 */
public class EnonicContentRetriever {
    private static final Logger logger = LoggerFactory.getLogger(EnonicContentRetriever.class);
    private static final String SLASH = "/";
    private static final String LOCALE_UTF_8 = "UTF-8";
    private static final String DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL = "Retrieving page content from url %s";
    private static final String WARN_MELDING_FLUSHER_CACHEN = "Flusher cachen: %s";

    private String baseUrl;
    private int httpTimeoutMillis;
    private HttpClient httpClient;

    private CacheManager cacheManager;
    private String CACHENAME;
    private int refreshIntervalSeconds;


    private EnonicContentRetriever() {
        if(cacheManager == null) {
            CacheManager.create();
        }
        httpClient = new DefaultHttpClient();
    }
    public EnonicContentRetriever(String CACHENAME) {
        this();
        this.CACHENAME = CACHENAME;
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);

        GenericCache<String> genericCache = new GenericCache<String>(cacheManager, refreshIntervalSeconds, url, CACHENAME) {
            protected String getContentFromSource() throws IOException {
                return getPageContentFromUrl(url);
            }
        };

        return genericCache.fetch();
    }

    public Properties getProperties(String propertiesPath) {
        final String url = createUrl(propertiesPath);

        GenericCache<Properties> genericCache = new GenericCache<Properties>(cacheManager, refreshIntervalSeconds, url, CACHENAME) {
            protected Properties getContentFromSource() throws IOException {
                String content = getPageContentFromUrl(url);
                ByteArrayInputStream propertiesStream = new ByteArrayInputStream(content.getBytes(LOCALE_UTF_8));
                Properties properties = new Properties();
                properties.loadFromXML(propertiesStream);
                return properties;
            }
        };
        return genericCache.fetch();
    }

    private String createUrl(String path) {
        return baseUrl + path;
    }

    private synchronized String getPageContentFromUrl(String url) throws IOException {
        logger.debug(String.format(DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL, url));
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, httpTimeoutMillis);
        HttpConnectionParams.setConnectionTimeout(httpParams, httpTimeoutMillis);
        HttpGet httpGet = new HttpGet(url);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        return httpClient.execute(httpGet, responseHandler);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = appendSlashIfNotPresent(baseUrl);
    }

    private String appendSlashIfNotPresent(String baseUrl) {
        if (!SLASH.equals(baseUrl.substring(baseUrl.length() - 1))) {
            baseUrl += SLASH;
        }
        return baseUrl;
    }

    public int getHttpTimeoutMillis() {
        return httpTimeoutMillis;
    }

    public void setHttpTimeoutMillis(int httpTimeout) {
        this.httpTimeoutMillis = httpTimeout;
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public void setRefreshIntervalSeconds(int refreshIntervalSeconds) {
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    protected void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void flushCache() {
        if(cacheManager.cacheExists(CACHENAME)) {
            logger.warn( String.format(WARN_MELDING_FLUSHER_CACHEN, CACHENAME) );
            cacheManager.getCache(CACHENAME).removeAll();
        }
    }
}
