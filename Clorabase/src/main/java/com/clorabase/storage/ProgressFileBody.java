package com.clorabase.storage;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.http.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProgressFileBody extends AbstractContentBody {
    private final ProgressListener listener;
    private final InputStream in;
    private final String name;

    public ProgressFileBody(InputStream fileStream, String filename, ProgressListener listener) {
        super(ContentType.DEFAULT_BINARY);
        this.listener = listener;
        this.in = fileStream;
        name = filename;
    }


    @Override
    public String getFilename() {
        return name;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        Args.notNull(out, "Output stream");
        final byte[] tmp = new byte[1024];
        int l;
        long writtenBytes = 0;
        long totalBytes = in.available();
        while ((l = in.read(tmp)) != -1) {
            out.write(tmp, 0, l);
            writtenBytes += l;
            listener.onProgress((int) ((writtenBytes * 100) / totalBytes));
        }
    }

    @Override
    public String getTransferEncoding() {
        return MIME.ENC_BINARY;
    }

    @Override
    public long getContentLength() {
        try {
            return in.available();
        } catch (IOException e) {
            return 0;
        }
    }


    public interface ProgressListener {
        void onProgress(int percentage);
    }
}
