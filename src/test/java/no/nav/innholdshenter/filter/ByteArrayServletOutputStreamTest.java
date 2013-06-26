package no.nav.innholdshenter.filter;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.IOException;

/**
 * Tester ByteArrayServletOutputStream
 */
public class ByteArrayServletOutputStreamTest {

   @Test
   public void shouldKeepOutput() throws IOException {
      ByteArrayServletOutputStream sut = new ByteArrayServletOutputStream("iso-8859-1");

      String str = "Dette er en streng.";

      for(byte b : str.getBytes()) {
         sut.write(b);
      }

      assertEquals("Hold ikke på innholdet", str, sut.getByteArrayOutputStream().toString());
   }

}
