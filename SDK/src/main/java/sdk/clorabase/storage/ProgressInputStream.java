package sdk.clorabase.storage;

import java.io.IOException;
import java.io.InputStream;

public class ProgressInputStream extends InputStream {

    private final InputStream inputStream;
    private final long totalBytes;
    private final ClorabaseStorageCallback progressListener;

    private long bytesRead = 0;

    public ProgressInputStream(InputStream inputStream, long totalBytes, ClorabaseStorageCallback progressListener) {
        this.inputStream = inputStream;
        this.totalBytes = totalBytes;
        this.progressListener = progressListener;
    }

    @Override
    public int read() throws IOException {
        int byteRead = inputStream.read();
        if (byteRead != -1) {
            bytesRead++;
            int progress = (int) (bytesRead * 100 / totalBytes);
            progressListener.onProgress(progress);
        }
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int byteRead = inputStream.read(b, off, len);
        if (byteRead != -1) {
            bytesRead++;
            int progress = (int) (bytesRead * 100 / totalBytes);
            progressListener.onProgress(progress);
        }
        return byteRead;
    }


    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}