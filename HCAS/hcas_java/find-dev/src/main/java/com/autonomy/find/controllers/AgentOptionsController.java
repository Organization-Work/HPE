package com.autonomy.find.controllers;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.find.config.AgentConfig;
import com.autonomy.find.dto.AgentOptions;
import com.autonomy.find.dto.AgentOptionsRequest;
import com.autonomy.find.dto.AgentUnread;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.factories.AgentFactory;
import com.autonomy.find.services.AgentOptionsService;
import com.autonomy.find.util.SessionHelper;

@Controller
@RequestMapping("/p/ajax/agents")
public class AgentOptionsController {

    @Autowired
    @Qualifier("agentOptionsService")
    private AgentOptionsService agentService;

    @Autowired
    @Qualifier("agentConfig")
    private AgentConfig agentConfig;

    @Autowired
    private SessionHelper sessionHelper;



    /**
     * Retrieves a list of all the agents associated with the current user.
     *
     * @param session - The current user session
     * @return - List of user's agents
     */
    @RequestMapping("/getAgents.json")
    @ResponseBody
    public List<AgentOptions> getAgents(
        final HttpSession session
    ) {
        final String username = sessionHelper.getRemoteUser(session);
        return agentService.getAgents(username);
    }


    /**
     * Retrieves details for an agent.
     *
     * @param session - The current user session
     * @param aid - The agent's aid
     * @param name - The agent's name
     * @return - Agent options
     */
    @RequestMapping("/getAgentDetail.json")
    @ResponseBody
    public AgentOptions getAgentDetail(
            final HttpSession session,
            @RequestParam(value = "aid", required = false) final String aid,
            @RequestParam(value = "name", required = false) final String name
    ) {
        final String username = sessionHelper.getRemoteUser(session);
        return agentService.getAgentDetail(username,
                AgentFactory.agentByAidOrName(aid, name));
    }


    /**
     * Creates a new agent for the current user.
     *
     * @param session - The current user session
	 * @param req - The agent config
     * @return - Status of success
     */
    @RequestMapping("/createAgent.json")
    @ResponseBody
    public boolean createAgent(
            final HttpSession session,
			@RequestBody final AgentOptionsRequest req
    ) {
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentForCreate(req.isUnreadOnly(), req.getName(),
                req.getConcepts(), req.getDocuments(), req.getDatabases(), req.getStartDate(),
                req.getFilters(), req.getMinScore(), req.getDispChars(), req.getCategoryId());
        return agentService.createAgent(username, options);
    }


    /**
     * Deletes a user's agent by either aid or name.
     *
     * *1 - One of these optional fields must be non-null.
     *
     * @param session - The current user session
     * @param aid - The aid of the agent to delete - optional *1
     * @param name - The name of the agent to delete - optional *1
     * @return - Status of success
     * @throws IllegalArgumentException
     */
    @RequestMapping("/deleteAgent.json")
    @ResponseBody
    public boolean deleteAgent(
            final HttpSession session,
            @RequestParam(value = "aid", required = false) final String aid,
            @RequestParam(value = "name", required = false) final String name
    ) throws IllegalArgumentException {
        if (aid == null && name == null) {
            throw new IllegalArgumentException("At least one of {aid, name} should be set.");
        }
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentByAidOrName(aid, name);
        return agentService.deleteAgent(username, options);
    }


    /**
     * Renames an agent for a user.
     *
     * *1 - One of these optional fields must be non-null.
     *
     * @param session - The current user session
     * @param aid - The agent's aid - optional *1
     * @param name - The agent's name - optional *1
     * @param newName - The name to change the agent to
     * @return - Status of success
     * @throws IllegalArgumentException
     */
    @RequestMapping("/renameAgent.json")
    @ResponseBody
    public boolean renameAgent(
            final HttpSession session,
            @RequestParam(value = "aid", required = false) final String aid,
            @RequestParam(value = "name", required = false) final String name,
            @RequestParam(value = "newName", required = true) final String newName
    ) throws IllegalArgumentException {
        if (aid == null && name == null) {
            throw new IllegalArgumentException("At least one of {aid, name} should be set.");
        }
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentForRename(aid, name, newName);
        return agentService.renameAgent(username, options);
    }


    /**
     * Edits an agent.
     *
     * *1 - One of these optional fields must be non-null.
     *
     * @param session - The current user session
     * @param req - The request data
     * @return
     * @throws IllegalArgumentException
     */
    @RequestMapping("/editAgent.json")
    @ResponseBody
    public boolean editAgent(
            final HttpSession session,
			@RequestBody final AgentOptionsRequest req
    ) throws IllegalArgumentException {
        if (req.getAid() == null && req.getName() == null) {
            throw new IllegalArgumentException("At least one of {aid, name} should be set.");
        }
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentForEdit(req.isUnreadOnly(), req.getAid(), req.getName(),
                req.getNewName(), req.getConcepts(), req.getDocuments(), req.getDatabases(), req.getStartDate(),
                req.getFilters(), req.getMinScore(), req.getDispChars(), req.getCategoryId());
        return agentService.editAgent(username, options, req.getRemoveStartDate());
    }


