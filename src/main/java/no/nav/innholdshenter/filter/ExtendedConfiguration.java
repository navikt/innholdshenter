package no.nav.innholdshenter.filter;

import java.util.Map;

/**
 * Utvidet konfigurasjon for DecoratorFilter (brukes først og fremst av SBL Arbeid)
 */
public class ExtendedConfiguration {
    private Map<String, String> menuMap;
    private Map<String, String> subMenuPathMap;

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
}
