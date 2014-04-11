package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.imageio.ImageIO;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static no.nav.innholdshenter.filter.DecoratorFilter.ALREADY_DECORATED_HEADER;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
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
    public void should_inject_submenu_when_submenu_path_and_fragment_is_defined() throws IOException, ServletException {
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

        assertThat(decoratorFilter.getNoDecoratePatterns().size(), is(1));
        assertThat(decoratorFilter.getNoDecoratePatterns().get(0), is(".*isAlive.*"));

        withNoDecoratePattern("testDecoratePattern");

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

    @Test
    public void should_preserve_image_response() throws IOException, ServletException, URISyntaxException {
        FileInputStream file = new FileInputStream(new File(getClass().getResource("/dashedhorizontal.gif").toURI()));
        BufferedImage image = ImageIO.read(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "gif", outputStream);
        final byte[] bytes = outputStream.toByteArray();

        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getOutputStream().write(bytes);
                servletResponse.setContentType("image/gif;charset=UTF-8");
            }
        };

        decoratorFilter.doFilter(request, response, chain);
        byte[] result = response.getContentAsByteArray();

        assertThat(result, is(bytes));
    }

    @Test
    public void should_handle_empty_response_when_response_should_be_merged_with_fragments() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("");
                servletResponse.setContentType("text/html");
            }
        };
        withFragments("header", "footer");

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is(""));
    }

    @Test
    public void should_replace_title_placeholder_with_value_from_html_title_tag() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><head><title>Bidragsveileder</title></head><body><h1>{{fragment.title}}</h1></html>");
                servletResponse.setContentType("text/html");
            }
        };

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><head><title>Bidragsveileder</title></head><body><h1>Bidragsveileder</h1></html>"));
    }

    @Test
    public void should_handle_no_title_in_application_markup() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><head></head><body><h1>{{fragment.title}}</h1></html>");
                servletResponse.setContentType("text/html");
            }
        };

        decoratorFilter.doFilter(request, response, chain);

        assertThat(response.getContentAsString(), is("<html><head></head><body><h1></h1></html>"));

    }

    @Test
    public void should_remove_submenu_and_expand_grid_when_requestUri_matches_no_submenu_pattern() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body><main id=\"maincontent\"><div class=\"row\"><div class=\"col-md-4\">{{fragment.submenu}}</div><div class=\"col-md-8\"></div></div></main></body></html>");
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
        assertThat(response.getContentAsString(), not(containsString("<div class=\"col-md-4\"></div>")));
        assertThat(response.getContentAsString(), containsString("<div class=\"col-md-12\"></div>"));
    }

    @Test
    public void should_not_throw_exception_when_page_does_not_contain_submenu_placeholder_and_page_should_not_contain_submenu() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html><body>{{fragment.header}}<main id=\"maincontent\"><div class=\"row\"><div class=\"col-md-4\">{{fragment.submenu}}</div><div class=\"col-md-8\"></div></div></main>{{fragment.footer}}</body></html>");
                servletResponse.setContentType("text/html");
            }
        };
        withFragments("header", "footer", "submenu");
        decoratorFilter.setSubMenuPath("path/to/menu");
        decoratorFilter.setNoSubmenuPatterns(asList(".*selftest.*"));
        request.setRequestURI("/internal/selftest");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"header\"></div><div id=\"submenu\"></div><div id=\"footer\"></div>");

        decoratorFilter.doFilter(request, response, chain);
    }

    @Test
    public void response_should_not_contain_a_body_when_status_code_is_304() throws IOException, ServletException, URISyntaxException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                servletResponse.getOutputStream().write("hvaSomHelst".getBytes());
                servletResponse.setContentType("image/gif;charset=UTF-8");
                httpServletResponse.setStatus(304);
            }
        };

        decoratorFilter.doFilter(request, response, chain);

        byte[] result = response.getContentAsByteArray();
        assertThat(result, is("".getBytes()));
    }

    @Test
    public void should_send_active_item_in_menu_map_if_request_uri_matches_menu_map_key() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html></html>");
                servletResponse.setContentType("text/html");
            }
        };
        decoratorFilter.setShouldIncludeActiveItem(true);
        ExtendedConfiguration extendedConfiguration = new ExtendedConfiguration();
        Map<String, String> menuMap = new HashMap<String, String>();
        menuMap.put("^/sbl/kategori.*", "/sbl/ag/sok/enkelt.do");
        extendedConfiguration.setMenuMap(menuMap);
        decoratorFilter.setExtendedConfiguration(extendedConfiguration);
        request.setRequestURI("/sbl/kategorier");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?activeitem=%2Fsbl%2Fag%2Fsok%2Fenkelt.do");
    }

    @Test
    public void should_send_submenupath_based_on_request_uri_and_submenu_map() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html></html>");
                servletResponse.setContentType("text/html");
            }
        };
        withFragments("submenu");
        when(contentRetriever.getPageContent(anyString())).thenReturn("<div id=\"submenu\"><nav id=\"submenu\"></nav></div><<</div>");
        ExtendedConfiguration extendedConfiguration = new ExtendedConfiguration();
        Map<String, String> subMenuPathMap = new HashMap<String, String>();
        subMenuPathMap.put("^/sbl/ag.*", "ditt-nav/din-side");
        subMenuPathMap.put("^/sbl/.*", "ditt-nav/din-side-arbeid");
        extendedConfiguration.setSubMenuPathMap(subMenuPathMap);
        decoratorFilter.setExtendedConfiguration(extendedConfiguration);
        request.setRequestURI("/sbl/ag/minside.do");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?submenu=ditt-nav%2Fdin-side");
    }

    @Test
    public void should_use_hodeFotKey_as_requestUri_if_exists_in_application_markup() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<meta name=\"hodeFotKey\" content=\"/sbl\">");
                servletResponse.setContentType("text/html");
            }
        };
        request.setRequestURI("/meldekort");
        decoratorFilter.setShouldIncludeActiveItem(true);

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?activeitem=%2Fsbl");
    }

    @Test
    public void should_send_tns_value_based_on_request_uri_and_tns_values() throws IOException, ServletException {
        chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                servletResponse.getWriter().write("<html></html>");
                servletResponse.setContentType("text/html");
            }
        };
        ExtendedConfiguration extendedConfiguration = new ExtendedConfiguration();
        Map<String, String> tnsValues = new HashMap<String, String>();
        tnsValues.put("^/sbl/as/minside.*", "AS-DinSide");
        extendedConfiguration.setTnsValues(tnsValues);
        decoratorFilter.setExtendedConfiguration(extendedConfiguration);
        decoratorFilter.setApplicationName("Arbeid");
        request.setRequestURI("/sbl/as/minside.do");

        decoratorFilter.doFilter(request, response, chain);

        verify(contentRetriever).getPageContent("http://nav.no/fragments?appname=AS-DinSide");
    }
}
