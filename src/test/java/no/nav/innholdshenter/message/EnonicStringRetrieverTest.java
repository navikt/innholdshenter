package no.nav.innholdshenter.message;

import no.nav.innholdshenter.common.ContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Properties;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tester for {@link EnonicStringRetriever}
 */
@RunWith(MockitoJUnitRunner.class)
public class EnonicStringRetrieverTest {
    @Mock
    private ContentRetriever contentRetriever;

    @Mock
    private MessageListener messageListener;

    private static final String PATH = "systemsider/ledetekster/appnavn";
    private static final Properties PROPERTIES = new Properties();
    private static final Properties PROPERTIES_EN = new Properties();
    private static final Properties PROPERTIES_INGEN = new Properties();

    static {
        PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land");
        PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform");
        PROPERTIES.setProperty("kontaktinfo.overskrifter.spraak", "Språk");

        PROPERTIES_EN.setProperty("kontaktinfo.overskrifter.spraak", "Language");
        PROPERTIES_INGEN.setProperty("kontaktinfo.overskrifter.spraak", "Ikke tilgjengelig");
    }

    @Before
    public void setUp() {
        when(contentRetriever.getProperties(PATH + "?locale=no_NO&variant=")).thenReturn(PROPERTIES);
        when(contentRetriever.getProperties(PATH + "?locale=&variant=")).thenReturn(PROPERTIES);
        when(contentRetriever.getProperties(PATH + "?locale=en_US&variant=")).thenReturn(PROPERTIES_EN);
        when(contentRetriever.getProperties(PATH + "?locale=no_NO&variant=ingendata")).thenReturn(PROPERTIES_INGEN);
    }

    @Test
    public void skalHentePropertySomFinnes() throws Exception {
        EnonicStringRetriever retriever = new EnonicStringRetriever(contentRetriever, PATH);
        String property = retriever.retrieveString("cv.kontaktdetaljer.kontaktinfo.land", "no_NO");
        assertEquals("Land", property);
    }

    @Test
    public void skalHentePropertySomIkkeFinnes() throws Exception {
        StringRetriever stringHenter = new EnonicStringRetriever(contentRetriever, PATH);
        String property = stringHenter.retrieveString("a", "no_NO");
        assertEquals("<b>[a locale:no_NO, variant:null]</b>", property);
    }

    @Test
    public void skalKalleMessageListeners() {
        MessageListenerAdapter adapter = new MessageListenerAdapter(new EnonicStringRetriever(contentRetriever, PATH));
        adapter.setMessageListeners(asList(messageListener));

        when(messageListener.onMessageRetrieved("Land")).thenReturn("-Land-");

        String property = adapter.retrieveString("cv.kontaktdetaljer.kontaktinfo.land", "no_NO", null);

        verify(messageListener).onMessageRetrieved("Land");

        assertEquals("-Land-", property);
    }

    @Test
    public void skalHentePropertyUtenLocale() {
        EnonicStringRetriever retriever = new EnonicStringRetriever(contentRetriever, PATH);
        String property = retriever.retrieveString("cv.kontaktdetaljer.kontaktinfo.land");
        assertEquals("Land", property);
    }

    @Test
    public void skalHenteTekstForaGittLocale() {
        EnonicStringRetriever retriever = new EnonicStringRetriever(contentRetriever, PATH);
        assertEquals("Språk", retriever.retrieveString("kontaktinfo.overskrifter.spraak", "no_NO", null));
        assertEquals("Language", retriever.retrieveString("kontaktinfo.overskrifter.spraak", "en_US", ""));

    }

    @Test
    public void skalHenteTekstForaGittVariant() {
        EnonicStringRetriever retriever = new EnonicStringRetriever(contentRetriever, PATH);
        assertEquals("Språk", retriever.retrieveString("kontaktinfo.overskrifter.spraak", "no_NO", null));
        assertEquals("Ikke tilgjengelig", retriever.retrieveString("kontaktinfo.overskrifter.spraak", "no_NO", "ingendata"));
    }
}
