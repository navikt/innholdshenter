package no.nav.innholdshenter.message;

import no.nav.innholdshenter.common.EnonicContentRetriever;

import org.apache.commons.collections.map.FixedSizeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;


/**
 * Henter properties fra EnonicContentRetriever og returnerer en enkelt
 * property.
 */
public class EnonicMessageBean extends FixedSizeMap {

    private static final long serialVersionUID = -7473169095493321572L;

    private static final Logger logger = LoggerFactory.getLogger(EnonicMessageBean.class);
    private static final String FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY = "Feil ved henting av property med key '{}': {}";
    private static final String MISSING_KEY_TEMPLATE = "<b>[%s]</b>";

    private String propertiesPath;
    private EnonicContentRetriever enonicContentRetriever;

    private boolean debugMode = false;
    private boolean disregardUnknownPropertyKeys = false;
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>();

    public EnonicMessageBean(EnonicContentRetriever vsRetriever, String propertiesPath) {
        super(new HashMap<Object, String>());
        this.enonicContentRetriever = vsRetriever;
        this.propertiesPath = propertiesPath;
    }

    @Override
    public String get(Object key) {
        String propertyValue = null;
        boolean errorTriggered = false;
        try {
            Properties properties = enonicContentRetriever.getProperties(propertiesPath);
            propertyValue = properties.getProperty(key.toString().trim());
        } catch (IllegalStateException e) {
            logger.error(FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY, key, e.getMessage());
            errorTriggered = true;
        }
        if (propertyValue != null) {
            for (MessageListener messageListener : messageListeners) {
                propertyValue = messageListener.onMessageRetrieved(propertyValue);
            }
        } else {
            if (debugMode) {
                propertyValue = String.format(MISSING_KEY_TEMPLATE, key);
            } else {
                if (!errorTriggered && !disregardUnknownPropertyKeys) {
                    logger.error(FEILMELDING_FEIL_VED_HENTING_AV_PROPERTY_MED_KEY, key, "");
                }
                propertyValue = "";
            }
        }
        return propertyValue;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setDisregardUnknownPropertyKeys(boolean disregardUnknownPropertyKeys) {
        this.disregardUnknownPropertyKeys = disregardUnknownPropertyKeys;

    }

    public void setMessageListeners(List<MessageListener> messageListeners) {
        this.messageListeners = messageListeners;
    }
}
