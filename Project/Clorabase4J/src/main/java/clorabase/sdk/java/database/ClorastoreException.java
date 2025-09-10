package clorabase.sdk.java.database;

import clorabase.sdk.java.Reason;

/**
 * Exception class for Clorastore database operations.
 * This class extends the Exception class and provides additional context
 * for errors that occur during database operations.
 */
public class ClorastoreException extends Exception {
    private Reason reason;

    public ClorastoreException(String message,Reason reason) {
        super(message);
        this.reason = reason;
    }

    public ClorastoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the reason for the exception.
     * @return Reason enum indicating the specific reason for the exception.
     */
    public Reason getReason() {
        return reason;
    }
}
