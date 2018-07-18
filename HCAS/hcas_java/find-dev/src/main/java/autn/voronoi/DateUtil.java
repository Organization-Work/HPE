package autn.voronoi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/DateUtil.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class DateUtil {
	public static String formatDate(final long epochSeconds) {
		if (epochSeconds > 0) {
			return epochSeconds + "e";
		}

		// IDOL doesn't allow negative values in the mindate/maxdate epoch date format
		final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(new Date(epochSeconds * 1000));
	}
}
