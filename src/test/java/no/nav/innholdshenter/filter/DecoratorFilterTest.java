package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tester ByteArrayServletOutputStream
 */
@RunWith(MockitoJUnitRunner.class)
public class DecoratorFilterTest {

    private DecoratorFilter decoratorFilter;
    private String templateurl = "/systemsider/templates/decorator";
    private ByteArrayServletOutputStream stream = new ByteArrayServletOutputStream("UTF-8");

    @Mock
    private DecoratorResponseWrapper mockResponseWrapper;
    @Mock
    private HttpServletResponse httpServletResponse;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private DecoratorFrame decoratorFrame;
    @Mock
    private EnonicContentRetriever contentRetriever;


    @Before
    public void setUp() throws IOException {
        when(mockResponseWrapper.getOutputAsString()).thenReturn("");
        when(mockResponseWrapper.getResponse()).thenReturn(httpServletResponse);
        PrintWriter out = new PrintWriter(stream);
        when(httpServletResponse.getWriter()).thenReturn(out);
        when(httpServletResponse.getOutputStream()).thenReturn(stream);
        decoratorFilter = new DecoratorFilter(contentRetriever, templateurl);
        decoratorFilter.setDecoratorFrame(decoratorFrame);
    }

    @Test
    public void shouldSetupFrameCorrectly() {
        assertTrue(decoratorFrame == decoratorFilter.getDecoratorFrame());
        verify(decoratorFrame).setContentRetriever(contentRetriever);
        verify(decoratorFrame).setTemplateUrl(templateurl);
    }

    @Test
    public void shouldReplacePlaceholders() throws IOException {
        String page = "<html><head><title>ti</title>he</head><body id=\"2\">bo</body></html>";
        String applicationFrame = "<html><head><title><!-- ${title} --></title><!-- ${head} --></head><body id=\"1\"><!-- ${body} --></body></html>";
        when(mockResponseWrapper.getOutputAsString()).thenReturn(page);

        HtmlPage newPage = decoratorFilter.decorateResponseContent(applicationFrame, new HtmlPage(page));
        decoratorFilter.writeTransformedResponse(mockResponseWrapper, newPage);

        assertEquals("<html><head><title>ti</title>he</head><body id=\"1\">bo</body></html>", stream.toString());
    }

    @Test
    public void shouldHandleTextHTML() throws IOException, ServletException {
        FilterChain chain = new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html");
            }
        };

        HttpSession session = mock(HttpSession.class);

        // setup

        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.nav.no/context/somepath"));
        when(httpServletRequest.getContextPath()).thenReturn("/context");
        when(httpServletRequest.getSession(true)).thenReturn(session);
        when(decoratorFrame.getHtmlFrame(httpServletRequest, null, null)).thenReturn(new HtmlPage("<html><body>generated page frame</body></html>"));
        decoratorFilter.doFilter(httpServletRequest, httpServletResponse, chain);

        verify(decoratorFrame).getHtmlFrame(httpServletRequest, null, null);
    }


    @Test
    public void shouldHandleHtmlByDefault() {
        assertTrue("Should handle html", decoratorFilter.canHandleContentType("text/html"));
    }

    @Test
    public void shouldHandleXHtmlByDefault() {
        assertTrue("Should handle xhtml", decoratorFilter.canHandleContentType("application/xhtml+xml"));
    }

    @Test
    public void unknownContentTypeShouldPassThrough() throws IOException, ServletException {
        FilterChain chain = new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.setContentType("text/css");
            }
        };

        HttpSession session = mock(HttpSession.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);

        // setup
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.nav.no/context/somepath"));
        when(httpServletRequest.getContextPath()).thenReturn("/context");
        when(httpServletRequest.getSession(true)).thenReturn(session);
        when(httpServletResponse.getOutputStream()).thenReturn(outputStream);

        decoratorFilter.doFilter(httpServletRequest, httpServletResponse, chain);

        verify(decoratorFrame, never()).getHtmlFrame(httpServletRequest, null, null);
    }


}
