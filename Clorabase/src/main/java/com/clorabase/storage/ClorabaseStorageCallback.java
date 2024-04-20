package com.clorabase.storage;

import androidx.annotation.NonNull;

public interface ClorabaseStorageCallback {
    void onFailed(@NonNull Exception e);
    void onProgress(int percent);
    void onComplete();
}