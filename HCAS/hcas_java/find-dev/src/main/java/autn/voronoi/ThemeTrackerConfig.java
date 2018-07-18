package autn.voronoi;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/ThemeTrackerConfig.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
public class ThemeTrackerConfig {
	private final Map<String, String> jobs;

	// The default time span, in seconds
	private final long defaultTimeSpan;

	private final boolean timeSpanUI;

	public ThemeTrackerConfig(final boolean timeSpanUI, final long defaultTimeSpan, final Map<String, String> jobs) {
		this.timeSpanUI = timeSpanUI;
		this.defaultTimeSpan = defaultTimeSpan;
		this.jobs = jobs;
	}

	public ThemeTrackerConfig(final boolean timeSpanUI, final long defaultTimeSpan, final String jobs) throws IOException {
		this(timeSpanUI, defaultTimeSpan, new ObjectMapper().<Map<String, String>>readValue(jobs, new TypeReference<Map<String, String>>() {
		}));
	}

	public Map<String, String> getJobs() {
		return jobs;
	}

	public long getDefaultTimeSpan() {
		return defaultTimeSpan;
	}

	public boolean isTimeSpanUI() {
		return timeSpanUI;
	}
}
