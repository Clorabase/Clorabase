package com.clorabase.clorograph;

import com.google.android.gms.tasks.Task;

import db.clorabase.clorograph.Savable;
import db.clorabase.clorograph.graphs.Graph;

public interface OnCompleteCallback<T> {
    void onFetched(T fetched);
    void onUpdated();
    void onFailure(Exception e);
}
