package clorabase.sdk.java.storage;

/**
 * ProgressListener interface for monitoring the progress of file operations.
 */
public interface ProgressListener {
    void onProgress(long bytesRead, long totalBytes);
    void onComplete();
    void onError(Exception e);
}
