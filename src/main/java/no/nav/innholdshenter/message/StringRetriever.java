package no.nav.innholdshenter.message;

/**
 * Interface for string retriever
 */
public interface StringRetriever {
    String retrieveString(String key, String locale);

    String retrieveString(String key, String locale, String variant);
}
