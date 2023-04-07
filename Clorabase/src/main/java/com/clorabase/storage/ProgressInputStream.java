package com.clorabase.storage;

import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ProgressInputStream extends FileInputStream {
    private long readBytes = 0;
    private final long totalBytes;
    private final ClorabaseStorageCallback callback;
    private final Handler handler;

    public ProgressInputStream(File file, ClorabaseStorageCallback callback, Handler handler) throws FileNotFoundException {
        super(file);
        totalBytes = file.length();
        this.callback = callback;
        this.handler = handler;
    }

    @Override
    public int read() throws IOException {
        var read = super.read();
        readBytes += read;
        if (read != -1) trackProgress();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        var read = super.read(b, off, len);
        readBytes += read;
        if (read != -1) trackProgress();
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        var read = super.skip(n);
        readBytes += read;
        trackProgress();
        return read;
    }

    private void trackProgress(){
        int percentage = (int) ((readBytes*100)/totalBytes);
        handler.post(() -> callback.onProgress(percentage));
    }
}
