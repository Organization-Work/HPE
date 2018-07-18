package com.autonomy.find.util;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;

@Service
public class TimeHelper {

    public TimeHelper() {

    }

    /**
     *
     * @return returns a timestamp of the current time
     */
	public Timestamp getCurrentTimestamp() {
		return new Timestamp(new Date().getTime());
	}

    /**
     *
     * @param offset - in milliseconds
     * @return returns a timestamp of the current time plus an offset
     */
	public Timestamp getCurrentTimestampWithOffset(final long offset) {
		return new Timestamp(new Date().getTime() + offset);
	}


    public long calculateDaysAgo(final long offset) {
        return (System.currentTimeMillis() / 1000L) - (60 * 60 * 24 * offset);
    }
}
