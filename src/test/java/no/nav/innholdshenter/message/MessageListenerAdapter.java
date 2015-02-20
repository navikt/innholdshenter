package no.nav.innholdshenter.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for StringRetriever that calls message listeners when new strings are fetched
 */
public class MessageListenerAdapter implements StringRetriever {
    private List<MessageListener> messageListeners = new ArrayList<MessageListener>();
    private StringRetriever stringRetriever;

    public MessageListenerAdapter(StringRetriever stringRetriever) {
        this.stringRetriever = stringRetriever;
    }

    public String retrieveString(String key, String locale) {
        return retrieveString(key, locale, null);
    }

    public String retrieveString(String key, String locale, String variant) {
        String value = stringRetriever.retrieveString(key, locale, variant);
        if (value != null) {
            for (MessageListener messageListener : messageListeners) {
                value = messageListener.onMessageRetrieved(value);
            }
        }
        return value;
    }

    public void setMessageListeners(List<MessageListener> messageListeners) {
        this.messageListeners = messageListeners;
    }
}

