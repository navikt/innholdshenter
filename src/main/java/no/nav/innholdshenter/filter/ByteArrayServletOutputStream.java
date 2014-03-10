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

    @Override
    public String toString() {
        try {
            return stream.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            logger.error("Unable to convert stream to string. Unsupported encoding ({}) used in request: {}", encoding, e.getMessage());
        }

        return "";
    }
}
