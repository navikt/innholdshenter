package no.nav.innholdshenter.filter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wraps the response from the servlet. Used to decorate response.
 */
public class DecoratorResponseWrapper extends HttpServletResponseWrapper {

    private static final String FEILMELDING_GET_WRITER_HAS_ALREADY_BEEN_CALLED = "getWriter() has already been called!";
    private static final String FEILMELDING_GET_OUTPUT_STREAM_HAS_ALREADY_BEEN_CALLED = "getOutputStream() has already been called!";
    private ByteArrayServletOutputStream stream = null;
    private PrintWriter writer = null;
    private String contentType = null;
    private HttpServletResponse origResponse = null;

    public DecoratorResponseWrapper(HttpServletResponse response) {
        super(response);
        origResponse = response;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
        super.setContentType(contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public String getOutputAsString() {
        if (stream != null) {
            return stream.toString();
        }
        return "";
    }

    public byte[] getOutputAsByteArray() {
        if (stream != null) {
            return stream.getByteArrayOutputStream().toByteArray();
        } else {
            return "".getBytes();
        }
    }

    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }

        if (stream != null) {
            stream.flush();
        }
    }

    @Override
    public int getBufferSize() {
        if (stream != null) {
            return stream.getSize();
        }

        return 0;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if (writer != null) {
            throw new IllegalStateException(FEILMELDING_GET_WRITER_HAS_ALREADY_BEEN_CALLED);
        }

        if (stream == null) {
            stream = new ByteArrayServletOutputStream(origResponse.getCharacterEncoding());
        }

        return stream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer != null) {
            return writer;
        }

        if (stream != null) {
            throw new IllegalStateException(FEILMELDING_GET_OUTPUT_STREAM_HAS_ALREADY_BEEN_CALLED);
        }

        stream = new ByteArrayServletOutputStream(origResponse.getCharacterEncoding());
        writer = new PrintWriter(new OutputStreamWriter(stream, origResponse.getCharacterEncoding()));
        return writer;
    }

    @Override
    public void reset() {
        resetBuffer();
        super.reset();
    }

    @Override
    public void resetBuffer() {
        if (stream != null) {
            stream.reset();
        }
    }

    @Override
    public void setBufferSize(int size) throws IllegalStateException {
        try {
            if (stream != null) {
                stream.setSize(size);
            }
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe.getMessage());
        }
    }
}
