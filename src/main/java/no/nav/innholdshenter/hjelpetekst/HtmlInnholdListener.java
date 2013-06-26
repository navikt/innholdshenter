package no.nav.innholdshenter.hjelpetekst;

/**
 * Listener API for HtmlInnhold
 */
public interface HtmlInnholdListener {

    /**
     * Kalles hver gang html innhold hentes fra EVS. Gir mulghet for å endre innhold før det ender hos mottaker.
     *
     * @param htmlInnhold original htmlInnhold
     * @return prosessert htmlInnhold
     */
    HtmlInnhold onHtmlInnholdRetrieved(HtmlInnhold htmlInnhold);
}
