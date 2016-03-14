package no.nav.innholdshenter.hjelpetekst;

import no.nav.innholdshenter.common.ContentRetriever;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderJDOMFactory;
import org.jdom2.input.sax.XMLReaders;

import java.io.ByteArrayInputStream;


/**
 * retrieves XML from vertical site and build jdom document
 */
class EnonicDocumentRetriever {
    private static final String LOCALE_UTF_8 = "UTF-8";
    private static final String FEILMELDING_COULD_NOT_LOAD_CONFIG_FROM_URL = "Could not load config from %s";
    private static final String FEILMELDING_COULD_NOT_PARSE_CONFIG_FROM_URL = "Could not parse config from %s";
    private ContentRetriever contentRetriever;

    public Document loadDocument(String configURL) {
        String configXML = contentRetriever.getPageContent(configURL);
        if (configXML == null || configXML.isEmpty()) {
            throw new RuntimeException(String.format(FEILMELDING_COULD_NOT_LOAD_CONFIG_FROM_URL, configURL));
        }
        return buildDocument(configXML, configURL);
    }

    private Document buildDocument(String configString, String configURL) {
        Document doc;
        SAXBuilder sb = new SAXBuilder();
        XMLReaderJDOMFactory novalidationfactory = XMLReaders.NONVALIDATING;
        sb.setXMLReaderFactory(novalidationfactory);

        try {
            doc = sb.build(new ByteArrayInputStream(configString.getBytes(LOCALE_UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(String.format(FEILMELDING_COULD_NOT_PARSE_CONFIG_FROM_URL, configURL), e);
        }
        return doc;
    }

    public void setContentRetriever(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }
}
