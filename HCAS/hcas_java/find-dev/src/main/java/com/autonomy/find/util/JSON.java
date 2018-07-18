package com.autonomy.find.util;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.Reader;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/util/JSON.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: wo $ on $Date: 2013/10/18 $
 */
public class JSON {
	public static String toJSON(final Object object) throws IOException {
		return new ObjectMapper().writeValueAsString(object);
	}

    public static <T> T toObject(Reader src, Class<T> valueType) throws IOException {
        return new ObjectMapper().readValue(src, valueType);
    }
}
