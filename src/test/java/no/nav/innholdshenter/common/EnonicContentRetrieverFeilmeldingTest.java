package no.nav.innholdshenter.common;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverFeilmeldingTest extends EnonicContentRetrieverTestSetup {

    @Test
    public void notfound_feil_skal_lage_feilmelding_i_listen() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenThrow(new HttpResponseException(404, "Not found"));
        try {
            contentRetriever.getPageContent(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, CacheStatusMelding> feil = contentRetriever.getCacheStatusMeldinger();
        assertTrue(feil.size() >= 1);
        int errorcode = feil.get(URL).getStatusCode();
        assertTrue(errorcode == 404);
        assertEquals("Not found", feil.get(URL).getMelding());
    }

    @Test
    public void forbidden_feil_skal_lage_feilmelding_i_listen() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenThrow(new HttpResponseException(403, "Forbidden"));

        try {
            contentRetriever.getPageContent(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, CacheStatusMelding> feil = contentRetriever.getCacheStatusMeldinger();
        assertTrue(feil.size() >= 1);
        int errorcode = feil.get(URL).getStatusCode();
        assertTrue(errorcode == 403);
        assertEquals("Forbidden", feil.get(URL).getMelding());
    }

    @Test
    public void ok_request_skal_lage_melding_i_listen() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(CONTENT);

        try {
            contentRetriever.getPageContent(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, CacheStatusMelding> cacheStatusMeldinger = contentRetriever.getCacheStatusMeldinger();
        assertTrue(cacheStatusMeldinger.size() == 1);
        int statusCode = cacheStatusMeldinger.get(URL).getStatusCode();
        assertTrue(statusCode == 200);
        assertEquals("OK", cacheStatusMeldinger.get(URL).getMelding());
    }
}
