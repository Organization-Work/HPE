/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/database/Databases.java#7 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.database;

import com.autonomy.aci.content.identifier.QueryIdentifiers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * <p>Helper class for building database restrictions, such as those used in the <tt>databasematch</tt> parameter of a
 * query. Individual database names are represented as Strings and deduplicated where possible.
 *
 * <p>Some of the characteristics of the {@code append} methods of this class might be considered inconsistent with the
 * behaviour of IDOL's <tt>databasematch</tt> parameter. This has been done to make those methods behave as a logical
 * <tt>OR</tt> operations in all circumstances. The IDOL behaviour which is not honoured is:
 *
 * <ul>
 *   <li>An empty <tt>databasematch</tt> will match all non-internal databases, whereas <tt>*</tt> will match all
 *       databases.
 *   <li>If the first non-blank database in the <tt>databasematch</tt> is a <tt>*</tt> then it will match everything. If
 *       the <tt>*</tt> appears later in the <tt>databasematch</tt> it is ignored.
 * </ul>
 *
 * Instead, {@code Databases} exhibits the following behaviour:
 *
 * <ul>
 *   <li>If <tt>*</tt> is added to an instance then all other values will be discarded.
 *   <li>If an instance is empty then its string representation will be a nonsense string intended to match no
 *       databases.
 *   <li>An instance cannot be created that has an empty string representation.
 *   <li>{@link #parse(String)} treats an empty string as equivalent to <tt>*</tt>.
 * </ul>
 *
 * <p>This class makes no attempt at internal synchronization and an instance is not safe to be used by multiple threads
 * without external synchronization.
 */
public class Databases implements Iterable<String>, QueryIdentifiers {

    private static final String MATCH_NOTHING = "MATCH_NOTHING_9be3e331-8046-4182-9e2d-ebbcf12f6e4c";
    private static final Pattern SEPARATORS = Pattern.compile("[,\\+ ]");
    private static final Pattern INVALID = Pattern.compile(".*[,\\+ ].*");
    private static final Pattern SPACES = Pattern.compile(" *");

    private final Set<String> values = new LinkedHashSet<String>();

    /**
     * <p>An instance of {@code Databases} that corresponds to all databases, i.e. *
     *
     * <p>This instance is immutable, attempting to append further databases will only validate their names, it will not
     * actually append them.
     */
    public static final Databases ALL = new Databases("*");

    /**
     * Create a new instance using the database names provided.
     *
     * @param databases The names of the databases
     */
    public Databases(final Iterable<String> databases) {
        doAppend(databases);
    }

    /**
     * Create a new instance using the database names provided.
     *
     * @param databases The names of the databases
     */
    public Databases(final String... databases) {
        doAppend(Arrays.asList(databases));
    }

    /**
     * Appends the given database names to this instance.
     *
     * @param values The names of the databases
     * @return {@code this}
     */
    public Databases append(final Iterable<String> values) {
        return doAppend(values);
    }

    /**
     * Appends the given database names to this instance.
     *
     * @param values The names of the databases
     * @return {@code this}
     */
    public Databases append(final String... values) {
        return doAppend(Arrays.asList(values));
    }

    private Databases doAppend(final Iterable<String> databases) {
        Validate.notNull(databases, "Databases must not be null");

        // In case of nulls, build a new Set and then add them all
        final Set<String> newDatabases = new LinkedHashSet<String>();

        for (final String database : databases) {
            Validate.isTrue(StringUtils.isNotEmpty(database), "One of the database names provided was empty");
            Validate.isTrue(!INVALID.matcher(database).matches(), "A database name cannot contain a plus, comma or whitespace: [" + database + "]");

            // Split on comma, plus and space
            newDatabases.add(database);
        }

        // Here we use contains rather than just this.equals(ALL) to avoid using an overridable method from a
        // constructor. Once the use of 'final' is sorted out for this class we might change it back to the more obvious
        // version.
        if (!this.values.contains("*")) {
            if (newDatabases.contains("*")) {
                this.values.clear();
                this.values.add("*");
            }
            else {
                this.values.addAll(newDatabases);
            }
        }

        return this;
    }

    /**
     * {@link Iterator} for the database names. Removing elements is not currently supported.
     *
     * @return A suitable iterator
     */
    @Override
    public Iterator<String> iterator() {
        // For now we disable removal
        return Collections.unmodifiableSet(values).iterator();
    }

    /**
     * The number of database names currently in this instance.
     *
     * @return Number of databases
     */
    public int size() {
        return values.size();
    }

    /**
     * Whether or not size is 0. Note that, for consistency reasons, empty corresponds to matching no databases rather
     * than all databases and the implementations of {@link #toString()} and {@link #toIndexingString()} takes this into
     * account.
     *
     * @return {@code true} if and only if {@code size() == 0}
     */
    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Two {@code Databases} objects are considered equal if they contain the same set of database names. Equality is
     * order invariant.
     *
     * @param obj A object to test for equality
     * @return {@code true} if the {@code obj} is equal to {@code this}
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof Databases && this.values.equals(((Databases) obj).values);
    }

    /**
     * A suitable hashcode implementation.
     *
     * @return The hashcode
     */
    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /**
     * <p>A {@code String} representation of the databases, suitable for use in a <tt>query</tt> or <tt>getcontent</tt>
     * action.
     *
     * <p>If this object is empty then this method will return a nonsense value that will not match any database. This
     * makes appending databases consistently an <tt>OR</tt> operation and also closes a common security hole. Note that
     * when building a query it is generally preferred to explicitly check {@link #isEmpty()} rather than proceeding to
     * execute a query that matches no documents.
     *
     * @return A string representation of the databases
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        for(final String value : this) {
            // Oddly, this doesn't need URL encoding
            builder.append(value).append('+');
        }

        if(builder.length() == 0) {
            return MATCH_NOTHING;
        }

        builder.deleteCharAt(builder.length() - 1);

        return builder.toString();
    }

    /**
     *
     * @return {@code "databasematch"} or equivalent
     */
    @Override
    public String getMatchParameterName() {
        return DATABASE_MATCH;
    }

    /**
     *
     * @return {@code "DREDBNAME"} or equivalent
     */
    @Override
    public String getIndexingIdentifierName() {
        return DREDBNAME;
    }

    /**
     * <p>A {@code String} representation of the databases, suitable for use in a <tt>DREREPLACE</tt> index command.
     *
     * <p>If this object is empty then current behaviour is to return a nonsense value. Note that this behaviour should
     * not be relied upon and an explicit check using {@link #isEmpty()} should be used instead.
     *
     * @return A string representation of the databases
     */
    @Override
    public String toIndexingString() {
        return toString();
    }

    /**
     * Converts an iterable of strings into a {@code Databases}. This method can be more efficient than using the
     * equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param databases The references to convert
     * @return An equivalent instance of {@code Databases}
     */
    public static Databases from(final Iterable<String> databases) {
        return (databases instanceof Databases) ? (Databases)databases : new Databases(databases);
    }

    /**
     * Parses a {@code String} of database names in the format used in a <tt>query</tt> or <tt>getcontent</tt> action.
     * This includes parsing the output of {@link #toString()}. An empty string will be treated as equivalent to
     * <tt>*</tt>, even though technically there is a slight difference when working with internal databases.
     *
     * @param databases The string representation to parse
     * @return A {@code Databases} object
     */
    public static Databases parse(final String databases) {
        Validate.notNull(databases, "Databases must not be null");

        if (SPACES.matcher(databases).matches()) {
            return Databases.ALL;
        }
        
        final Set<String> databasesSet = new LinkedHashSet<String>(Arrays.asList(SEPARATORS.split(databases)));
        databasesSet.remove("");

        if (databasesSet.isEmpty()) {
            return new Databases();
        }

        if ("*".equals(databasesSet.iterator().next())) {
            return ALL;
        }

        databasesSet.remove("*");
        databasesSet.remove(MATCH_NOTHING);

        return new Databases(databasesSet);
    }
}
