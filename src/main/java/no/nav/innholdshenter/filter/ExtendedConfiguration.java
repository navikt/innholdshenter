package no.nav.innholdshenter.filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Utvidet konfigurasjon for DecoratorFilter (brukes først og fremst av SBL Arbeid)
 */
public class ExtendedConfiguration {
    private Map<String, String> menuMap = new HashMap<>();
    private Map<String, String> subMenuPathMap = new HashMap<>();
    private Map<String, String> tnsValues = new HashMap<>();

    public void setMenuMap(Map<String, String> menuMap) {
        this.menuMap = menuMap;
    }

    public Map<String, String> getMenuMap() {
        return menuMap;
    }

    public void setSubMenuPathMap(Map<String, String> subMenuPathMap) {
        this.subMenuPathMap = subMenuPathMap;
    }

    public Map<String, String> getSubMenuPathMap() {
        return subMenuPathMap;
    }

    public Map<String, String> getTnsValues() {
        return tnsValues;
    }

    public void setTnsValues(Map<String, String> tnsValues) {
        this.tnsValues = tnsValues;
    }
}
