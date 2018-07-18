package com.autonomy.find.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapTreeWalker implements TreeWalker {

	private final Map<String, Object> model;

	/**
	 * Map Tree Walker Constructor
	 * 
	 * @param map
	 */
	public MapTreeWalker(final Map<String, Object> map) {
		this.model = map;
	}

	/**
	 * Selects data from a tree given a key path to walk.
	 * 
	 * @param path
	 * @return this
	 */
	public Object select(final List<String> path) {
		final int last = path.size() - 1;
		@SuppressWarnings("unchecked")
		final Map<String, Object> subModel = (Map<String, Object>)walkPath(this.model, path, last);
		final String node = path.get(last);
        if (subModel.containsKey(node)) {
            return subModel.get(node);
        }
        throw new RuntimeException("Path not found.");
	}

	/**
	 * Updates data from in tree given a key path to walk.
	 * 
	 * @param path
	 * @param data
	 * @return this
	 */
	public TreeWalker update(final List<String> path, final Object data) {
		final int last = path.size() - 1;
		final Map<String, Object> subModel = forcePath(this.model, path, last);
		final String node = path.get(last);
		subModel.put(node, data);
		return this;
	}

	/**
	 * Forces a path to exist in a tree.
	 * 
	 * @param model
	 * @param path
	 * @param depth
	 * @return The model locate at the required depth/end of the path - either created or found
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> forcePath(
			final Map<String, Object> model,
			final List<String> path,
			final int depth) {

		final int max = (int) Math.min(path.size() - 1, depth);
		Map<String, Object> subModel = model;
		String node;

		for (int i = 0; i < max; i += 1) {

			node = path.get(i);

			if (!(subModel.containsKey(node)) || !(subModel.get(node) instanceof Map)) {
				subModel.put(node, new HashMap<String, Object>());
			}

			subModel = (Map<String, Object>) subModel.get(node);
		}

		return subModel;
	}

	/**
	 * Walks through a tree given a specific key path and a depth.
	 * 
	 * @param model
	 * @param path
	 * @param depth
	 * @return the data at the end of the path.
	 */
	@SuppressWarnings("unchecked")
	private static Object walkPath(
			final Map<String, Object> model,
			final List<String> path,
			final int depth) {

		final int max = (int) Math.min(path.size() - 1, depth);
		Map<String, Object> subModel = model;
		String node;

		// for each node in path.init
		for (int i = 0; i < max; i += 1) {
			node = path.get(i);
			if (subModel.containsKey(node)) {
				try {
					subModel = (Map<String, Object>) subModel.get(node);
				} catch (final RuntimeException e) {
					throw new RuntimeException("Path not found.", e);
				}
			} else {
                throw new RuntimeException("Path not found.");
			}
		}

		return subModel;
	}

	/**
	 * 
	 * @param map
	 * @return
	 */
	public static MapTreeWalker forMap(final Map<String, Object> map) {
		return new MapTreeWalker(map);
	}
}
