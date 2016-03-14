package no.nav.innholdshenter.hjelpetekst;

import no.nav.innholdshenter.common.ContentRetriever;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jdom2.output.Format.getRawFormat;

/**
 * Henter hjelpetekster (i html) fra EnonicContentRetriever basert på nøkkel.
 */
public class EnonicHtmlInnholdBean {

    private static final Logger logger = LoggerFactory.getLogger(EnonicHtmlInnholdBean.class);
    private static final String HTMLINNHOLD_ELEMENT = "htmlinnhold";
    private static final String TITLE_ELEMENT = "title";
    private static final String HTML_ELEMENT = "html";
    private static final String LOCALE_UTF_8 = "UTF-8";
    private static final String INNHOLDSLISTE_ELEMENT = "innholdsliste";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String KEY_URL_QUERY_PATH = "?key=";
    private static final String FEILMELDING_FEIL_VED_HENTING_AV_HTMLINNHOLD_MED_KEY = "Feil ved henting av htmlinnhold med key '{}': {}";

    private String helptextPath;

    private EnonicDocumentRetriever enonicDocumentRetriever;
    private List<HtmlInnholdListener> htmlInnholdListeners = new ArrayList<HtmlInnholdListener>();

    public EnonicHtmlInnholdBean(ContentRetriever retriever, String helptextPath) {
        setRetriever(retriever);
        this.helptextPath = helptextPath;
    }

    public EnonicHtmlInnholdBean() {
    }

    public HtmlInnhold getHjelpetekst(Object key) {
        HtmlInnhold htmlInnhold = null;
        try {
            Document document = enonicDocumentRetriever.loadDocument(helptextPath + KEY_URL_QUERY_PATH + key);
            Element rootElement = document.getRootElement();
            if (HTMLINNHOLD_ELEMENT.equals(rootElement.getName())) {
                htmlInnhold = new HtmlInnhold();
                htmlInnhold.setTitle(rootElement.getChildText(TITLE_ELEMENT));
                htmlInnhold.setHtml(getHtml(rootElement.getChild(HTML_ELEMENT)));
                htmlInnhold = processHtmlInnhold(htmlInnhold);
            }
        } catch (IllegalStateException e) {
            logger.error(FEILMELDING_FEIL_VED_HENTING_AV_HTMLINNHOLD_MED_KEY, key, e.getMessage());
        }
        return htmlInnhold;
    }

    private String getHtml(Element element) {
        String htmlString = null;
        try {
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(getRawFormat().setEncoding(LOCALE_UTF_8));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            xmlOutputter.output(element.getChildren(), outputStream);
            htmlString = new String(outputStream.toByteArray(), LOCALE_UTF_8);
        } catch (IOException e) {
            logger.error("Kunne ikke skrive html for element");
        }
        return htmlString;
    }

    public List<HtmlInnhold> getHjelpetekster() {
        List<HtmlInnhold> hjelpetekster = new ArrayList<HtmlInnhold>();
        try {
            Document document = enonicDocumentRetriever.loadDocument(helptextPath);
            Element rootElement = document.getRootElement();
            if (INNHOLDSLISTE_ELEMENT.equals(rootElement.getName())) {
                List children = rootElement.getChildren(HTMLINNHOLD_ELEMENT);
                for (Object child : children) {
                    Element element = (Element) child;
                    if (HTMLINNHOLD_ELEMENT.equals(element.getName())) {
                        HtmlInnhold htmlInnhold = new HtmlInnhold();
                        htmlInnhold.setTitle(element.getAttributeValue(TITLE_ELEMENT));
                        htmlInnhold.setKey(element.getAttributeValue(KEY_ATTRIBUTE));
                        hjelpetekster.add(processHtmlInnhold(htmlInnhold));
                    }
                }
            }
        } catch (IllegalStateException e) {
            logger.error("Feil ved henting av hjelpetekster fra url {}", helptextPath);
        }

        return hjelpetekster;
    }

    private HtmlInnhold processHtmlInnhold(HtmlInnhold htmlInnhold) {
        HtmlInnhold result = htmlInnhold;
        for (HtmlInnholdListener htmlInnholdListener : htmlInnholdListeners) {
            result = htmlInnholdListener.onHtmlInnholdRetrieved(result);
        }
        return result;
    }

    public void setHelptextPath(String helptextPath) {
        this.helptextPath = helptextPath;
    }

    private void setRetriever(ContentRetriever retriever) {
        enonicDocumentRetriever = new EnonicDocumentRetriever();
        enonicDocumentRetriever.setContentRetriever(retriever);
    }

    public void setHtmlInnholdListeners(List<HtmlInnholdListener> htmlInnholdListeners) {
        this.htmlInnholdListeners = htmlInnholdListeners;
    }
}
