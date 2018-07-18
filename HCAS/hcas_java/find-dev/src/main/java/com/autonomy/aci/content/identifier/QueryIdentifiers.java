/*
 * $Id:$
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author:$ on $Date:$
 */
package com.autonomy.aci.content.identifier;

/**
 * {@code QueryIdentifiers} are document identifiers that can be used to filter a <tt>query</tt> action using a 'match'
 * parameter, such as <tt>matchreference</tt> or <tt>statematchid</tt>. e.g.:
 *
 * <pre>
 *    QueryIdentifiers identifiers = ...;
 *
 *    AciParameters parameters = new AciParameters("query");
 *    parameters.add("text", "*");
 *    parameters.add(identifiers.getMatchParameterName(), identifiers);
 * </pre>
 */
public interface QueryIdentifiers extends IndexingIdentifiers {
    String DATABASE_MATCH = "databasematch";
    String MATCH_ID = "matchid";
    String MATCH_REFERENCE = "matchreference";
    String STATE_MATCH_ID = "statematchid";

    /**
     * The name of the 'match' parameter to use for this set of identifiers, such as <tt>matchreference</tt>.
     *
     * @return The <tt>query</tt> 'match' parameter name
     */
    String getMatchParameterName();

    /**
     * The string representation of this identifier, suitable for use as the value of a 'match' parameter on a
     * <tt>query</tt> action.
     *
     * @return The <tt>query</tt> 'match' parameter value
     */
    @Override
    String toString();
}