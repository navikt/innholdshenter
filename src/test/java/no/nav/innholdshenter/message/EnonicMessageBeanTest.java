package no.nav.innholdshenter.message;

import no.nav.innholdshenter.common.EnonicContentRetriever;
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
 * Testklasse for EnonicMessageBean.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnonicMessageBeanTest {

    @Mock
    private EnonicContentRetriever contentRetriever;

    @Mock
    private MessageListener messageListener;

    private static final String PATH = "systemsider/nav-sbl-arbeid-properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        PROPERTIES.setProperty("cv.kontaktdetaljer.kontaktinfo.land", "Land");
        PROPERTIES.setProperty("kontaktinfo.overskrifter.maalform", "Ønsket målform");
    }

    @Before
    public void setUp() {
        when(contentRetriever.getProperties(PATH)).thenReturn(PROPERTIES);
    }

    @Test
    public void skalHentePropertySomFinnes() throws Exception {
        EnonicMessageBean messageBean = new EnonicMessageBean(contentRetriever, PATH);
        String property = messageBean.get("cv.kontaktdetaljer.kontaktinfo.land");
        assertEquals("Land", property);
    }

    @Test
    public void skalHentePropertySomIkkeFinnesIDebugModus() throws Exception {
        EnonicMessageBean messageBean = new EnonicMessageBean(contentRetriever, PATH);
        messageBean.setDebugMode(true);
        String property = messageBean.get("a");
        assertEquals("<b>[a]</b>", property);
    }


    @Test
    public void skalHentePropertySomIkkeFinnes() throws Exception {
        EnonicMessageBean messageBean = new EnonicMessageBean(contentRetriever, PATH);
        String property = messageBean.get("a");
        assertEquals("", property);
    }

    @Test
    public void skalKalleMessageListeners() {
        EnonicMessageBean messageBean = new EnonicMessageBean(contentRetriever, PATH);
        messageBean.setMessageListeners(asList(messageListener));
        when(messageListener.onMessageRetrieved("Land")).thenReturn("-Land-");

        String property = messageBean.get("cv.kontaktdetaljer.kontaktinfo.land");

        verify(messageListener).onMessageRetrieved("Land");

        assertEquals("-Land-", property);
    }


}
