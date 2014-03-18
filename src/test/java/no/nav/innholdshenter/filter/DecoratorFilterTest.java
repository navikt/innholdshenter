package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static java.util.Arrays.asList;
import static no.nav.innholdshenter.filter.DecoratorFilter.ALREADY_DECORATED_HEADER;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DecoratorFilterTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;
    private EnonicContentRetriever contentRetriever;

    private DecoratorFilter decoratorFilter;

    @Before
    public void setUp() throws IOException {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        contentRetriever = mock(EnonicContentRetriever.class);
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"header\"><nav></nav></div><div id=\"footer\"><footer></footer></div>");

        decoratorFilter = new DecoratorFilter();
        decoratorFilter.setContentRetriever(contentRetriever);
        decoratorFilter.setFragmentsUrl("http://nav.no/fragments");
    }

    private void withFragments(String... fragments) {
        decoratorFilter.setFragmentNames(asList(fragments));
    }

    private void withDefaultFilterChain() {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.header}}{{fragment.footer}}</body></html>");
                servletResponse.setContentType("text/html");
            }
        };
    }

    private void withNoDecoratePattern(String noDecoratePattern) {
        ArrayList<String> noDecoratePatterns = new ArrayList<String>();
        noDecoratePatterns.add(noDecoratePattern);
        decoratorFilter.setNoDecoratePatterns(noDecoratePatterns);
    }

    @Test
    public void should_replace_elements_in_fragments_list() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><body><nav></nav><footer></footer></body></html>"));
    }

    @Test
    public void should_build_url_based_on_fragment_list() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?header=true&footer=true");
    }

    @Test
    public void should_build_url_with_application_name() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");
        decoratorFilter.setApplicationName("bidragsveileder");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?appname=bidragsveileder&header=true&footer=true");
    }

    @Test
    public void should_not_inject_fragments_when_response_is_invalid_content_type() throws IOException, ServletException {
        final String expected = "<html><body>{{fragment.header}}{{fragment.footer}}</body></html>";
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write(expected);
                servletResponse.setContentType(null);
            }
        };

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is(expected));
    }

    @Test
    public void should_inject_sub_menu_when_submenu_path_and_fragment_is_defined() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.submenu}}</body></html>");
                servletResponse.setContentType("text/html");
            }
        };
        withFragments("submenu");
        decoratorFilter.setSubMenuPath("/ditt-nav/din-side-arbeid");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"submenu\"><nav id=\"submenu\"></nav></div><<</div>");

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><body><nav id=\"submenu\"></nav></body></html>"));
    }

    @Test
    public void should_build_url_with_activeitem_if_include_active_item_is_set() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");
        decoratorFilter.setShouldIncludeActiveItem(true);
        request.setRequestURI("/minside");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?activeitem=%2Fminside&header=true&footer=true");
    }

    @Test
    public void should_build_url_with_userrole_if_meta_tag_brukerstatus_exists() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><head><meta name=\"Brukerstatus\" content=\"ARBS\"></head><body></body></html>");
                servletResponse.setContentType("text/html");
            }
        };

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?userrole=ARBS");
    }

    @Test
    public void should_not_decorate_request_and_remove_placeholder_when_requestUri_matches_no_decorate_pattern() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");
        withNoDecoratePattern(".*selftest.*");
        request.setRequestURI("/internal/selftest");

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><body></body></html>"));
    }

    @Test
    public void should_not_inject_submenu_and_remove_placholder_when_requestUri_matches_no_submenu_pattern() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.submenu}}</body></html>");
                servletResponse.setContentType("text/html");
            }
        };

        withFragments("submenu");
        decoratorFilter.setSubMenuPath("path/to/menu");
        decoratorFilter.setNoSubmenuPatterns(asList(".*selftest.*"));
        request.setRequestURI("/internal/selftest");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"submenu\"></div>");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?submenu=path%2Fto%2Fmenu");
        assertThat(response.getContentAsString(), is("<html><body></body></html>"));
    }

    @Test
    public void should_not_decorate_response_when_request_has_exclude_header() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever, times(0)).getPageContent(anyString());
    }

    @Test
    public void should_not_decorate_response_when_request_is_already_decorated() throws IOException, ServletException {
        withDefaultFilterChain();
        withFragments("header", "footer");
        request.setAttribute(ALREADY_DECORATED_HEADER, Boolean.TRUE);

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever, times(0)).getPageContent(anyString());
    }

    @Test
    public void should_inject_static_resource_fragment() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><head>{{fragment.resources-head}}</head><body></body></html>");
                servletResponse.setContentType("text/html");
            }
        };
        withFragments("resources-head");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"resources-head\"><link href=\"main.css\" /></div>");

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><head><link href=\"main.css\" /></head><body></body></html>"));
    }

    @Test
    public void should_have_default_noDecoratePattern() {
        withDefaultFilterChain();

        withNoDecoratePattern("test");

        assertThat(decoratorFilter.getNoDecoratePatterns().size(), is(2));
        assertThat(decoratorFilter.getNoDecoratePatterns().get(1), is(".*isAlive.*"));
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_exception_when_expected_fragment_is_not_found_in_response_from_enonic() throws Exception {
        withDefaultFilterChain();
        withFragments("header", "footer");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"header\"></div>");

        decoratorFilter.doFilter(request, response, chain);
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_exception_when_placeholder_is_not_found_in_application_markup() throws Exception {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.header}}</body></html>");
                servletResponse.setContentType("text/html");

            }
        };
        withFragments("header", "footer");

        decoratorFilter.doFilter(request, response, chain);
    }

    @Test(expected = RuntimeException.class)
    public void should_throw_exception_when_application_markup_has_unresolved_placeholders() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.header}}{{fragment.footer}}{{fragment.ubrukt_placeholder}}</body></html>");
                servletResponse.setContentType("text/html");

            }
        };
        withFragments("header", "footer");

        decoratorFilter.doFilter(request, response, chain);
    }

}
