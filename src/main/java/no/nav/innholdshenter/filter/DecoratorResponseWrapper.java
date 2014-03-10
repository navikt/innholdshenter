package no.nav.innholdshenter.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Wraps the response from the servlet. Used to decorate response.
 */
class DecoratorResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayServletOutputStream stream;
    private PrintWriter writer;
    private HttpServletResponse originalResposne;

    public DecoratorResponseWrapper(HttpServletResponse response) {
        super(response);
        originalResposne = response;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called!");
        }

        if (stream == null) {
            stream = new ByteArrayServletOutputStream(originalResposne.getCharacterEncoding());
        }

        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }

        if (stream != null) {
            throw new IllegalStateException("getOutputStream() has already been called!");
        }

        stream = new ByteArrayServletOutputStream(originalResposne.getCharacterEncoding());
        writer = new PrintWriter(new OutputStreamWriter(stream, originalResposne.getCharacterEncoding()));
        return writer;
    }

    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }

        if (stream != null) {
            stream.flush();
        }
    }

    public String getOutputAsString() {
        if (stream != null) {
            return stream.toString();
        }
        return "";
    }
}
