package com.autonomy.find.services.aci;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.autonomy.aci.client.services.AciConstants;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.identifier.reference.Reference;
import com.autonomy.find.config.AgentConfig;
import com.autonomy.find.dto.AgentOptions;
import com.autonomy.find.services.ParametricService;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldTextDetail;
import com.autonomy.find.util.FieldTextUtil;

/**
 * Provides utility methods for generating aci parameters related to agent actions.
 */
public class AgentAciParameters {

    private static final ObjectMapper mapper                    = new ObjectMapper();

    private static final String ACTION_GET_AGENTS               = "UserReadAgentList";
    private static final String ACTION_AGENT_ADD                = "AgentAdd";
    private static final String ACTION_AGENT_DELETE             = "AgentDelete";
    private static final String ACTION_AGENT_RENAME             = "AgentEdit";
    private static final String ACTION_AGENT_EDIT               = "AgentEdit";
    private static final String ACTION_AGENT_RESULTS            = "AgentGetResults";

    private static final String ACTION_GET_UNREAD               = "AgentGetResults";
    private static final String ACTION_MARK_AS_READ             = "AgentGetResults";
    private static final String ACTION_GET_DOCUMENTS            = "Query";

    private static final String PARAM_USERNAME                  = "username";

    private static final String PARAM_AGENT_AID                 = "aid";
    private static final String PARAM_AGENT_NAME                = "agentname";
    private static final String PARAM_AGENT_NEW_NAME            = "newagentname";
    private static final String PARAM_AGENT_CONCEPTS            = "fieldfind_concepts";
    private static final String PARAM_AGENT_TRAINING            = "training";
    private static final String PARAM_AGENT_DOCUMENTS           = "positivedocs";
    private static final String PARAM_AGENT_DATABASEMATCH       = "dredatabasematch";
    private static final String PARAM_AGENT_DATABASES_FIELD     = "fieldfind_databases";
    private static final String PARAM_AGENT_STARTDATE           = "DREMinDate";
    private static final String PARAM_AGENT_STARTDATE_FIELD     = "fieldfind_startdate";
	private static final String PARAM_AGENT_UNREAD_ONLY         = "fieldfind_unreadonly";
	private static final String PARAM_AGENT_FILTERS             = "fieldfind_filters";
	private static final String PARAM_AGENT_MINSCORE            = "fieldfind_minscore";
	private static final String PARAM_AGENT_DISPCHARS           = "fieldfind_dispchars";
    private static final String PARAM_AGENT_CATEGORY_ID         = "fieldfind_category_id";
    private static final String PARAM_AGENT_FIELDTEXTRESTRICTION= "FieldTextRestriction";
    private static final String PARAM_AGENT_CLEAR_TRAINING      = "clearTraining";

    private static final String PARAM_MODE                      = "mode";
    private static final String VALUE_REFERENCE                 = "reference";
    private static final String PARAM_MAX_RESULTS               = "DREMaxResults";
    private static final String PARAM_MIN_SCORE                 = "DREMinScore";
    private static final String PARAM_DRE_SORT                  = "DRESort";
    private static final String VALUE_BY_DATE                   = "Date";
    private static final String PARAM_PRINT_XMLMETA             = "DREXmlMeta";
    private static final String PARAM_OFFSET                    = "DREStart";
    private static final String PARAM_DISPCHARS                 = "DRECharacters";
    private static final String PARAM_EXCLUDE_READ              = "ExcludeReadDocuments";
    private static final String PARAM_PRINT                     = "DREprint";
    private static final String VALUE_FIELDS                    = "fields";
    private static final String PARAM_PRINT_FIELDS              = "DREprintFields";
    private static final String VALUE_AUTN_DATE                 = "autn:date";
    private static final String VALUE_NO_RESULTS                = "NoResults";
    private static final String PARAM_ADD_AS_READ               = "AddSetToReadDocuments";
    private static final String PARAM_DRE_MATCH_REFERENCE       = "DREMatchReference";
    private static final String PARAM_TEXT                      = "text";
    private static final String VALUE_STAR                      = "*";
    private static final String PARAM_MATCH_REFERENCE           = "MatchReference";
    private static final String PARAM_SUMMARY                   = "summary";
    private static final String VALUE_QUICK                     = "quick";
    private static final String PARAM_COMBINE                   = "combine";
    private static final String VALUE_SIMPLE                    = "simple";
	private static final String PARAM_AGENTFIELDTEXT            = "AgentFieldText";


