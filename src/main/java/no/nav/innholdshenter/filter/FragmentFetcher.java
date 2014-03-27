package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.List;

import static no.nav.innholdshenter.filter.DecoratorFilterUtils.isFragmentSubmenu;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class FragmentFetcher {

    private final static Logger logger = LoggerFactory.getLogger(FragmentFetcher.class);

    private EnonicContentRetriever contentRetriever;
    private String fragmentsUrl;
    private String applicationName;
    private boolean shouldIncludeActiveItem;
    private List<String> fragmentNames;
    private String subMenuPath;
    private final HttpServletRequest request;
    private final String originalResponseString;

    public FragmentFetcher(EnonicContentRetriever contentRetriever, String fragmentsUrl, String applicationName,
                           boolean shouldIncludeActiveItem, String subMenuPath, List<String> fragmentNames, HttpServletRequest request, String originalResponseString) {
        this.contentRetriever = contentRetriever;
        this.fragmentsUrl = fragmentsUrl;
        this.applicationName = applicationName;
        this.shouldIncludeActiveItem = shouldIncludeActiveItem;
        this.fragmentNames = fragmentNames;
        this.subMenuPath = subMenuPath;
        this.request = request;
        this.originalResponseString = originalResponseString;
    }

    public Document fetchHtmlFragments() {
        String url = null;
        try {
            url = buildUrl();
        } catch (URISyntaxException e) {
            logger.warn("Exception when building URL", e);
        }

        String pageContent = contentRetriever.getPageContent(url);
        return Jsoup.parse(pageContent);
    }

    private String buildUrl() throws URISyntaxException {
        URIBuilder urlBuilder = new URIBuilder(fragmentsUrl);

        if (applicationName != null) {
            urlBuilder.addParameter("appname", applicationName);
        }

        if (shouldIncludeActiveItem) {
            urlBuilder.addParameter("activeitem", request.getRequestURI());
        }

        String role = extractMetaTag("Brukerstatus");
        if (!isEmpty(role)) {
            urlBuilder.addParameter("userrole", role);
        }

        for (String fragmentName : fragmentNames) {
            if (isFragmentSubmenu(fragmentName)) {
                urlBuilder.addParameter("submenu", subMenuPath);
            } else {
                urlBuilder.addParameter(fragmentName, "true");
            }
        }

        return urlBuilder.build().toString();
    }

    private String extractMetaTag(String tag) {
        Document doc = Jsoup.parse(originalResponseString);
        Elements brukerStatus = doc.select(String.format("meta[name=%s]", tag));

        if (!brukerStatus.isEmpty()) {
            return brukerStatus.attr("content");
        } else {
            return null;
        }
    }
}
