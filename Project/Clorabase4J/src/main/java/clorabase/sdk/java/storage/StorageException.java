package clorabase.sdk.java.storage;

import clorabase.sdk.java.Reason;
import clorabase.sdk.java.database.ClorastoreException;

public class StorageException extends ClorastoreException {
    public StorageException(String message, Reason reason) {
        super(message, reason);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
