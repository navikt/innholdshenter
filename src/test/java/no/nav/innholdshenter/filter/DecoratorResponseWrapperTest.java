package no.nav.innholdshenter.filter;

import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DecoratorResponseWrapperTest {
    private static final String ISO_8859_1 = "iso-8859-1";

    @Test
    public void shouldKeepOutputForStream() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getCharacterEncoding()).thenReturn(ISO_8859_1);

        DecoratorResponseWrapper sut = new DecoratorResponseWrapper(response);

        ServletOutputStream out = sut.getOutputStream();
        out.write("oneæøå".getBytes(ISO_8859_1));
        out.write("twoæøå".getBytes(ISO_8859_1));

        assertEquals("oneæøåtwoæøå", sut.getOutputAsString());
    }

    @Test
    public void shouldKeepOutputForWriter() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getCharacterEncoding()).thenReturn(ISO_8859_1);

        DecoratorResponseWrapper sut = new DecoratorResponseWrapper(response);

        PrintWriter out = sut.getWriter();
        out.print("one");
        out.write("two");
        out.append('t');
        out.flush();

        assertEquals("onetwot", sut.getOutputAsString());
    }

    @Test(expected = IllegalStateException.class)
    public void writerThenOutputStreamShouldThrowException() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getCharacterEncoding()).thenReturn(ISO_8859_1);

        DecoratorResponseWrapper sut = new DecoratorResponseWrapper(response);

        sut.getWriter();
        sut.getOutputStream();
    }

    @Test(expected = IllegalStateException.class)
    public void outputStreamThenWriterShouldThrowException() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        DecoratorResponseWrapper sut = new DecoratorResponseWrapper(response);

        sut.getOutputStream();
        sut.getWriter();
    }

}
