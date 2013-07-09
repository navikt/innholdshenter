package no.nav.innholdshenter.message;

import no.nav.innholdshenter.common.EnonicContentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


/**
 * Henter properties fra EnonicContentRetriever og returnerer en enkelt
 * property.
 */
public class EnonicStringRetriever implements StringRetriever {

    private static final Logger logger = LoggerFactory.getLogger(EnonicStringRetriever.class);
    private static final String FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY = "Feil ved henting av property med key '%s', locale '%s', variant '%s': %s";

    private String propertiesPath;
    private EnonicContentRetriever enonicContentRetriever;

    public EnonicStringRetriever(EnonicContentRetriever vsRetriever, String propertiesPath) {
        this.enonicContentRetriever = vsRetriever;
        this.propertiesPath = propertiesPath;
    }

    public String retrieveString(String key, String locale, String variant) {
        try {
            String path = getPropertiesPath(locale, variant);
            Properties properties = enonicContentRetriever.getProperties(path);
            return properties.getProperty(key.trim());
        } catch (IllegalStateException e) {
            logger.error(FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY, key, locale, variant, e.getMessage());
            return null;
        }
    }

    private String getPropertiesPath(String locale, String variant) {
        return String.format("%s?locale=%s&variant=%s", propertiesPath, getString(locale), getString(variant));
    }

    private String getString(String locale) {
        return locale == null ? "" : locale;
    }
}
