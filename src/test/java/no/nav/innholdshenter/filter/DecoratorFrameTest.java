package no.nav.innholdshenter.filter;

import junit.framework.TestCase;
import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


/**
 * Testklasse for DecoratorFrame.
 */
@RunWith(MockitoJUnitRunner.class)
public class DecoratorFrameTest extends TestCase {

    @Mock
    private EnonicContentRetriever contentRetriever;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession httpSession;

    private DecoratorFrame decoratorFrame;

    private static final String TEMPLATE_URL = "http://www.nav.no/systemsider/Meny";
    private static final String HTTP_REQUEST_URL = "http://www.nav.no/sbl/ag/minside.do";
    private static final String HTTPS_REQUEST_URL = "https://www.nav.no/sbl/ag/minside.do";
    private static final String REQUEST_URL_PATH = "/sbl/ag/minside.do";

    @Before
    public void setUp() {
        when(request.getSession(eq(true))).thenReturn(httpSession);
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTP_REQUEST_URL));

        when(request.getPathTranslated()).thenReturn(REQUEST_URL_PATH);
        when(request.getRequestURI()).thenReturn(REQUEST_URL_PATH);

        decoratorFrame = new DecoratorFrame();
        decoratorFrame.setContentRetriever(contentRetriever);
        decoratorFrame.setTemplateUrl(TEMPLATE_URL);
    }


    private String generateCorrectUrlString(String template)
    {
        return template + "?urlPath="+ REQUEST_URL_PATH;
    }

    @Test
    public void shouldReturnHtmlFrame() {
        String page = "<html><head><base href = \"http://www.nav.no/\" /><title>ti</title>he</head><body id=\"2\">" +
              "<a href=\"/sbl/arbeid/innlogging\">Logg inn</a><a href=\"http://www.nav.no/Forsiden\">Forsiden</a>" +
              "<a href=\"http://www.google.com/analytics/\">Google Analytics</a></body></html>";
        when(request.getRequestURL()).thenReturn(new StringBuffer(HTTPS_REQUEST_URL));
        when(contentRetriever.getPageContent(generateCorrectUrlString(TEMPLATE_URL))).thenReturn(page);

        HtmlPage fetchedPage = decoratorFrame.getHtmlFrame(request, null, null);

        assertEquals(page, fetchedPage.getHtml());
    }

}
