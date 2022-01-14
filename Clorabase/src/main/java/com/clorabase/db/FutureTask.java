package com.clorabase.db;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public final class FutureTask<T> {
    protected T value;
    protected Throwable exception;
    protected boolean isCompletionCallback;
    protected OnSuccessListener<T> successListener = value -> {};
    protected OnFailureListener failureListener = exception -> {};


    public FutureTask<T> addOnSuccessListener(OnSuccessListener<T> listener) {
        successListener = listener;
        return this;
    }

    public FutureTask<T> addOnFailureListener(OnFailureListener listener) {
        failureListener = listener;
        return this;
    }

    public T getValue() {
        return value;
    }

    public boolean isSuccessful() {
        return exception == null;
    }

    public Throwable getException() {
        return exception;
    }

    public interface OnSuccessListener<T> {
        void onSuccess(T value);
    }

    public interface OnCompleteCallback {
        void onSuccess();

        void onFailed(Throwable e);
    }

    public interface OnCompletionListener<T> {
        void onComplete(FutureTask<T> task);
    }

    public interface OnFailureListener {
        void onFailed(Throwable exception);
    }

}
