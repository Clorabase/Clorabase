package clorabase.sdk.java.database;

import java.io.FileNotFoundException;
import clorabase.sdk.java.Reason;

/**
 * Exception class for Clorastore database operations.
 * This class extends the Exception class and provides additional context
 * for errors that occur during database operations.
 */
public class ClorastoreException extends Exception {
    private final Reason reason;

    public ClorastoreException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public ClorastoreException(String message, Throwable cause) {
        super(message, cause);
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Returns the reason for the exception.
     * @return Reason enum indicating the specific reason for the exception.
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Handles an exception by wrapping it into a ClorastoreException.
     * @param e The exception to handle
     * @param notFoundMessage Message to use if the exception is a FileNotFoundException
     * @param defaultMessage Message to use for other exceptions
     * @throws ClorastoreException The wrapped exception
     */
    public static void handle(Exception e, String notFoundMessage, String defaultMessage) throws ClorastoreException {
        if (e instanceof ClorastoreException) {
            throw (ClorastoreException) e;
        } else if (e instanceof FileNotFoundException) {
            throw new ClorastoreException(notFoundMessage, Reason.NOT_EXISTS);
        } else {
            throw new ClorastoreException(defaultMessage, e);
        }
    }

    /**
     * Handles an exception by wrapping it into a ClorastoreException with a default message.
     * @param e The exception to handle
     * @param message The message to use
     * @throws ClorastoreException The wrapped exception
     */
    public static void handle(Exception e, String message) throws ClorastoreException {
        handle(e, message, message);
    }
}
