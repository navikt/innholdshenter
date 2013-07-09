package no.nav.innholdshenter.common;

import no.nav.innholdshenter.tools.InnholdshenterTools;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RandomStringUtils.class)
public class RandomizeURLTest {
    public static final String RANDOMSTRING = "AAAb312RAWEFF";
    EnonicContentRetriever enonicContentRetriever;

    private static final String cacheName = "TestEhcacheCacheName";
    private static final String SERVER = "http://www-t1.nav.no:9000/";
    private static final String PATH = "systemsider/ApplicationFrame";
    private static final String URLPARAMS = "?1&fdsafad=321&fine=cool";

    @Before
    public void setUp() {
        enonicContentRetriever = new EnonicContentRetriever(cacheName);
        
    }
    
    @Test
    public void testGenerateRandomURL() {
        mockStatic(RandomStringUtils.class);

        when(RandomStringUtils.randomAlphanumeric(anyInt())).thenReturn(RANDOMSTRING);
        String url = SERVER + PATH;
        String randomUrl = InnholdshenterTools.makeRandomUrl(url);

        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid(randomUrl));
        assertEquals(url + "?sid=" + RANDOMSTRING, randomUrl);
    }

    @Test
    public void testGenerateRandomURLWithLeadingParams() {
        mockStatic(RandomStringUtils.class);

        when(RandomStringUtils.randomAlphanumeric(anyInt())).thenReturn(RANDOMSTRING);
        String url = SERVER + PATH + URLPARAMS;
        String randomUrl = InnholdshenterTools.makeRandomUrl(url);

        assertTrue(randomUrl.contains("sid=" + RANDOMSTRING));
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid(randomUrl));
    }
}