    /**
     * Builds a JSON string interpretation of an Object.
     * @param data
     * @return
     */
    private static String toJSON(
            final Object data
    ) {
        try { return mapper.writeValueAsString(data); }
        catch (final IOException e) { return ""; }
    }

    /**
     * Defines the aci parameters for retrieving agent data for a user.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @return - The parameters
     */
    public static AciParameters parametersForGetAgents(
            final AciParameters params,
            final String username
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_GET_AGENTS);

        params.add(PARAM_USERNAME, username);

        return params;
    }

    /**
     * Defines the aci parameters for retrieving agent detail.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @param options - Details for identifying the agent
     * @return - The parameters
     */
    public static AciParameters parametersForAgentDetail(
            final AciParameters params,
            final String username,
            final AgentOptions options
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_GET_AGENTS);

        params.add(PARAM_USERNAME, username);

        if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
        else { params.add(PARAM_AGENT_AID, options.getAid()); }

        return params;
    }

    /**
     * Defines the aci parameters for creating an agent for a user.
     *
     *
	 * @param params - Parameters to append to
	 * @param username - The user's name
	 * @param options - Agent options
	 * @param parametricService
	 * @return - The Parameters
     */
    public static AciParameters parametersForCreate(
			final AciParameters params,
			final String username,
			final AgentOptions options,
			final ParametricService parametricService) {
        params.add(AciConstants.PARAM_ACTION, ACTION_AGENT_ADD);

        params.add(PARAM_USERNAME, username);
        params.add(PARAM_AGENT_NAME, options.getName());
        params.put(PARAM_MODE, VALUE_REFERENCE);

        params.add(PARAM_AGENT_TRAINING, options.getTraining());
        params.add(PARAM_AGENT_CONCEPTS, toJSON(options.getConcepts()));

		if (options.getFilters() != null) {
			final Map<String, FieldTextDetail> fieldNames = null; // parametricService.getParaFieldNames(null);
			params.add(PARAM_AGENT_FILTERS, toJSON(options.getFilters()));
			params.add(PARAM_AGENT_FIELDTEXTRESTRICTION, FieldTextUtil.buildFilterExpression(options.getFilters(), fieldNames));
		}

		params.add(PARAM_AGENT_UNREAD_ONLY, options.isUnreadOnly());
		params.add(PARAM_AGENT_DISPCHARS, options.getDispChars());
		params.add(PARAM_AGENT_MINSCORE, options.getMinScore());

        if (options.hasDocuments()) {
            params.add(PARAM_AGENT_DOCUMENTS, CollUtils.intersperse("+", CollUtils.mapReferencePlus(options.getDocuments())));
        }
        if (options.hasDatabases()) {
            params.add(PARAM_AGENT_DATABASES_FIELD, toJSON(options.getDatabases()));
        }
        if (options.hasStartDate()) {
            params.add(PARAM_AGENT_STARTDATE_FIELD, options.getStartDate());
        }
        if (options.hasCategoryId()) {
            params.add(PARAM_AGENT_CATEGORY_ID, options.getCategoryId());
        }

        return params;
    }

    /**
     * Defines the aci parameters for deleting an agent, for a specific user.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @param options - Agent options
     * @return - The Parameters
     */
    public static AciParameters parametersForDelete(
            final AciParameters params,
            final String username,
            final AgentOptions options
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_AGENT_DELETE);

        params.add(PARAM_USERNAME, username);

        if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
        else { params.add(PARAM_AGENT_AID, options.getAid()); }

        return params;
    }

    /**
     * Defines the aci parameters for renaming an agent, for a specific user.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @param options - The agent's options
     * @return - The Parameters
     */
    public static AciParameters parametersForRename(
            final AciParameters params,
            final String username,
            final AgentOptions options
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_AGENT_RENAME);

        params.add(PARAM_USERNAME, username);
        if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
        else { params.add(PARAM_AGENT_AID, options.getAid()); }
        params.add(PARAM_AGENT_NEW_NAME, options.getNewName());

        return params;
    }

    /**
     * Defines the aci parameters for editing an agent, for a specific user.
     *
     *
	 * @param params - Parameters to append to
	 * @param username - The user's name
	 * @param options - The agent's options
	 * @param removeStartDate - If the agent's start date should be removed
	 * @param parametricService
	 * @return - The Parameters
     */
    public static AciParameters parametersForEdit(
			final AciParameters params,
			final String username,
			final AgentOptions options,
			final Boolean removeStartDate,
			final ParametricService parametricService) {
        params.add(AciConstants.PARAM_ACTION, ACTION_AGENT_EDIT);

        params.add(PARAM_USERNAME, username);
		params.add(PARAM_AGENT_UNREAD_ONLY, options.isUnreadOnly());
		params.add(PARAM_AGENT_DISPCHARS, options.getDispChars());
		params.add(PARAM_AGENT_MINSCORE, options.getMinScore());

        if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
        else { params.add(PARAM_AGENT_AID, options.getAid()); }

        if (options.hasNewName()) { params.add(PARAM_AGENT_NEW_NAME, options.getNewName()); }
        if (options.hasConcepts()) {
            params.add(PARAM_AGENT_CONCEPTS, toJSON(options.getConcepts()));
            params.add(PARAM_AGENT_TRAINING, options.getTraining());
        }
		if (options.getFilters() != null) {
			final Map<String, FieldTextDetail> fieldNames = null; //parametricService.getParaFieldNames(null);
			params.add(PARAM_AGENT_FILTERS, toJSON(options.getFilters()));
			params.add(PARAM_AGENT_FIELDTEXTRESTRICTION, FieldTextUtil.buildFilterExpression(options.getFilters(), fieldNames));
		}
        if (options.getDocuments() != null) {
            /*  Can only clear training if the training is going to be updated.
             *  Clear training is required to remove from the positivedocs of an agent
             *  In the case that the concepts are not updated, no documents can be
             *  removed from the positivedocs, but they can be added. */
            if (options.hasConcepts()) { params.add(PARAM_AGENT_CLEAR_TRAINING, true); }
            params.add(PARAM_AGENT_DOCUMENTS, CollUtils.intersperse("+", CollUtils.mapReferencePlus(options.getDocuments())));
        }
        if (options.hasDatabases()) {
            params.add(PARAM_AGENT_DATABASEMATCH, options.getDatabases());
            params.add(PARAM_AGENT_DATABASES_FIELD, toJSON(options.getDatabases()));
        } else {
            params.add(PARAM_AGENT_DATABASEMATCH, "");
            params.add(PARAM_AGENT_DATABASES_FIELD, "[]");
        }

        if (options.hasCategoryId()) {
            params.add(PARAM_AGENT_CATEGORY_ID, options.getCategoryId());
        } else {
            params.add(PARAM_AGENT_CATEGORY_ID, "");
        }

        if (removeStartDate != null && removeStartDate) {
            params.add(PARAM_AGENT_STARTDATE_FIELD, ""); // Empty value should remove the field.
        } else {
            if (options.hasStartDate()) { params.add(PARAM_AGENT_STARTDATE_FIELD, options.getStartDate()); }
        }

        return params;
    }

    /**
     * Defines the aci parameters for retrieving result documents
     * for a specified agent, given a user.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @param options - The agent's options
     * @param offset - The results offset
     * @param maxResults - The maximum number of results to retrieve
     * @param minScore - The minimum score of a document for it to qualify
     * @param unreadOnly - Only list unread documents
     * @return - The Parameters
     */
    public static AciParameters parametersForResults(
            final AciParameters params,
            final String username,
            final AgentOptions options,
            final int offset,
            final Integer maxResults,
            final Integer minScore,
            final boolean unreadOnly,
            final AgentConfig config,
            final String minDate
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_AGENT_RESULTS);

		addSharedReadParams(params, username, options, minScore, config, minDate);

		params.add(PARAM_PRINT_XMLMETA, true);
		params.add(PARAM_PRINT, VALUE_FIELDS);
		params.add(PARAM_PRINT_FIELDS, VALUE_AUTN_DATE);
		params.add(PARAM_EXCLUDE_READ, unreadOnly);
		params.add(PARAM_DRE_SORT, VALUE_BY_DATE);
		params.add(PARAM_DISPCHARS, options.getDispChars());
		params.add(PARAM_OFFSET, offset + 1);
		params.add(PARAM_MAX_RESULTS, (maxResults != null ? maxResults : config.getDefaultMaxResults()) + offset);

        return params;
    }

	/**
	 * Defines the aci parameters for retrieving the unread counts
	 * of an agent for a specific user.
	 *
	 * @param params - Parameters to append to
	 * @param username - The user's name
	 * @param options - The agent's options
	 * @param maxResults - The maximum number of results to retrieve
	 * @param minScore - The minimum score of a document for it to qualify
	 * @param config - the config to use to fetch defaults
	 * @return
	 */
    public static AciParameters parametersForGetUnreadCount(
            final AciParameters params,
            final String username,
            final AgentOptions options,
            final Integer maxResults,
            final Integer minScore,
            final AgentConfig config
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_GET_UNREAD);

		addSharedReadParams(params, username, options, minScore, config, null);

		params.add(PARAM_PRINT, VALUE_NO_RESULTS);
		params.add(PARAM_EXCLUDE_READ, true);
		params.add(PARAM_MAX_RESULTS, maxResults != null ? maxResults : config.getDefaultMaxResults());

        return params;
    }

	private static void addSharedReadParams(
			final AciParameters params,
			final String username,
			final AgentOptions options,
			final Integer minScore,
			final AgentConfig config,
			final String minDate
	) {
		params.add(PARAM_AGENTFIELDTEXT, true);
		params.add(PARAM_USERNAME, username);
		if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
		else { params.add(PARAM_AGENT_AID, options.getAid()); }

		final Double agentMinScore = options.getMinScore();

		params.add(PARAM_MIN_SCORE,
                minScore != null ? minScore
				: agentMinScore != null ? agentMinScore
				: config.getDefaultMinScore());

		if (options.hasDatabases()) {
			params.add(PARAM_AGENT_DATABASEMATCH, CollUtils.intersperse("+", options.getDatabases()));
		}

		if (options.hasStartDate()) {
			params.add(PARAM_AGENT_STARTDATE, minDate != null ? minDate : options.getStartDate());
		}
	}

	/**
     * Defines the aci parameters for marking a single document as read
     * for a specific agent, given a user.
     *
     * @param params - Parameters to append to
     * @param username - The user's name
     * @param options - The agent's options
     * @param document - The document reference to makr as read
     * @return - The Parameters
     */
    public static AciParameters parametersForMarkAsRead(
            final AciParameters params,
            final String username,
            final AgentOptions options,
            final String document
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_MARK_AS_READ);

        params.add(PARAM_USERNAME, username);
        if (options.hasName()) { params.add(PARAM_AGENT_NAME, options.getName()); }
        else { params.add(PARAM_AGENT_AID, options.getAid()); }
        params.add(PARAM_ADD_AS_READ, true);
        params.add(PARAM_DRE_MATCH_REFERENCE, new Reference(document));

        return params;
    }

    /**
     * Defines the aci parameters for retrieving document data given
     * a list of document references.
     *
     * I.e: map documentFromReference references
     *
     * @param params - Parameters to append to
     * @param references - A list of document references
     * @return - The Parameters
     */
    public static AciParameters parametersForDocumentsFromReferences(
            final AciParameters params,
            final List<String> references
    ) {
        params.add(AciConstants.PARAM_ACTION, ACTION_GET_DOCUMENTS);
        params.add(PARAM_TEXT, VALUE_STAR);

        //  Merge results from the same source
        params.add(PARAM_COMBINE, VALUE_SIMPLE);
        //  Get enough for 1 per reference
        params.add(PARAM_MAX_RESULTS, references.size());
        //  For the references provided
        params.add(PARAM_MATCH_REFERENCE, CollUtils.intersperse("+", CollUtils.mapReferencePlus(references)));
        //  Get a quick summary
        params.add(PARAM_SUMMARY, VALUE_QUICK);

        return params;
    }
}
