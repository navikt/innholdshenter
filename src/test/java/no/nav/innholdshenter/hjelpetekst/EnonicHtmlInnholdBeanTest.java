package no.nav.innholdshenter.hjelpetekst;

import no.nav.innholdshenter.common.ContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tester for {@link EnonicHtmlInnholdBean}
 */
@RunWith(MockitoJUnitRunner.class)
public class EnonicHtmlInnholdBeanTest {

    @Mock
    private ContentRetriever ecRetriever;
    @Mock
    private HtmlInnholdListener listener;

    private EnonicHtmlInnholdBean htmlInnholdBean;

    @Before
    public void setUp() throws Exception {
        htmlInnholdBean = new EnonicHtmlInnholdBean(ecRetriever, "path");
    }

    @Test
    public void henterListeMedHjelpetekster() {
        when(ecRetriever.getPageContent("path")).thenReturn("<innholdsliste><htmlinnhold key=\"key.a\" title=\"Tittel A\"/><htmlinnhold key=\"key.b\" title=\"Tittel B\"/></innholdsliste>");
        List<HtmlInnhold> hjelpetekster = htmlInnholdBean.getHjelpetekster();
        assertThat(hjelpetekster.size(), is(2));
        assertThat(hjelpetekster.get(0).getKey(), is("key.a"));
        assertThat(hjelpetekster.get(0).getTitle(), is("Tittel A"));
        assertThat(hjelpetekster.get(1).getKey(), is("key.b"));
        assertThat(hjelpetekster.get(1).getTitle(), is("Tittel B"));
    }

    @Test
    public void henterEnEnkeltHjelpetekst() {
        when(ecRetriever.getPageContent("path?key=key.a")).thenReturn("<htmlinnhold><title>Tittel</title><html><p><strong>asdfasdf</strong></p></html></htmlinnhold>");
        HtmlInnhold htmlInnhold = htmlInnholdBean.getHjelpetekst("key.a");
        assertThat(htmlInnhold.getTitle(), is("Tittel"));
        assertThat(htmlInnhold.getHtml(), is("<p><strong>asdfasdf</strong></p>"));
    }

    @Test
    public void kallerHjelpetekstListener() {
        HtmlInnhold htmlInnhold = new HtmlInnhold();
        when(ecRetriever.getPageContent("path?key=key.a")).thenReturn("<htmlinnhold><title>Tittel</title><html><p><strong>asdfasdf</strong></p></html></htmlinnhold>");
        htmlInnholdBean.setHtmlInnholdListeners(asList(listener));
        when(listener.onHtmlInnholdRetrieved(any(HtmlInnhold.class))).thenReturn(htmlInnhold);

        HtmlInnhold res = htmlInnholdBean.getHjelpetekst("key.a");

        verify(listener).onHtmlInnholdRetrieved(any(HtmlInnhold.class));
        assertEquals(res, htmlInnhold);
    }
}
