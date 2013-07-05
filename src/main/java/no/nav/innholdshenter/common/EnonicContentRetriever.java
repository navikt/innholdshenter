package no.nav.innholdshenter.common;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private static final String WARN_MELDING_REFRESH_CACHE = "Refresh cachen: %s";
    private static final String INFO_LAGE_NY_UNIK_URL_FEILET = "Feilet å lage ny unik url, url: %s.";

    private String baseUrl;
    private int httpTimeoutMillis;
    private HttpClient httpClient;

    private CacheManager cacheManager;
    private String cachename;
    private int refreshIntervalSeconds;


    public EnonicContentRetriever() {
        if(cacheManager == null) {
            cacheManager = CacheManager.create();
        }
        httpClient = new DefaultHttpClient();
    }
    public EnonicContentRetriever(String cachename) {
        this();
        this.cachename = cachename;
        this.setCacheName(cachename);
    }

    public String getPageContent(String path) {
        final String url = createUrl(path);

        GenericCache<String> genericCache = new GenericCache<String>(cacheManager, refreshIntervalSeconds, url, cachename) {
            protected String getContentFromSource() throws IOException {
                return getPageContentFromUrl(url);
            }
        };

        return genericCache.fetch();
    }

    public Properties getProperties(String propertiesPath) {
        final String url = createUrl(propertiesPath);

        GenericCache<Properties> genericCache = new GenericCache<Properties>(cacheManager, refreshIntervalSeconds, url, cachename) {
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
        String randomUrl = makeRandomUrl(url);
        logger.debug(String.format(DEBUG_RETRIEVING_PAGE_CONTENT_FROM_URL, randomUrl));
        HttpParams httpParams = httpClient.getParams();
        HttpConnectionParams.setSoTimeout(httpParams, httpTimeoutMillis);
        HttpConnectionParams.setConnectionTimeout(httpParams, httpTimeoutMillis);
        HttpGet httpGet = new HttpGet(randomUrl);
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        return httpClient.execute(httpGet, responseHandler);
    }

    public static String makeRandomUrl(String url) {
        String sidToAvoidServerCache = RandomStringUtils.randomAlphanumeric(15);
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameter("sid", sidToAvoidServerCache);
            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            logger.info(String.format(INFO_LAGE_NY_UNIK_URL_FEILET, url));
        }
        return url;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = appendSlashIfNotPresent(baseUrl);
    }

    private String appendSlashIfNotPresent(String inputBaseUrl) {
        if (!SLASH.equals(inputBaseUrl.substring(inputBaseUrl.length() - 1))) {
            inputBaseUrl += SLASH;
        }
        return inputBaseUrl;
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

    public void setCacheName(String cacheName) {
        if (!cacheManager.cacheExists(cachename)) {
            logger.debug(String.format("Removing cache: ", cachename));
            cacheManager.removeCache(cachename);
        }
        cachename = cacheName;
        if (!cacheManager.cacheExists(cachename)) {
            logger.debug(String.format("Creating cache: ", cachename));
            cacheManager.addCacheIfAbsent(cacheName);
        }
    }

    protected void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void flushCache() {
        if(cacheManager.cacheExists(cachename)) {
            logger.warn( String.format(WARN_MELDING_FLUSHER_CACHEN, cachename) );
            cacheManager.getCache(cachename).removeAll();
        }
    }

    public void refreshCache() {
        if(cacheManager.cacheExists(cachename)) {
            logger.warn( String.format(WARN_MELDING_REFRESH_CACHE, cachename) );
            Cache c = cacheManager.getCache(cachename);
            for(Object key : c.getKeys()) {
                final String url = (String) key;
                int hardcode_TTL_to_ensure_cache_is_updated = -1;
                GenericCache<String> genericCache = new GenericCache<String>(cacheManager, hardcode_TTL_to_ensure_cache_is_updated, url, cachename) {
                    protected String getContentFromSource() throws IOException {
                        return getPageContentFromUrl(url);
                    }
                };
                genericCache.fetch();
            }
        }
    }

    public synchronized List getAllElements () {
        if(!cacheManager.cacheExists(this.cachename)) {
            return new ArrayList();
        }
        List liste = new LinkedList();
        Cache c = cacheManager.getCache(this.cachename);
        List keys = c.getKeys();
        for (Object o: keys) {
            liste.add(c.getQuiet(o));
        }
        return liste;
    }
}
