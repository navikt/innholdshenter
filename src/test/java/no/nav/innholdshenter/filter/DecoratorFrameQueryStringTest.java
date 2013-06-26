package no.nav.innholdshenter.filter;

import junit.framework.TestCase;
import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Testklasse for DecoratorFrame.
 */
@RunWith(MockitoJUnitRunner.class)
public class DecoratorFrameQueryStringTest extends TestCase {

    @Mock
    private EnonicContentRetriever contentRetriever;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession httpSession;

    private DecoratorFrame decoratorFrame;

    private static final String TEMPLATE_URL = "http://www.nav.no/systemsider/Meny";
    private static final String DEFAULT_BASE_URL = "http://www.nav.no/";
    private static final String REQUEST_URL_PATH_NO_QS = "/sbl/ag/minside.do";
    private static final String REQUEST_URL = "http://www.nav.no/sbl/ag/minside.do?qs=true";
    private static final String REQUEST_QS = "qs=true";
    private static final String REQUEST_URL_WITH_ROLE = "http://www.nav.no/sbl/ag/minside.do?role=ARBS";
    private static final String REQUEST_ROLE = "role=ARBS";

    private static String page = "<html><head></body></html>";


    @Before
    public void setUp() {
        when(request.getSession(eq(true))).thenReturn(httpSession);
        when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));

        when(request.getPathTranslated()).thenReturn(REQUEST_URL_PATH_NO_QS);
        when(request.getRequestURI()).thenReturn(REQUEST_URL_PATH_NO_QS);
        when(request.getQueryString()).thenReturn(REQUEST_QS);

        decoratorFrame = new DecoratorFrame();
        decoratorFrame.setContentRetriever(contentRetriever);
        decoratorFrame.setTemplateUrl(TEMPLATE_URL);
    }

    private String generateUrlWithQueryString() {
        return TEMPLATE_URL + "?urlPath=" + REQUEST_URL_PATH_NO_QS + "," + REQUEST_QS;
    }

    private String generateUrlWithoutQueryString() {
        return TEMPLATE_URL + "?urlPath=" + REQUEST_URL_PATH_NO_QS;
    }

    private String generateUrlWithRoleQueryString() {
        return TEMPLATE_URL + "?urlPath=" + REQUEST_URL_PATH_NO_QS + "&" +REQUEST_ROLE;
    }

    @Test
    public void shouldSendQueryStringWithRole() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);
        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, "ARBS");
        verify(contentRetriever).getPageContent(generateUrlWithRoleQueryString());
    }

    @Test
    public void shouldSendQueryStringWhenFlagForItSetToTrue() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }

    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToFalse() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }

    @Test
    public void shouldSendQueryStringWhenFlagForItSetToFalseButTheBaseUrlIsInTheWhitelist() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        List<String> whitelist = new ArrayList<String>();
        whitelist.add("/sbl/ag");
        decoratorFrame.setIncludeQueryStringInDecorationPatterns(whitelist);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }

    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToTrueButTheBaseUrlIsInTheBlacklist() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        List<String> blacklist = new ArrayList<String>();
        blacklist.add("/sbl/ag");

        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(blacklist);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }


    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToFalseAndTheBaseUrlIsNotInTheWhitelist() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        List<String> whitelist = new ArrayList<String>();
        whitelist.add("/sbl/notmatching/ag");
        decoratorFrame.setIncludeQueryStringInDecorationPatterns(whitelist);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }

    @Test
    public void shouldSendQueryStringWhenFlagForItSetToTrueAndTheBaseUrlIsNotInTheBlacklist() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        List<String> blacklist = new ArrayList<String>();
        blacklist.add("/sbl/notmatching/ag");

        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(blacklist);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }


    @Test
    public void shouldSendQueryStringWhenFlagForItSetToFalseButTheQueryStringIsInTheWhitelist() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        List<String> whitelist = new ArrayList<String>();
        whitelist.add("\\?.*qs");
        decoratorFrame.setIncludeQueryStringInDecorationPatterns(whitelist);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }

    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToTrueButTheQueryStringIsInTheBlacklist() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        List<String> blacklist = new ArrayList<String>();
        blacklist.add("\\?.*qs");

        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(blacklist);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }

    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToFalseAndTheQueryStringIsNotInTheWhitelist() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        List<String> whitelist = new ArrayList<String>();
        whitelist.add("");
        whitelist.add("\\?.*notmatching");
        whitelist.add("");
        decoratorFrame.setIncludeQueryStringInDecorationPatterns(whitelist);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }

    @Test
    public void shouldSendQueryStringWhenFlagForItSetToTrueAndTheQueryStringIsNotInTheBlacklist() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        List<String> blacklist = new ArrayList<String>();
        blacklist.add("");
        blacklist.add("\\?.*notmatching");
        blacklist.add("");

        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(blacklist);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }

    @Test
    public void shouldNotSendQueryStringWhenFlagForItSetToFalseAndTheWhitelistHasEmptyValuesOnly() {
        decoratorFrame.setIncludeQueryStringInDecoration(false);

        List<String> whitelist = new ArrayList<String>();
        whitelist.add("");
        whitelist.add("");
        whitelist.add("");
        decoratorFrame.setIncludeQueryStringInDecorationPatterns(whitelist);

        when(contentRetriever.getPageContent(generateUrlWithoutQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithoutQueryString());
    }


    @Test
    public void shouldSendQueryStringWhenFlagForItSetToTrueAndTheBlacklistHasEmptyValuesOnly() {
        decoratorFrame.setIncludeQueryStringInDecoration(true);

        List<String> blacklist = new ArrayList<String>();
        blacklist.add("");
        blacklist.add("");
        blacklist.add("");

        decoratorFrame.setExcludeQueryStringFromDecorationPatterns(blacklist);

        when(contentRetriever.getPageContent(generateUrlWithQueryString())).thenReturn(page);
        decoratorFrame.getHtmlFrame(request, null, null);
        verify(contentRetriever).getPageContent(generateUrlWithQueryString());
    }


}