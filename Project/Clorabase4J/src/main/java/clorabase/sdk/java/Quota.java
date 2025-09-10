package clorabase.sdk.java;

import java.util.Date;

public class Quota {
    public int remaining;
    public long resetSeconds;
    public Date resetTime;

    public Quota(int remaining, long resetSeconds) {
        this.remaining = remaining;
        this.resetSeconds = resetSeconds;
        this.resetTime = new Date(resetSeconds*1000L);
    }
}
