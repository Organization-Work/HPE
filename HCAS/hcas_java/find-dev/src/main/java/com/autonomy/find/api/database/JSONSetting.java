package com.autonomy.find.api.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSetter;

import lombok.Data;
import lombok.NonNull;

public @Data
class JSONSetting {

	private List<String> path;
	private Map<String, Object> data;

	/**
	 * Sets the setting's data
	 * @param data
	 * @return
	 */
	public JSONSetting setData(
			final Map<String, Object> data) {
		return setData(data, true);
	}

	/**
	 * Sets the setting's data
	 * @param data
	 * @param clone
	 * @return
	 */
	public JSONSetting setData(
			final Map<String, Object> data,
			final boolean clone) {
		this.data = clone ? new HashMap<String, Object>(data) : data;
		return this;
	}

	/**
	 * Sets the setting's path
	 * @param path
	 * @return
	 */
	public JSONSetting setPath(final @NonNull List<String> path) {
		return setPath(path, true);
	}

	/**
	 * Sets the setting's path
	 * @param path
	 * @param clone
	 * @return
	 */
	public JSONSetting setPath(
			final @NonNull List<String> path,
			final boolean clone) {

		this.path = clone ? new LinkedList<String>(path) : path;
		return this;
	}

	/**
	 * Sets the setting's path
	 * @param pathString
	 * @return
	 */
	@JsonSetter
	public JSONSetting setPath(final @NonNull String pathString) {
		final List<String> path = new LinkedList<String>();
        Collections.addAll(path, pathString.split("."));
		return setPath(path);
	}

	/**
	 * Sets the setting's path
	 * @return
	 */
	public boolean pathInvariant() {
		return (path.size() > 0);
	}
}