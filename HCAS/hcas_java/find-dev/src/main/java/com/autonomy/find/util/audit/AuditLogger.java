package com.autonomy.find.util.audit;

import com.autonomy.find.util.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class AuditLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);


    public static final void log(final String user,
                                 final AuditActions action){
        log(user, action, null);
    }

    public static final void log(final String user,
                                 final AuditActions action,
                                 final Object data) {
        if (StringUtils.isEmpty(user)) {
            throw new IllegalArgumentException("Missing user parameter.");
        }

        final String dataStr = data != null ? getLogString(data) : null;


        LOGGER.info("[user: {}], action: {}, data: {}", new Object[] {user, action, dataStr});

    }

    public static final Map<String, Object> getDataMap() {
        return new TreeMap<String, Object>();
    }

    private static final String getLogString(final Object data) {

        try {
            return JSON.toJSON(data);

        } catch (final IOException e) {
            return (data == null) ? null : data.toString();
        }
    }


}
