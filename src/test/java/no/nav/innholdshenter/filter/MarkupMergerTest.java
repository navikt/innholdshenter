package no.nav.innholdshenter.filter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

/**
 * Testklasse for MarkupMerger.
 */
@RunWith(MockitoJUnitRunner.class)
public class MarkupMergerTest {
    String encodingTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    String body = "<body>this is the body</body>";
    String head = "<head>this is the head</head>";
    String title = "<title>Title</title>";

    @Test
    public void shouldInsertHeadAndBody() {
        String frame = "<html><head><title>$1</title> $2</head> <body>$3</body><html>";

        String inputFrame = String.format(frame, "<!-- ${title} -->", "<!-- ${head} -->", "<!-- ${body} -->");
        String outputFrame = String.format(frame, title, head, body);

        String html = head + body + title;
        HtmlPage page = new HtmlPage(html);
        page = MarkupMerger.mergeMarkup(inputFrame, page);
        assertEquals("Content was not merged", outputFrame, page.getHtml());
    }

    @Test
    public void testMergeHeaderBarComponent() {
        String frame = encodingTag + "<html><head><title>%s</title> %s %s</head><body><!-- headerbar_start -->Ukjent!<!-- headerbar_end --> %s</body><html>";

        String inputFrame = String.format(frame, "<!-- ${title} -->", "<!-- ${head} -->", "<!-- ${headerbarcomponent} -->", "<!-- ${body} -->");
        String html = head + body + title;
        HtmlPage htmlpage = new HtmlPage(html);
        HtmlPage mergepage = MarkupMerger.mergeMarkup(inputFrame, htmlpage);
        mergepage = MarkupMerger.mergeHeaderBarComponent(mergepage, "<!-- headerbar_start -->", "<!-- headerbar_end -->");
        System.out.println(mergepage.getHtml());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head><title>Title</title> this is the head Ukjent!</head><body> this is the body</body><html>", mergepage.getHtml());
    }

    @Test
    public void testEncodingTagExtractionFail() {
        String frame = "<?xml version=\"1.0\" ?><html>" + head + body + "</html>";
        String encodingtag = MarkupMerger.getEncodingTag(frame);
        assertEquals("", encodingtag);
    }

    @Test
    public void testEncodingTagExtraction() {
        String frame = encodingTag + "<html>" + head + body + "</html>";
        String encodingtag = MarkupMerger.getEncodingTag(frame);
        assertEquals(encodingTag, encodingtag);
    }

}