package no.nav.innholdshenter.message;

/**
 * Adapter til StringRetriever grensesnittet som returnerer nøkkelverdi hvis ingen verdi er funnet. 
 */
public class DebugAdapter implements StringRetriever{
    private static final String MISSING_KEY_TEMPLATE = "<b>[%s locale:%s, variant:%s]</b>";
    
    private StringRetriever stringRetriever;

    public DebugAdapter(StringRetriever stringRetriever) {
        this.stringRetriever = stringRetriever;
    }

    public String retrieveString(String key, String locale, String variant) {
        String value = stringRetriever.retrieveString(key, locale, variant);
        return value == null ? String.format(MISSING_KEY_TEMPLATE, key, locale, variant) : value;
    }
}
