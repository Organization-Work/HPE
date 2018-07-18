package com.autonomy.find.util;

import java.util.ArrayList;
import java.util.List;

public class PathLexer {
	public static List<String> lex(final String pathString) {

		final String[] arr = pathString.split("/+");
		final List<String> path = new ArrayList<String>(arr.length);
		for (final String node : arr) {
			if (!("".equals(node))) {
				path.add(node);
			}
		}
		return path;
	}
}
