package no.nav.innholdshenter.message;

import no.nav.innholdshenter.common.ContentRetriever;
import org.apache.commons.collections.map.FixedSizeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;


/**
 * Henter properties fra EnonicContentRetriever og returnerer en enkelt
 * property.
 */
public class EnonicStringRetriever extends FixedSizeMap implements StringRetriever {

    private static final Logger logger = LoggerFactory.getLogger(EnonicStringRetriever.class);
    private static final String FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY = "Feil ved henting av property med key '{}', locale '{}', variant '{}': {}";
    private static final String MISSING_KEY_TEMPLATE = "<b>[%s locale:%s, variant:%s]</b>";

    private String propertiesPath;
    private ContentRetriever contentRetriever;

    public EnonicStringRetriever(ContentRetriever vsRetriever, String propertiesPath) {
        super(new HashMap<Object, String>());
        this.contentRetriever = vsRetriever;
        this.propertiesPath = propertiesPath + "?locale=";
    }

    @Override
    public String get(Object key) {
        if (key != null) {
            return retrieveString(key.toString());
        }
        return "";
    }

    public String retrieveString(String key) {
        return retrieveString(key, null, null);
    }

    public String retrieveString(String key, String locale) {
        return retrieveString(key, locale, null);
    }

    public String retrieveString(String key, String locale, String variant) {
        try {
            String path = propertiesPath;
            if (variant == null) {
                path += emptyStringIfNull(locale) + "&variant=";
            } else {
                path += getPropertiesPath(locale, variant);
            }
            Properties properties = contentRetriever.getProperties(path);
            String value = properties.getProperty(key.trim());
            return (value != null) ? value : String.format(MISSING_KEY_TEMPLATE, key, locale, variant);
        } catch (RuntimeException e) {
            logger.error(FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY, key, locale, variant, e.getMessage());
            return String.format(MISSING_KEY_TEMPLATE, key, locale, variant);
        }
    }

    private String getPropertiesPath(String locale, String variant) {
        return String.format("%s&variant=%s", emptyStringIfNull(locale), emptyStringIfNull(variant));
    }

    private String emptyStringIfNull(String string) {
        return string == null ? "" : string;
    }
}
