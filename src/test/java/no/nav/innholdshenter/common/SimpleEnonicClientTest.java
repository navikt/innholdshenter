package no.nav.innholdshenter.common;

import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.TestCase.assertEquals;

public class SimpleEnonicClientTest {

    private LocalTestServer localTestServer;

    public static final String ENONIC_RESPONSE =
            "<texts categorykey=\"8302\">\n" +
                "<text>\n" +
                    "<key>applikasjon.tittel</key>\n" +
                    "<value formatting=\"string\">Beslutningsstøtte for sykmeldere</value>\n" +
                "</text>\n" +
                "<text>\n" +
                    "<key>beslutningsstotte.ingen</key>\n" +
                    "<value formatting=\"string\">Det finnes ingen beslutningsstøtte for valgt diagnose.</value>\n" +
                "</text>" +
            "</texts>";

    private String baseUrl;

    @Before
    public void setUp() throws Exception {
        localTestServer = new LocalTestServer(null, null);
        localTestServer.register("/mypath", (httpRequest, httpResponse, httpContext) -> {
            httpResponse.setEntity(new StringEntity(ENONIC_RESPONSE));
            httpResponse.setStatusCode(200);
        });
        localTestServer.start();
        baseUrl = "http:/" + localTestServer.getServiceAddress().toString();
    }

    @After
    public void tearDown() throws Exception {
        localTestServer.stop();
    }

    @Test
    public void testGetPageContent() throws Exception {
        SimpleEnonicClient simpleEnonicClient = new SimpleEnonicClient(baseUrl);

        String pageContent = simpleEnonicClient.getPageContent("/mypath");

        assertEquals(ENONIC_RESPONSE, pageContent);
    }

    @Test
    public void testGetProperties() throws Exception {
        SimpleEnonicClient simpleEnonicClient = new SimpleEnonicClient(baseUrl);

        Properties properties = simpleEnonicClient.getProperties("/mypath");

        assertEquals(2, properties.size());
        assertEquals("Beslutningsstøtte for sykmeldere", properties.getProperty("applikasjon.tittel"));
        assertEquals("Det finnes ingen beslutningsstøtte for valgt diagnose.", properties.getProperty("beslutningsstotte.ingen"));
    }
}