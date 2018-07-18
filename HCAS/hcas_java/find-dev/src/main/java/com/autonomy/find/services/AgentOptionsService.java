package com.autonomy.find.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autonomy.aci.actions.DontCareAsLongAsItsNotAnErrorProcessor;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.AgentConfig;
import com.autonomy.find.dto.AgentOptions;
import com.autonomy.find.dto.AgentUnread;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.services.aci.AgentAciParameters;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import com.googlecode.ehcache.annotations.KeyGenerator;
import com.googlecode.ehcache.annotations.PartialCacheKey;
import com.googlecode.ehcache.annotations.Property;
import com.googlecode.ehcache.annotations.TriggersRemove;

/**
 * Provides aci service actions specific to agents.
 */
@Service
public class AgentOptionsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentOptionsService.class);

    private static final String ACTION_MARK_UNREAD = "AgentGetResults";
    private static final String ACTION_GET_DOCUMENTS = "Query";

    @Autowired
    private SearchService searchService;
    @Autowired
    private AciService searchAciService;
    @Autowired
    @Qualifier("communityAciService")
    private AciService communityAci;
    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;
    @Autowired
    @Qualifier("agentConfig")
    private AgentConfig agentConfig;
	@Autowired
	private ParametricService parametricService;
    @Autowired
    private CategoryTrainingService trainingService;

    /**
     * Retrieves all the agent details for a user.
     *
     * @param username - The user's username
     * @return - A list of the user's agents
     */
	@Cacheable(cacheName = "AgentOptionsService.agents", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE,
		keyGenerator = @KeyGenerator (name = "HashCodeCacheKeyGenerator", properties = @Property( name="includeMethod", value="false")))
    public List<AgentOptions> getAgents(
			@PartialCacheKey final String username
    ) {
        return executeWithParamsForListOrEmpty(communityAci,
                AgentAciParameters.parametersForGetAgents(
                        new AciParameters(), username),
                AgentOptions.class);
    }


    /**
     * Retrieves details for an agent.
     *
     * @param username - The user's username
     * @param options - Agent options with either aid or name
     * @return Agent options
     */
    public AgentOptions getAgentDetail(
            final String username,
            final AgentOptions options
    ) {
        final List<AgentOptions> results = executeWithParamsForListOrEmpty(communityAci,
                AgentAciParameters.parametersForAgentDetail(
                        new AciParameters(), username, options),
                AgentOptions.class);
        return ( results.isEmpty()
                ? options
                : results.get(0) );
    }


    /**
     * Creates an agent for a user.
     *
     * @param username - The user's username
     * @param options  - The agent creation options
     * @return - Boolean indicating success of operation
     */
	@TriggersRemove(cacheName = "AgentOptionsService.agents",
		keyGenerator = @KeyGenerator (name = "HashCodeCacheKeyGenerator", properties = @Property( name="includeMethod", value="false")))
    public boolean createAgent(
			@PartialCacheKey final String username,
            final AgentOptions options
    ) {
        if (options.hasCategoryId()) {
            options.addQueryExtension(trainingService.getCategoryQueryExtension(options.getCategoryId()));
        }
        return executeWithParamsForBoolean(communityAci,
                AgentAciParameters.parametersForCreate(
                        new AciParameters(), username, options, parametricService));
    }


    /**
     * Deletes a user's agent.
     *
     * @param username - The user's username
     * @param options  - The agent deletion options
     * @return - Boolean indicating success of operation
     */
	@TriggersRemove(cacheName = "AgentOptionsService.agents",
		keyGenerator = @KeyGenerator (name = "HashCodeCacheKeyGenerator", properties = @Property( name="includeMethod", value="false")))
    public boolean deleteAgent(
			@PartialCacheKey final String username,
            final AgentOptions options
    ) {
        return executeWithParamsForBoolean(communityAci,
                AgentAciParameters.parametersForDelete(
                        new AciParameters(), username, options));
    }


    /**
     * Renames a user's agent.
     *
     * @param username - The user's username
     * @param options  - The agent rename options
     * @return - Boolean indicating success of operation
     */
	@TriggersRemove(cacheName = "AgentOptionsService.agents",
		keyGenerator = @KeyGenerator (name = "HashCodeCacheKeyGenerator", properties = @Property( name="includeMethod", value="false")))
    public boolean renameAgent(
			@PartialCacheKey final String username,
            final AgentOptions options
    ) {
        return executeWithParamsForBoolean(communityAci,
                AgentAciParameters.parametersForRename(
                        new AciParameters(), username, options));
    }


    /**
     * Edits a user's agent.
     *
     * @param username - The user's username
     * @param options  - The agent editing options
     * @param removeStartDate - If the start date should be removed
     * @return - Boolean indicating success of operation
     */
	@TriggersRemove(cacheName = "AgentOptionsService.agents",
		keyGenerator = @KeyGenerator (name = "HashCodeCacheKeyGenerator", properties = @Property( name="includeMethod", value="false")))
    public boolean editAgent(
            @PartialCacheKey final String username,
            final AgentOptions options,
            final Boolean removeStartDate
    ) {
        if (options.hasCategoryId()) {
            options.addQueryExtension(trainingService.getCategoryQueryExtension(options.getCategoryId()));
        }
        return executeWithParamsForBoolean(communityAci,
                AgentAciParameters.parametersForEdit(
                        new AciParameters(), username, options, removeStartDate, parametricService));
    }


    /**
     * Retrieves agent results for a specified user's agent.
     *
     * @param username   - The user's username
     * @param options    - The agent options
     * @param offset     - How many to offset the results by
     * @param maxResults - The maximum number of results to return
     * @param unreadOnly - Filter the results by only those that are unread
     * @return - List of the agent's results
     */
    public List<ResultDocument> getResults(
            final String username,
            final AgentOptions options,
            final int offset,
            final Integer maxResults,
            final Integer minScore,
            final boolean unreadOnly
    ) {

        final AgentOptions detailedOptions = getAgentDetail(username, options);

        return getResultsWithoutLookup(username, detailedOptions, offset, maxResults, minScore, unreadOnly, null);
    }


    /**
     * Retrieves agent results for a specified user's agent.
     *
     * @param username   - The user's username
     * @param options    - The agent options
     * @param offset     - How many to offset the results by
     * @param maxResults - The maximum number of results to return
     * @param unreadOnly - Filter the results by only those that are unread
     * @return - List of the agent's results
     */
    public List<ResultDocument> getResultsWithoutLookup(
            final String username,
            final AgentOptions options,
            final int offset,
            final Integer maxResults,
            final Integer minScore,
            final boolean unreadOnly,
            final String minDate
    ) {
        return executeWithParamsForListOrEmpty(communityAci,
                AgentAciParameters.parametersForResults(
                        new AciParameters(), username, options,
                        offset, maxResults, minScore, unreadOnly,
                        agentConfig, minDate),
                ResultDocument.class);
    }


    /**
     * Retrieves a list of the document unread counts for all agents
     * owned by specific user.
     *
     * @param username   - The user's username
     * @param maxResults - The maximum number of results to return
     * @param minScore   - The minimum score of a result to be listed
     * @return - List of unread counts for all the agents of a user
     */
    public List<AgentUnread> getUnreadCount(
			final List<AgentOptions> agents,
			final String username,
            final Integer maxResults,
            final Integer minScore
    ) {
		final List<AgentUnread> counts = new ArrayList<>(agents.size());

		try {
			for (final AgentOptions agent : agents) {
				counts.addAll(communityAci.executeAction(AgentAciParameters.parametersForGetUnreadCount(
						new AciParameters(),
						username,
						agent,
						maxResults,
						minScore,
						agentConfig
				), processorFactory.listProcessorForClass(AgentUnread.class)));
			}
		} catch (final AciErrorException aci) {
			LOGGER.error("ACI error when trying fetch agent counts", aci);
		} catch (final ProcessorException pe) {
			LOGGER.error("Processor exception when trying to fetch agent counts", pe);
		}

		return counts;
    }


    /**
     * Marks a document as read for a given user's agent.
     *
     * @param username - The user's username
     * @param options  - The agent's options
     * @param document - The document to mark as read
     * @return - Boolean indicating success of operation
     */
    public boolean markAsRead(
            final String username,
            final AgentOptions options,
            final String document
    ) {
        return executeWithParamsForBoolean(communityAci,
                AgentAciParameters.parametersForMarkAsRead(
                        new AciParameters(ACTION_MARK_UNREAD), username, options, document));
    }


    /**
     * Retrieves a list of documents related to a list of document references.
     *
     * @param references - List of references to lookup
     * @return - List of documents related to the reference supplied
     */
    public List<ResultDocument> getDocumentsFromReferences(
            final List<String> references
    ) {
        return executeWithParamsForListOrEmpty(searchAciService,
                AgentAciParameters.parametersForDocumentsFromReferences(
                        new AciParameters(ACTION_GET_DOCUMENTS),
                        references),
                ResultDocument.class);
    }


    /**
     * Retrieves concepts related to some training text.
     *
     * @param training - Training text
     * @return - List of concepts
     */
    public List<String> getRelatedConcepts(
            final String training
    ) {
        return searchService.getRelatedConcepts(training);
    }


    /**
     * Retrieves documents related to some training text.
     *
     * @param training - Training text
     * @return - List of documents
     */
    public List<ResultDocument> getRelatedDocuments(
            final String training
    ) {
        return "".equals(training) ? new LinkedList<ResultDocument>() : searchService.searchResults(training);
    }


    /**
     * Utility method for executing an aci action, detecting if it succeeded.
     *
     * @param service - The aci service to execute the action on
     * @param params  - The aci parameters related to the acion
     * @return - Boolean indicating success of operation
     */
    private static boolean executeWithParamsForBoolean(
            final AciService service,
            final AciParameters params
    ) {
        try {
            return service.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Utility method for retrieving a list of elements for some aci execution.
     *
     * @param service - The aci service to execute the action on
     * @param params  - The aci parameters related to the action
     * @param clazz   - The class of objects to build
     * @param <A>     - The type of objects to retrieve
     * @return - A potentially empty list of elements of the appropriate type
     */
    @SuppressWarnings("unchecked")
    private <A> List<A> executeWithParamsForListOrEmpty(
            final AciService service,
            final AciParameters params,
            final Class clazz
    ) {
        try {
            return (List<A>) service.executeAction(params,
                    processorFactory.listProcessorForClass(clazz));
        } catch (final Exception e) {
            return new LinkedList<A>();
        }
    }
}
