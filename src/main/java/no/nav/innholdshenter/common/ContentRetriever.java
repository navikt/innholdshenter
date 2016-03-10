package no.nav.innholdshenter.common;

import java.util.Properties;

public interface ContentRetriever {
    String getPageContent(String path);

    String getPageContentFullUrl(String url);

    Properties getProperties(String path);

    Properties getPropertiesFullUrl(String url);

    void setBaseUrl(String baseUrl);
}
