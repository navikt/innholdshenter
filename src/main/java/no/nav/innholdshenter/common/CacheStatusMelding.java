package no.nav.innholdshenter.common;

import java.io.Serializable;

public class CacheStatusMelding implements Serializable {
    private int statusCode;
    private String melding;
    private long timestamp;

    public CacheStatusMelding(int statusCode, String melding, long timestamp) {
        this.statusCode = statusCode;
        this.melding = melding;
        this.timestamp = timestamp;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMelding() {
        return melding;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
