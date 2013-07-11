package no.nav.innholdshenter.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Keeps the response from server. Used by response wrapper
 */
class ByteArrayServletOutputStream extends ServletOutputStream {

    private static final Logger logger = LoggerFactory.getLogger(ByteArrayServletOutputStream.class);
    private static final String FEILMELDING_UNABLE_TO_CONVERT_STREAM_TO_STRING = "Unable to convert stream to string. Unsupported encoding used in request.";

    private ByteArrayOutputStream stream = null;
    private String encoding = null;


    public ByteArrayServletOutputStream(String encoding) {
        this.stream = new ByteArrayOutputStream();
        this.encoding = encoding;
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return stream;
    }

    @Override
    public String toString() {
        try {
            return stream.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            logger.error(FEILMELDING_UNABLE_TO_CONVERT_STREAM_TO_STRING, e);
        }

        return "";
    }

    @Override
    public void write(int b) {
        stream.write(b);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    public int getSize() {
        return stream.size();
    }

    public void setSize(int size) throws IOException {
        ByteArrayOutputStream oldStream = stream;
        stream = new ByteArrayOutputStream(size);
        stream.write(oldStream.toByteArray());
    }

    public void reset() {
        stream.reset();
    }
}
