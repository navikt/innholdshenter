package no.nav.innholdshenter.common;

import no.nav.innholdshenter.common.EhcacheTestListener.ListenerStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnonicContentRetrieverCacheTest extends EnonicContentRetrieverTestSetup {
    private static final String OLD_CONTENT = "<html><body>Gammelt innhold</body></html>";
    private static final String NEW_CONTENT = "<html><body>Nytt innhold</body></html>";
    private static final String PATH2 = "systemsider/ledetekster";

    @Test
    public void refresh_cache_should_give_a_populated_cache_and_fetching_an_element_should_return_the_updated_content() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(OLD_CONTENT)
                .thenReturn(NEW_CONTENT)
                .thenReturn(CACHED_CONTENT);

        testListener.resetStatus();
        String result = contentRetriever.getPageContent(PATH);
        assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
        assertEquals(OLD_CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));

        testListener.resetStatus();
        contentRetriever.refreshCache();
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.ELEMENT_UPDATED, testListener.getLastStatus());

        testListener.resetStatus();
        result = contentRetriever.getPageContent(PATH);
        assertEquals(NEW_CONTENT, result);
        assertEquals(ListenerStatus.RESET, testListener.getLastStatus());
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
    }

    @Test
    public void refresh_cache_should_still_give_old_content_when_update_fails() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(OLD_CONTENT)
                .thenThrow(new IOException())
                .thenReturn(CACHED_CONTENT);

        testListener.resetStatus();
        String result = contentRetriever.getPageContent(PATH);
        assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
        assertEquals(OLD_CONTENT, result);
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));

        testListener.resetStatus();
        contentRetriever.refreshCache();
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.RESET, testListener.getLastStatus());

        testListener.resetStatus();
        result = contentRetriever.getPageContent(PATH);
        assertEquals(OLD_CONTENT, result);
        assertEquals(ListenerStatus.RESET, testListener.getLastStatus());
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
    }

    @Test
    public void refresh_cache_should_update_all_urls_even_if_first_url_fails() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(OLD_CONTENT)
                .thenReturn(CACHED_CONTENT)
                .thenThrow(new IOException())
                .thenReturn(NEW_CONTENT);

        testListener.resetStatus();
        String result = contentRetriever.getPageContent(PATH);
        assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
        verify(httpClient).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(OLD_CONTENT, result);

        testListener.resetStatus();
        result = contentRetriever.getPageContent(PATH2);
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        assertEquals(ListenerStatus.ELEMENT_ADDED, testListener.getLastStatus());
        assertEquals(CACHED_CONTENT, result);

        testListener.resetStatus();
        contentRetriever.refreshCache();
        assertEquals(ListenerStatus.ELEMENT_UPDATED, testListener.getLastStatus());

        result = contentRetriever.getPageContent(PATH);
        assertEquals(OLD_CONTENT, result);
        verify(httpClient, times(4)).execute(any(HttpGet.class), any(BasicResponseHandler.class));

        String result2 = contentRetriever.getPageContent(PATH2);
        assertEquals(NEW_CONTENT, result2);
        verify(httpClient, times(4)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
    }

    @Test
    public void cache_should_be_able_to_refresh_and_retrieve_both_properties_and_strings() throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(BasicResponseHandler.class)))
                .thenReturn(OLD_CONTENT)
                .thenReturn(PROPERTIES_CONTENT)
                .thenThrow(new IOException())
                .thenReturn(PROPERTIES_CONTENT_2);

        testListener.resetStatus();
        String innhold = contentRetriever.getPageContent(PATH);
        Properties nokler = contentRetriever.getProperties(PATH2);
        assertEquals(OLD_CONTENT, innhold);
        assertEquals(PROPERTIES, nokler);
        verify(httpClient, times(2)).execute(any(HttpGet.class), any(BasicResponseHandler.class));

        testListener.resetStatus();
        contentRetriever.refreshCache();

        verify(httpClient, times(4)).execute(any(HttpGet.class), any(BasicResponseHandler.class));
        innhold = contentRetriever.getPageContent(PATH);
        nokler = contentRetriever.getProperties(PATH2);
        assertEquals(OLD_CONTENT, innhold);
        assertEquals(PROPERTIES_2, nokler);

    }
}
