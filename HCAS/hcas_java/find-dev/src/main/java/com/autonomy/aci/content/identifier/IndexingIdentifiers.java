/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/IndexingIdentifiers.java#4 $
 *
 * Copyright (c) 2009-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */
package com.autonomy.aci.content.identifier;

/**
 * {@code IndexingIdentifiers} are document identifiers that can be used in the data of a <tt>DREREPLACE</tt>. e.g.:
 *
 * <pre>
 *    IndexingIdentifiers identifiers = ...;
 *
 *    // Build up the first line of the replace body
 *    StringBuilder dreReplace = new StringBuilder()
 *            .append('#')
 *            .append(identifiers.getIndexingIdentifierName())
 *            .append(' ')
 *            .append(identifiers.toIndexingString())
 *            .append('\n');
 * </pre>
 */
public interface IndexingIdentifiers {

    String DREALL = "DREALL";
    String DREDOCID = "DREDOCID";
    String DREDBNAME = "DREDBNAME";
    String DREDOCREF = "DREDOCREF";
    String DRESTATEID = "DRESTATEID";

    /**
     * {@code IndexingIdentifier} to represent <tt>DREALL</tt>, which can be reused to replace fields on all the
     * documents in an engine. This might be used when purging a particular field-value pair from an entire engine
     * without the need to explicitly find which documents have that value.
     *
     * <p>This object is immutable.
     */
    IndexingIdentifiers ALL = new IndexingIdentifiers() {
        @Override
        public String toIndexingString() {
            return "";
        }

        @Override
        public String getIndexingIdentifierName() {
            return DREALL;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    };

    /**
     * Whether or not the identifier is empty.
     *
     * <p>An empty identifier is one that cannot correspond to any documents, e.g. an empty reference list. Under some
     * circumstances it can be unsafe to use an empty identifier as it could match all documents, rather than no
     * documents. Though the classes in this API attempt to cope with this contingency as smoothly as possible (using
     * dummy values and throwing exceptions as appropriate), it is usually preferable to do an empty check prior to
     * using an identifier and handling that case separately.
     *
     * @return {@code true} if the identifier is empty
     */
    boolean isEmpty();

    /**
     * The string representation of this identifier, suitable for use as part of a <tt>DREREPLACE</tt>.
     *
     * <p> Though the {@code IndexingIdentifiers} interface does not prescribe an implementation of the
     * {@link Object#toString()} method, in practice it usually gives an ACI-ready version of the identifier. In most
     * cases this will be the same as the indexing version but care should be taken to use the correct method as
     * differences between the strings can cause subtly incorrect behaviour.
     *
     * @return A <tt>DREREPLACE</tt> identifier
     */
    String toIndexingString();

    /**
     * The identifier name, as used in <tt>DREREPLACE</tt>. It will not include the leading #. e.g. <tt>DREDOCREF</tt>.
     *
     * @return A <tt>DREREPLACE</tt> identifier name
     */
    String getIndexingIdentifierName();
}