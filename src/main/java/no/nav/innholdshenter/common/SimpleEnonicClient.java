package no.nav.innholdshenter.common;

import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class SimpleEnonicClient implements ContentRetriever {
    private static final Logger logger = LoggerFactory.getLogger(SimpleEnonicClient.class);

    private static final String RETRIEVING_PAGE_CONTENT_FROM_URL = "Retrieving page content from url {}";
    private static final String ERROR_RETRIEVING_PAGE_CONTENT_FROM_URL = "Error retrieving content from url {}";
    private String baseUrl;

    private HttpClient httpClient;

    public SimpleEnonicClient(String baseUrl) {
        this.httpClient = new DefaultHttpClient(new PoolingClientConnectionManager());
        this.baseUrl = baseUrl;
    }

    private static String makeFullUrl(String path, String baseUrl) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }

    @Override
    public String getPageContent(String path) {
        String url = makeFullUrl(path, baseUrl);
        return getPageContentFullUrl(url);
    }

    @Override
    public String getPageContentFullUrl(String url) {
        String uniqueRandomUrl = InnholdshenterTools.makeUniqueRandomUrl(url);
        HttpGet request = new HttpGet(uniqueRandomUrl);
        try {
            logger.info(RETRIEVING_PAGE_CONTENT_FROM_URL, url);
            return httpClient.execute(request, new BasicResponseHandler());
        } catch (IOException exception) {
            logger.error(ERROR_RETRIEVING_PAGE_CONTENT_FROM_URL, url);
            throw new RuntimeException("Http-kall feilet", exception);
        }
        finally {
            request.releaseConnection();
        }
    }

    @Override
    public Properties getProperties(String path) {
        String url = makeFullUrl(path, baseUrl);
        return getPropertiesFullUrl(url);
    }

    @Override
    public Properties getPropertiesFullUrl(String url) {
        String xmlstring = getPageContentFullUrl(url);

        Document document = Jsoup.parse(xmlstring, "", Parser.xmlParser());
        Map<String, String> map = document.select("text").stream().collect(Collectors.toMap(
                element -> unescapeHtml4(element.getElementsByTag("key").first().html()),
                element -> unescapeHtml4(element.getElementsByTag("value").first().html())
        ));

        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

