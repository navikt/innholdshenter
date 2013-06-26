package no.nav.innholdshenter.filter;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Tester CachedPageStore
 */
public class HtmlPageTest {

    private final String titleText = "This is a title";
    private final String titleTag = "<title>" + titleText + "</TITLE>";
    private final String headText = "<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">\n<script language=\"javascript\">\nvar contextPath = '';\n</script>\n<link rel=\"stylesheet\" href=\"/styles/main-action.css?spaceKey=navnofor\" type=\"text/css\" />\n<link rel=\"shortcut icon\" href=\"/images/icons/favicon.ico\">";
    private final String headTag = "<HEAD>" + titleTag + headText + "</head >";
    private final String bodyText = "<div>\n<script language=\"JavaScript\">\nfunction showBreadcrumbsEllipsis()\n{\ndocument.getElementById('breadcrumbsEllipsis').style.display = 'none';\ndocument.getElementById('breadcrumbsExpansion').style.display = 'inline';\n}\n</script></div>";
    private final String bodyTag = "<boDy onLoad=\"placeFocus()\">" + bodyText + "</bOdy >";
    private final String hodeFotKey = "/sbl/ag/";
    private final String metaTag = "<meta name=\"hodeFotKey\" content=\""+hodeFotKey+"\" />  \n <div class=\"venstreKolonne\">&nbsp;</div>";
    private final String html = "<hTmL>" + headTag + bodyTag + "</html>";

    @Test
    public void shouldGetTitle() {
        HtmlPage sut = new HtmlPage(html);
        assertEquals("Title ble ikke riktig", titleText, sut.getTitlePartOfHtml());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnNullInput() {
        HtmlPage sut = new HtmlPage(null);
        sut.getTitlePartOfHtml();
    }

    @Test
    public void shouldFindeMetaTag() {
        String html = "<html>"+ headTag + metaTag +  bodyTag +"</html>";
        HtmlPage sut = new HtmlPage(html);
        assertEquals(hodeFotKey, sut.extractMetaTagInformation("hodeFotKey"));
    }

    @Test
    public void shouldThrowExceptionOnNonExistingTitle() {
        String tmpHtml = "<html><head></head><body><body/></html>";
        HtmlPage sut = new HtmlPage(tmpHtml);
        assertEquals("Should return empty string for head when there is no head in html source", "", sut.getTitlePartOfHtml());
    }

    @Test
    public void shouldGetHead() {
        HtmlPage sut = new HtmlPage(html);
        assertEquals("Head ble ikke riktig", headText, sut.getHeadPartOfHtml());
    }

    @Test
    public void shouldGetBody() {
        HtmlPage sut = new HtmlPage(html);
        assertEquals("Body ble ikke riktig", bodyText, sut.getBodyPartOfHtml());
    }

    @Test
    public void shouldReturnOnlyAreaWithinTags() {
        String htmlString = "<div id=\"NAVdarkGradientContainer\">\n" +
                "\n" +
                "\n" +
                "    <div class=\"NAVtjenesteLoginContainer\" id=\"NAVtjenesteLoginContainer\">\n" +
                "\n" +
                "\n" +
                "        <form name=\"aetatLogout_Form\" method=\"POST\" action=\"http://localhost:7141/sbl/logout.do\" autocomplete=\"off\">\n" +
                "\n" +
                "            <div>\n" +
                "\n" +
                "                Bruker: <strong>vaskeglad</strong>\n" +
                "                <input type=\"submit\" name=\"actionEvent.security_logout\" value=\"Logg ut\" class=\"NAVbtn\"/>\n" +
                "            </div>\n" +
                "\n" +
                "        </form>\n" +
                "\n" +
                "\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "    <!--START--><div class=\"tittelheader\"><!--END--><!-- [page.title.minSide] -->\n" +
                "        Din side\n" +
                "    </div>\n" +
                "\n" +
                "\n" +
                "</div>";
        HtmlPage sut = new HtmlPage(htmlString);
        String result = sut.getAreaWithinTagsFromHtml("<!--START-->", "<!--END-->");
        assertEquals("<!--START--><div class=\"tittelheader\"><!--END-->", result);
    }
}
