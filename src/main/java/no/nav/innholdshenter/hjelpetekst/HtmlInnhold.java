package no.nav.innholdshenter.hjelpetekst;

import java.io.Serializable;

/**
 * HtmlInnhold
 */
public class HtmlInnhold implements Serializable {
    private String title;
    private String html;
    private String key;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "HtmlInnhold{" +
                "key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", html='" + html + '\'' +
                '}';
    }
}
