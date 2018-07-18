package com.autonomy.find.api.controllers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.find.api.database.UserSession;
import com.autonomy.find.api.exceptions.UserSessionDoesNotExistException;
import com.autonomy.find.api.response.ResponseWithItems;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.config.FindNewsConfig;
import com.autonomy.find.dto.AgentOptions;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.factories.AgentFactory;
import com.autonomy.find.services.AgentOptionsService;
import com.autonomy.find.services.HistoryService;
import com.autonomy.find.services.UserPropertiesService;
import com.autonomy.find.services.UserService;

/**
 * User: liam.goodacre
 * Date: 13/12/12
 * Time: 12:13
 */
@Controller
@RequestMapping("/api/user/agent")
public class UserAgentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsController.class);
    public static final String AGENT_NOT_CREATED_ERROR = "Agent could not be created.";
    public static final String AGENT_NOT_DELETED_ERROR = "Agent could not be deleted.";
    public static final String AGENT_NOT_READ_ERROR = "Agent could not be read.";
    public static final String AGENT_NOT_RENAMED_ERROR = "Agent could not be renamed.";
    public static final String USER_SESSION_NOT_FOUND_ERROR = "User session could not be found.";

    @Autowired
    private UserService userService;

    @Autowired
    private UserPropertiesService propertiesService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private AgentOptionsService agentService;

    @Autowired
    @Qualifier("findNewsConfig")
    private FindNewsConfig findNewsConfig;


    /**
     * Creates an agent for a user, given some training concepts.
     *
     * @param session   - The user's current session
     * @param agentName - Name of the agent to create
     * @param training  - List of training concepts
     * @return ResponseWithSuccessError
     */
    @RequestMapping("createSearchAgent.json")
    @ResponseBody
    public ResponseWithSuccessError createSearchAgent(
            @RequestParam(value = "session", required = false) final String session,
			@RequestParam(value = "unreadOnly", defaultValue = "true") final boolean unreadOnly,
            @RequestParam(value = "agentName", required = false) final String agentName,
            @RequestParam(value = "training[]", required = false) final List<String> training) {

        try {
            final UserSession userSession = userService.getAndRefreshUserSession(session);
            final String username = userSession.getUserData().getId().toString();
            final AgentOptions options = AgentFactory.agentForCreate(unreadOnly, agentName, training, null, findNewsConfig.getAgentDatabasesList(), null, null, null, null, null);
            final boolean status = agentService.createAgent(username, options);
            final String error = status ? null : AGENT_NOT_CREATED_ERROR;
            if (!status) {
                LOGGER.error(String.format("Agent with name `%s` could not be created for community user with username `%s`", agentName, username));
            }
            return new ResponseWithSuccessError(status, error);
        } catch (final UserSessionDoesNotExistException e) {
            return new ResponseWithSuccessError(false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }


    /**
     * Creates an agent for a user, given some training concepts.
     *
     * @param session   - The user's current session
     * @param agentName - Name of the agent to create
     * @param trainingTerms  - List of training concepts
     * @return ResponseWithSuccessError
     */
    @RequestMapping("createSearchAgentByAND.json")
    @ResponseBody
    public ResponseWithSuccessError createSearchAgentByAND(
            @RequestParam(value = "session", required = false) final String session,
			@RequestParam(value = "unreadOnly", defaultValue = "true") final boolean unreadOnly,
            @RequestParam(value = "agentName", required = false) final String agentName,
            @RequestParam(value = "training", required = false) final String trainingTerms) {

        return createSearchAgent(session, unreadOnly, agentName, Arrays.asList(trainingTerms.split("\\s*AND\\s*")));
    }



    /**
     * Deletes an agent.
     *
     * @param session   - The user's current session
     * @param agentName - Name of the agent to delete
     * @return ResponseWithSuccessError
     */
    @RequestMapping("deleteSearchAgent.json")
    @ResponseBody
    public ResponseWithSuccessError deleteSearchAgent(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "agentName", required = false) final String agentName) {

        try {
            //  Retrieve and refresh the user's session if possible
            final UserSession userSession = userService.getAndRefreshUserSession(session);
            //  Get the user's community username
            final String username = userSession.getUserData().getId().toString();
            final AgentOptions options = AgentFactory.agentByAidOrName(null, agentName);
            final boolean status = agentService.deleteAgent(username, options);
            final String error = status ? null : AGENT_NOT_DELETED_ERROR;
            if (!status) {
                LOGGER.error(String.format("Agent with name `%s` could not be deleted for community user with username `%s`", agentName, username));
            }
            return new ResponseWithSuccessError(status, error);
        } catch (final UserSessionDoesNotExistException e) {
            return new ResponseWithSuccessError(false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }

    /**
     * Retrieves a result document list for an agent.
     * The agent is found given the pair: ( username , agentName ).
     *
     * @param session              - The user's current session
     * @param agentName            - Name of the agent to retrieve the results from
     * @param offset               - The result offset
     * @param maxResults           - The max number of results to get
     * @param excludeReadDocuments - Should read documents be excluded
     * @param trailingEnd          - Format the summary with a trailing end
     * @param tag                  - Add and calculate the tagDetails
     * @return ResponseWithItems<ResultDocument> or ResponseWithSuccessError
     */
    @RequestMapping("getSearchAgentResults.json")
    @ResponseBody
    public ResponseWithSuccessError getSearchAgentResults(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "agentName", required = false) final String agentName,
            @RequestParam(value = "offset", required = false, defaultValue = "0") final int offset,
            @RequestParam(value = "maxResults", required = false, defaultValue = "20") final int maxResults,
            @RequestParam(value = "excludeReadDocuments", required = false, defaultValue = "false") final boolean excludeReadDocuments,
            @RequestParam(value = "trailingEnd", required = false, defaultValue = "false") final boolean trailingEnd,
            @RequestParam(value = "tag", required = false, defaultValue = "false") final boolean tag,
            @RequestParam(value = "minDate", required = false, defaultValue = "01/01/2013") final String minDate){

        try {
            //  Retrieve and refresh the user's session if possible
            final UserSession userSession = userService.getAndRefreshUserSession(session);
            //  Get the user's community username (the user's id)
            final String username = userSession.getUserData().getId().toString();
            final AgentOptions options = AgentFactory.agentByAidOrName(null, agentName);
            options.setDatabases(findNewsConfig.getAgentDatabasesList());

            //  Attempt to get the results
            final List<ResultDocument> results = agentService.getResultsWithoutLookup(username, options, offset, maxResults, null, excludeReadDocuments, minDate);

            //  If no results were received
            if (results.isEmpty()) {
                //  Log the incident
                LOGGER.error(String.format("Agent could not be read for community user with username %s", username));
                //  Inform the client that the agent wasn't read properly
                return new ResponseWithSuccessError(false, AGENT_NOT_READ_ERROR);
            }

            //  Process the result documents
            //      If the client requested trailingEnd or tag.
            ResultDocument.process(results, trailingEnd, false, tag);
            return new ResponseWithItems<ResultDocument>(results, true, null);

        } catch (final UserSessionDoesNotExistException e) {

            return new ResponseWithSuccessError(false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }


    /**
     * Renames a given agent
     *
     * @param session
     * @param agentName
     * @param newName
     * @return
     */
    @RequestMapping("renameSearchAgent.json")
    @ResponseBody
    public ResponseWithSuccessError renameSearchAgent(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "agentName", required = false) final String agentName,
            @RequestParam(value = "newName", required = false) final String newName
    ) {

        try {
            //  Retrieve and refresh the user's session if possible
            final UserSession userSession = userService.getAndRefreshUserSession(session);
            //  Get the user's community username (the user's id)
            final String username = userSession.getUserData().getId().toString();
            //  Attempt to get the results
            final AgentOptions options = AgentFactory.agentForRename(null, agentName, newName);
            final boolean result = agentService.renameAgent(username, options);

            //  If the renamed failed
            if (!result) {
                //  Log the incident
                LOGGER.error(String.format("Agent could not be renamed for community user with username %s", username));
                //  Inform the client that the agent wasn't read properly
                return new ResponseWithSuccessError(false, AGENT_NOT_RENAMED_ERROR);
            }

            return new ResponseWithSuccessError(true, null);
        }
        catch (final UserSessionDoesNotExistException e) {

            return new ResponseWithSuccessError(false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }

    /**
     *
     * @param session
     * @return
     */
    @RequestMapping("getSearchAgentNames.json")
    public @ResponseBody ResponseWithSuccessError getSearchAgentNames(
        @RequestParam(value = "session", required = false) final String session
    ) {
        try {
            //  Retrieve and refresh the user's session if possible
            final UserSession userSession = userService.getAndRefreshUserSession(session);
            //  Get the user's community username (the user's id)
            final String username = userSession.getUserData().getId().toString();
            //  Attempt to get the list of agents
            final List<AgentOptions> agents = agentService.getAgents(username);

            final List<String> names = new LinkedList<String>();
            for (final AgentOptions agent : agents) {
                names.add(agent.getName());
            }

            return new ResponseWithItems<String>(names, true, null);
        }
        catch (final Exception e) {

            return new ResponseWithSuccessError(false, null);
        }
    }
}
