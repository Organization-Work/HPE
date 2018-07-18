package com.autonomy.find.idx;

import java.util.LinkedList;
import java.util.List;

public final class Idx {

	public static final String	DOCID				= "DOCID";
	public static final String	TITLE				= "DRETITLE";
	public static final String	REFERENCE			= "DREREFERENCE";
	public static final String	FIELDNAME			= "FIELDNAME";
	public static final String	FIELDVALUE			= "FIELDVALUE";
	public static final String	DELETEDFIELD		= "DELETEFIELD";
	public static final String	DELETEDFIELDVALUE	= "DELETEFIELDVALUE";

	public static final String	COLLECTION			= "%s#DREENDDATANOOP\n\n";
	public static final String	DOCUMENT_CONTENT	= "%s#DRECONTENT\n%s\n#DREENDDOC\n\n";
	public static final String	DOCUMENT			= "%s#DREENDDOC\n\n";
	public static final String	TERM				= "#DRE%s %s";
	public static final String	PAIR				= "%s=\"%s\"";

	private final Object		content;
	private final StringBuilder	document			= new StringBuilder();

	public Idx(final Object content, final Object... terms) {
		this.content = content;
		addTerms(terms);
	}

	public String toString() {
		if (content == null) {
			return String.format(DOCUMENT, document.toString()).replaceAll("\\n+", "\n") + "\n";
		} else {
			return String.format(DOCUMENT_CONTENT, document.toString(), content.toString()).replaceAll("\\n+", "\n") + "\n";
		}
	}

	public Idx addTerms(final Object... terms) {
		for (final Object term : terms) {
			this.document.append(term.toString()).append('\n');
		}
		return this;
	}

	private static String stringOf(final Object obj) {
		return (obj == null) ? "" : obj.toString();
	}

	/**
	 * Renders a collection of Idx documents snippets into a complete document.
	 * 
	 * @param documents
	 * @return
	 */
	public static String Render(final Idx... documents) {
		final StringBuilder result = new StringBuilder();
		for (final Idx doc : documents) {
			if (doc == null) {
				continue;
			}
			result.append(doc.toString());
		}
		return String.format(COLLECTION, result.toString());
	}

	public static String Term(final Object key, final Object value) {
		if (key == null) {
			return "";
		}
		return String.format(TERM, key.toString().toUpperCase(), stringOf(value));
	}

	public static String Pair(final Object key, final Object value) {
		if (key == null) {
			return "";
		}
		return String.format(PAIR, key.toString(), stringOf(value));
	}

	public static String Field(final Object key, final Object value) {
		if (key == null) {
			return "";
		}
		return Term("Field", Pair(key, value));
	}

	public static String FieldName(final Object key) {
		return Term(FIELDNAME, key);
	}

	public static String FieldValue(final Object value) {
		return Term(FIELDVALUE, value);
	}

	public static String UpdatedField(final Object key, final Object value) {
		if (key == null) {
			return "";
		}
		return String.format("%s\n%s", FieldName(key), FieldValue(value));
	}

	public static String DeleteField(final Object key) {
		if (key == null) {
			return "";
		}
		return Term(DELETEDFIELD, key);
	}

	/**
	 * 
	 * @param <O>
	 * @param key
	 * @return
	 */
	public static <O> String DeleteFieldValue(final O key) {
		if (key == null) {
			return "";
		}
		return String.format("%s\n%s", FieldName(key), Term(DELETEDFIELDVALUE, "Yes"));
	}

	/**
	 * Defining a multi-valued field. Used in a DreAdd command.
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public static String MultiField(final String key, final List<?> values) {
		final StringBuilder result = new StringBuilder();
		boolean first = true;
		for (final Object value : values) {
			if (value == null) {
				continue;
			}
			if (!first) {
				result.append('\n');
			} else {
				first = false;
			}
			result.append(Field(key, value));
		}
		return result.toString();
	}

	/**
	 * Update a muti-valued field. Used in a DreReplace command.
	 * 
	 * @param key
	 * @param values
	 * @return
	 */
	public static String UpdatedMultiField(final String key, final List<?> inputValues, final boolean deleteWhenEmpty, final boolean ignoreEmptyValues) {
		final List<String> values;

		// Filter the values to remove null or empty elements (if required)
		values = new LinkedList<String>();
		for (final Object value : inputValues) {
			if (value == null) {
				continue;
			}
			final String valString = value.toString();
			if ((ignoreEmptyValues && valString.trim() == "")) {
				continue;
			}
			values.add(valString);
		}

		// No elements? and required value deletion?
		if (values.size() == 0 && deleteWhenEmpty) {
			// Declare that the value should be deleted
			return DeleteFieldValue(key);
		}

		final StringBuilder result = new StringBuilder();
		boolean first = true;

		for (final String value : values) {
			if (!first) {
				result.append('\n');
			} else {
				first = false;
			}
			result.append(UpdatedField(key, value));
		}

		return result.toString();
	}
}