package sdk.clorabase.clorastore;

import com.clorabase.clorastore.Reasons;

public class ClorastoreException extends RuntimeException {
    private final String message;
    private final Reasons reason;

    public ClorastoreException(String message, Reasons reason) {
        super(message);
        this.message = message;
        this.reason = reason;
    }
}