    /**
     * Retrieves an agent's results.
     *
     * *1 - One of these optional fields must be non-null.
     *
     * @param session - The current user session
     * @param aid - The agent's aid - optional *1
     * @param name - The agent's name - optional *1
     * @param offset - The result offset - optional, default = 0
     * @param maxResults - The maximum number of results to retrieve - optional
     * @param minScore - The minimum score of a document for it to be included - optional
     * @param unreadOnly - Whether or not only unread documents should be included - optional, default = true
     * @return - List of result documents.
     * @throws IllegalArgumentException
     */
    @RequestMapping("/getResults.json")
    @ResponseBody
    public List<ResultDocument> getResults(
            final HttpSession session,
            @RequestParam(value = "aid", required = false) final String aid,
            @RequestParam(value = "name", required = false) final String name,
            @RequestParam(value = "offset", required = false, defaultValue = "0") final int offset,
            @RequestParam(value = "maxResults", required = false) final Integer maxResults,
            @RequestParam(value = "minScore", required = false) final Integer minScore,
            @RequestParam(value = "unreadOnly", required = false, defaultValue = "true") final boolean unreadOnly
    ) throws IllegalArgumentException {
        if (aid == null && name == null) {
            throw new IllegalArgumentException("At least one of {aid, name} should be set.");
        }
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentByAidOrName(aid, name);
        return agentService.getResults(username, options, offset, maxResults, minScore, unreadOnly);
    }


    /**
     * Retrieves a list of agent document unread counts.
     *
     * @param session - The current user session
     * @param maxResults - The maximum number of results to retrieve - optional
     * @param minScore - The minimum score of a document for it to be included - optional
     * @return - List of agent unread counts.
     */
    @RequestMapping("/getUnreadCount.json")
    @ResponseBody
    public List<AgentUnread> getUnreadCount(
            final HttpSession session,
            @RequestParam(value = "maxResults", required = false) final Integer maxResults,
            @RequestParam(value = "minScore", required = false) final Integer minScore
    ) {
        final String username = sessionHelper.getRemoteUser(session);
		final List<AgentOptions> agents = agentService.getAgents(username);
		return agentService.getUnreadCount(agents, username, maxResults, minScore);
    }


    /**
     * Marks a specific document as read for an agent.
     *
     * *1 - One of these optional fields must be non-null.
     *
     * @param session - The current user session
     * @param aid - The agent's aid - optional *1
     * @param name - The agent's name - optional *1
     * @param document - The reference of the document to mark as read
     * @return - Status of success
     */
    @RequestMapping("/markAsRead.json")
    @ResponseBody
    public boolean markAsRead(
            final HttpSession session,
            @RequestParam(value = "aid", required = false) final String aid,
            @RequestParam(value = "name", required = false) final String name,
            @RequestParam(value = "reference", required = true) final String document
    ) {
        if (aid == null && name == null) {
            throw new IllegalArgumentException("At least one of {aid, name} should be set.");
        }
        final String username = sessionHelper.getRemoteUser(session);
        final AgentOptions options = AgentFactory.agentByAidOrName(aid, name);
        return agentService.markAsRead(username, options, document);
    }


    /**
     * Looks-up documents given their references.
     *
     * @param references - The list of document references
     * @return - List of associated documents
     */
    @RequestMapping("/getDocumentsFromReferences.json")
    @ResponseBody
    public List<ResultDocument> getDocumentsFromReferences(
            @RequestParam(value = "references[]", required = true) final List<String> references
    ) {
        return agentService.getDocumentsFromReferences(references);
    }


    /**
     * Retrieves a list of concepts related to some seed training text.
     *
     * @param training - The training text
     * @return - The concept list
     */
    @RequestMapping("/getRelatedConcepts.json")
    @ResponseBody
    public List<String> getRelatedConcepts(
            @RequestParam(value = "training", required = true) final String training
    ) {
        return agentService.getRelatedConcepts(training);
    }


    /**
     * Retrieves a list of documents related to some seed training text.
     *
     * @param training - The training text
     * @return - The document list
     */
    @RequestMapping("/getRelatedDocuments.json")
    @ResponseBody
    public List<ResultDocument> getRelatedDocuments(
            @RequestParam(value = "training", required = true) final String training
    ) {
        return agentService.getRelatedDocuments(training);
    }


    /**
     * Retrieves a list of all the database options for agents.
     *
     * @return - List of database names
     */
    @RequestMapping("/getDatabaseOptions.json")
    @ResponseBody
    public List<String> getDatabaseOptions() {
        return agentConfig.getDatabaseOptions();
    }
}
