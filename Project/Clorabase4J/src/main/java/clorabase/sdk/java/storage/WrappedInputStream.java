package clorabase.sdk.java.storage;

import java.io.IOException;
import java.io.InputStream;

public class WrappedInputStream extends InputStream {
    private final InputStream wrapped;
    private final long totalBytes;
    private long bytesRead = 0;
    private final ProgressListener listener;
    private boolean completed = false;

    public WrappedInputStream(InputStream wrapped, ProgressListener listener) throws IOException {
        this.wrapped = wrapped;
        this.listener = listener;
        this.totalBytes = wrapped.available();
    }

    @Override
    public int read() throws IOException {
        try {
            int b = wrapped.read();
            if (b != -1) {
                bytesRead++;
                listener.onProgress(bytesRead, totalBytes);
            }
            return b;
        } catch (IOException e) {
            listener.onError(e);
            throw e;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            int n = wrapped.read(b, off, len);
            if (n > 0) {
                bytesRead += n;
                listener.onProgress(bytesRead, totalBytes);
            }
            return n;
        } catch (IOException e) {
            listener.onError(e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            wrapped.close();
        } catch (IOException e) {
            listener.onError(e);
            throw e;
        }
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}
