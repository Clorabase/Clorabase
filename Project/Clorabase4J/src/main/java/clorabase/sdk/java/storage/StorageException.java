package clorabase.sdk.java.storage;

import java.io.FileNotFoundException;
import clorabase.sdk.java.Reason;
import clorabase.sdk.java.database.ClorastoreException;

public class StorageException extends ClorastoreException {
    public StorageException(String message, Reason reason) {
        super(message, reason);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Handles an exception by wrapping it into a StorageException.
     * @param e The exception to handle
     * @param notFoundMessage Message to use if the exception is a FileNotFoundException
     * @param defaultMessage Message to use for other exceptions
     * @throws StorageException The wrapped exception
     */
    public static void handle(Exception e, String notFoundMessage, String defaultMessage) throws StorageException {
        if (e instanceof StorageException) {
            throw (StorageException) e;
        } else if (e instanceof FileNotFoundException) {
            throw new StorageException(notFoundMessage, Reason.NOT_EXISTS);
        } else {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("\"status\":\"422\"") || msg.contains("\"code\":\"already_exists\""))) {
                throw new StorageException(defaultMessage, Reason.File_ALREADY_EXISTS);
            }
            throw new StorageException(defaultMessage, e);
        }
    }

    /**
     * Handles an exception by wrapping it into a StorageException with a default message.
     * @param e The exception to handle
     * @param message The message to use
     * @throws StorageException The wrapped exception
     */
    public static void handle(Exception e, String message) throws StorageException {
        handle(e, message, message);
    }
}
