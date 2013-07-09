package no.nav.innholdshenter.common;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverFeilmeldingTest extends EnonicContentRetrieverTestSetup {

    @Test
    public void notfound_feil_skal_lage_feilmelding_i_listen() throws Exception{
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenThrow(new HttpResponseException(404, "Not found"));

        String result = contentRetriever.getPageContent(PATH);
        List<CacheStatusFeilmelding> feil = contentRetriever.getFeilmeldinger();
        assertTrue(feil.size()>=1);
        int errorcode = feil.get(0).getStatusCode();
        assertTrue(errorcode == 404);
        assertEquals("Not found", feil.get(0).getFeilmelding());
    }

    @Test
    public void forbidden_feil_skal_lage_feilmelding_i_listen() throws Exception{
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenThrow(new HttpResponseException(403, "Forbidden"));

        String result = contentRetriever.getPageContent(PATH);
        List<CacheStatusFeilmelding> feil = contentRetriever.getFeilmeldinger();
        assertTrue(feil.size()>=1);
        int errorcode = feil.get(0).getStatusCode();
        assertTrue(errorcode == 403);
        assertEquals("Forbidden", feil.get(0).getFeilmelding());
    }
}
