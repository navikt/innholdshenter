package no.nav.innholdshenter.common;

import java.io.Serializable;

public class CacheStatusFeilmelding implements Serializable {
    private int statusCode;
    private String feilmelding;
    private long timestamp;

    public CacheStatusFeilmelding(int statusCode, String feilmelding, long timestamp) {
        this.statusCode = statusCode;
        this.feilmelding = feilmelding;
        this.timestamp = timestamp;
    }
    public int getStatusCode() {
        return statusCode;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
