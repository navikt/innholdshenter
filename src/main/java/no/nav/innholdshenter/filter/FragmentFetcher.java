package no.nav.innholdshenter.filter;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static no.nav.innholdshenter.filter.DecoratorFilterUtils.createMatcher;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.extractMetaTag;
import static no.nav.innholdshenter.filter.DecoratorFilterUtils.getRequestUriOrAlternativePathBasedOnMetaTag;
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
    private ExtendedConfiguration extendedConfiguration;

    public FragmentFetcher(EnonicContentRetriever contentRetriever, String fragmentsUrl, String applicationName, boolean shouldIncludeActiveItem,
                           String subMenuPath, List<String> fragmentNames, HttpServletRequest request,
                           String originalResponseString, ExtendedConfiguration extendedConfiguration) {
        this.contentRetriever = contentRetriever;
        this.fragmentsUrl = fragmentsUrl;
        this.applicationName = applicationName;
        this.shouldIncludeActiveItem = shouldIncludeActiveItem;
        this.fragmentNames = fragmentNames;
        this.subMenuPath = subMenuPath;
        this.request = request;
        this.originalResponseString = originalResponseString;
        this.extendedConfiguration = extendedConfiguration;
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
            addApplicationName(urlBuilder);
        }

        if (shouldIncludeActiveItem) {
            addActiveItem(urlBuilder);
        }

        String role = extractMetaTag(originalResponseString, "Brukerstatus");
        if (!isEmpty(role)) {
            urlBuilder.addParameter("userrole", role);
        }

        for (String fragmentName : fragmentNames) {
            if (isFragmentSubmenu(fragmentName)) {
                addSubmenuPath(urlBuilder);
            } else {
                urlBuilder.addParameter(fragmentName, "true");
            }
        }

        return urlBuilder.build().toString();
    }

    private void addApplicationName(URIBuilder urlBuilder) {
        String requestUri = request.getRequestURI();
        if (extendedConfiguration != null) {
            Map<String, String> tnsValues = extendedConfiguration.getTnsValues();
            for (String key : tnsValues.keySet()) {
                Matcher matcher = createMatcher(key, requestUri);
                if (matcher.matches()) {
                    urlBuilder.addParameter("appname", tnsValues.get(key));
                    return;
                }
            }
        }

        urlBuilder.addParameter("appname", applicationName);
    }

    private void addActiveItem(URIBuilder urlBuilder) {
        String requestUri = getRequestUriOrAlternativePathBasedOnMetaTag(originalResponseString, request);
        if (extendedConfiguration != null) {
            Map<String, String> menuMap = extendedConfiguration.getMenuMap();
            for (String key : menuMap.keySet()) {
                Matcher matcher = createMatcher(key, requestUri);
                if (matcher.matches()) {
                    urlBuilder.addParameter("activeitem", menuMap.get(key));
                    return;
                }
            }
        }

        urlBuilder.addParameter("activeitem", requestUri);
    }

    private void addSubmenuPath(URIBuilder urlBuilder) {
        String requestUri = getRequestUriOrAlternativePathBasedOnMetaTag(originalResponseString, request);
        if (extendedConfiguration != null) {
            Map<String, String> subMenuPathMap = extendedConfiguration.getSubMenuPathMap();
            for (String key : subMenuPathMap.keySet()) {
                Matcher matcher = createMatcher(key, requestUri);
                if (matcher.matches()) {
                    urlBuilder.addParameter("submenu", subMenuPathMap.get(key));
                    return;
                }
            }
        }

        urlBuilder.addParameter("submenu", subMenuPath);
    }
}
