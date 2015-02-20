package no.nav.innholdshenter.message;

/**
 * Listener API for message retrieval
 */
public interface MessageListener {

    /**
     * This method is called as a message is retrieved so that listeners may process the message.
     *
     * @param message Original message
     * @return processed message
     */
    String onMessageRetrieved(String message);
}
