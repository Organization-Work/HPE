package com.autonomy.find.api.controllers;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import autn.voronoi.Voronoi;

import com.autonomy.find.api.database.UserData;
import com.autonomy.find.api.database.UserSession;
import com.autonomy.find.api.exceptions.UserAlreadyExistsException;
import com.autonomy.find.api.exceptions.UserDoesNotExistException;
import com.autonomy.find.api.exceptions.UserNotLoggedInException;
import com.autonomy.find.api.exceptions.UserNotLoggedOutException;
import com.autonomy.find.api.exceptions.UserNotNamedException;
import com.autonomy.find.api.exceptions.UserSessionDoesNotExistException;
import com.autonomy.find.api.exceptions.UsernamePasswordIncorrectException;
import com.autonomy.find.api.response.ResponseWithItems;
import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSession;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.config.FindNewsConfig;
import com.autonomy.find.dto.PropertyRequestData;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.services.HistoryService;
import com.autonomy.find.services.UserPropertiesService;
import com.autonomy.find.services.UserService;

@Controller
@RequestMapping("/api/user")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsController.class);

    private static final String
            USER_PROFILE_ERROR = "User profile does not exist.",
            PROFILE_CLUSTER_ERROR = "Profile cluster error.",
            USER_NOT_CREATED_ERROR = "User not created.",
            USER_NOT_NAMED_ERROR = "Anonymous user not named.",
            USERNAME_ALREADY_TAKEN_ERROR = "Username already taken.",
            PASSWORD_CHARACTERS_ERROR = "Password contains illegal characters.",
            USER_NOT_FOUND_ERROR = "User not found.",
            USER_DOES_NOT_EXIST_ERROR = "User does not exist.",
            USERNAME_PASSWORD_ERROR = "Username or password is incorrect.",
            USER_NOT_LOGGED_IN_ERROR = "User not logged in.",
            USER_NOT_LOGGED_OUT_ERROR = "User not logged out.",
            UNABLE_TO_GET_RECENT_SEARCH_HISTORY_ERROR = "Unable to get recent search history.",
            USER_SESSION_NOT_FOUND_ERROR = "User session not found.",
            USER_NOT_DELETED_ERROR = "User was not deleted.",
            PROPERTY_PATH_LOOKUP_ERROR = "Property path lookup error.";

    @Autowired
    private UserService userService;

    @Autowired
    private UserPropertiesService propertiesService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private FindNewsConfig config;

    /**
     * @param session
     * @return documents
     */
    @RequestMapping("getSuggested.json")
    public
    @ResponseBody
    ResponseWithItems<ResultDocument> getSuggested(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "trailingEnd", required = false, defaultValue = "false") final boolean trailingEnd,
            @RequestParam(value = "tag", required = false, defaultValue = "false") final boolean tag) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            final Integer userId = user.getId();
            final List<ResultDocument> docs = userService.getProfileDocuments(userId.toString());
            ResultDocument.process(docs, trailingEnd, false, tag);
            return new ResponseWithItems<ResultDocument>(docs, true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<ResultDocument>(
                    new ArrayList<ResultDocument>(), false, USER_SESSION_NOT_FOUND_ERROR);
        } catch (final Exception e) {
            LOGGER.error(USER_PROFILE_ERROR, e);
            return new ResponseWithItems<ResultDocument>(new ArrayList<ResultDocument>(), false, USER_PROFILE_ERROR);
        }
    }


    /**
     * @param session
     * @return documents
     */
    @RequestMapping("getSuggestedTopics.json")
    public
    @ResponseBody
    ResponseWithSuccessError getSuggestedTopics(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "daysBack", required = false, defaultValue = "7") final long daysBack,
            @RequestParam(value = "childLimit", required = false, defaultValue = "4") final int childLimit
    ) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);
            final Integer userId = user.getId();

            try {
                final Voronoi.Cluster cluster = userService.getProfileClusters(
                        userId.toString(), daysBack, childLimit, config.getProfileTopicsVoronoiEngine());
                return new ResponseWithResult<>(cluster, true, null);
            }
            catch (final Exception e) {
                LOGGER.error(PROFILE_CLUSTER_ERROR, e);
                return new ResponseWithSuccessError(PROFILE_CLUSTER_ERROR);
            }

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithSuccessError(USER_SESSION_NOT_FOUND_ERROR);
        } catch (final Exception e) {
            LOGGER.error(USER_PROFILE_ERROR, e);
            return new ResponseWithSuccessError(USER_PROFILE_ERROR);
        }
    }


    /**
     * @return anonGUID
     */
    @RequestMapping("createAnonymousUser.json")
    public
    @ResponseBody
    ResponseWithResult<String> createAnonymousUser() {
        try {
            return new ResponseWithResult<String>(userService.createAnonymousUser(), true, null);

        } catch (final Exception e) {
            LOGGER.error(USER_NOT_CREATED_ERROR, e);
            return new ResponseWithResult<String>("", false, USER_NOT_CREATED_ERROR);
        }
    }

    /**
     * @param username
     * @param password
     */
    @RequestMapping("createNamedUser.json")
    public
    @ResponseBody
    ResponseWithSuccessError createNamedUser(@RequestParam("username") final String username, @RequestParam("password") final String password) {

        if (userService.doesNamedUserExist(username))
            return new ResponseWithSuccessError(false, USERNAME_ALREADY_TAKEN_ERROR);

        try {
            userService.createNamedUser(username, password);
            return new ResponseWithSuccessError(true, null);

        } catch (final UnsupportedEncodingException e) {
            LOGGER.error(PASSWORD_CHARACTERS_ERROR, e);
            return new ResponseWithSuccessError(false, PASSWORD_CHARACTERS_ERROR);

        } catch (final Exception e) {
            LOGGER.error(USER_NOT_CREATED_ERROR, e);
            return new ResponseWithSuccessError(false, USER_NOT_CREATED_ERROR);
        }

    }

    /**
     * @param guid
     * @return session
     */
    @RequestMapping("loginAnonymousUser.json")
    public
    @ResponseBody
    ResponseWithSession loginAnonymousUser(@RequestParam("guid") final String guid) {

        try {
            return new ResponseWithSession(userService.loginAnonymousUser(guid), true, null);

        } catch (final UserNotLoggedInException e) {
            LOGGER.error(USER_NOT_LOGGED_IN_ERROR, e);
            return new ResponseWithSession("", false, USER_NOT_LOGGED_IN_ERROR);

        } catch (final UserDoesNotExistException e) {
            LOGGER.error(USER_DOES_NOT_EXIST_ERROR, e);
            return new ResponseWithSession("", false, USER_DOES_NOT_EXIST_ERROR);
        }

    }

    /**
     * @param username
     * @param password
     * @return session
     */
    @RequestMapping("loginNamedUser.json")
    public
    @ResponseBody
    ResponseWithSession loginNamedUser(@RequestParam("username") final String username, @RequestParam("password") final String password) {

        try {
            return new ResponseWithSession(userService.loginNamedUser(username, password), true, null);

        } catch (final UsernamePasswordIncorrectException e) {
            LOGGER.error(USERNAME_PASSWORD_ERROR, e);
            return new ResponseWithSession("", false, USERNAME_PASSWORD_ERROR);

        } catch (final UserNotLoggedInException e) {
            LOGGER.error(USER_NOT_LOGGED_IN_ERROR, e);
            return new ResponseWithSession("", false, USER_NOT_LOGGED_IN_ERROR);
        }
    }

    /**
     * @param username
     * @return boolean
     */
    @RequestMapping("doesNamedUserExist.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> doesNamedUserExist(@RequestParam("username") final String username) {

        return new ResponseWithResult<Boolean>(userService.doesNamedUserExist(username), true, null);
    }

    /**
     * @param anonGUID
     * @return boolean
     */
    @RequestMapping("doesAnonymousUserExist.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> doesAnonymousUserExist(@RequestParam("anonGUID") final String anonGUID) {

        return new ResponseWithResult<Boolean>(userService.doesAnonymousUserExist(anonGUID), true, null);
    }

    /**
     * @param session
     * @param username
     * @param password
     */
    @RequestMapping("nameAnonymousUser.json")
    public
    @ResponseBody
    ResponseWithSuccessError nameAnonymousUser(@RequestParam("session") final String session, @RequestParam("username") final String username, @RequestParam("password") final String password) {

        try {
            userService.nameAnonymousUser(session, username, password);
            return new ResponseWithSuccessError(true, null);

        } catch (final UserDoesNotExistException e) {
            LOGGER.error(USER_NOT_FOUND_ERROR, e);
            return new ResponseWithSuccessError(false, USER_NOT_FOUND_ERROR); // anonGUID

        } catch (final UserAlreadyExistsException e) {
            LOGGER.error(USERNAME_ALREADY_TAKEN_ERROR, e);
            return new ResponseWithSuccessError(false, USERNAME_ALREADY_TAKEN_ERROR); // username

        } catch (final UserNotNamedException e) {
            LOGGER.error(USER_NOT_NAMED_ERROR, e);
            // some other error
            return new ResponseWithSuccessError(false, USER_NOT_NAMED_ERROR);

        } catch (final Exception e) {
            LOGGER.error(PASSWORD_CHARACTERS_ERROR, e);
            return new ResponseWithSuccessError(false, PASSWORD_CHARACTERS_ERROR); // password
        }
    }

    /**
     * @param session
     * @param username
     * @param password
     */
    @RequestMapping("mergeAnonymousWithUser.json")
    public
    @ResponseBody
    ResponseWithSuccessError mergeAnonymousWithUser(@RequestParam("session") final String session, @RequestParam("username") final String username, @RequestParam("password") final String password) {

        try {
            userService.mergeAnonymousWithUser(session, username, password);
            return new ResponseWithSuccessError(true, null);

        } catch (final UserDoesNotExistException e) {
            LOGGER.error(USER_NOT_FOUND_ERROR, e);
            return new ResponseWithSuccessError(false, USER_NOT_FOUND_ERROR); // anonGUID

        } catch (final UsernamePasswordIncorrectException e) {
            // username & password
            LOGGER.error(USERNAME_PASSWORD_ERROR, e);
            return new ResponseWithSuccessError(false, USERNAME_PASSWORD_ERROR);
        }
    }

    @RequestMapping("logout.json")
    public
    @ResponseBody
    ResponseWithSuccessError logout(@RequestParam("session") final String session) {
        try {
            userService.logout(session);
            return new ResponseWithSuccessError(true, null);
        } catch (final UserNotLoggedOutException ex) {
            LOGGER.error(USER_NOT_LOGGED_OUT_ERROR, ex);
            return new ResponseWithSuccessError(false, USER_NOT_LOGGED_OUT_ERROR);
        }
    }

    @RequestMapping("isAnonymousUserLoggedIn.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> isAnonymousUserLoggedIn(@RequestParam("guid") final String user) {
        return new ResponseWithResult<Boolean>(userService.isUserLoggedIn(user, true), true, null);
    }

    @RequestMapping("isNamedUserLoggedIn.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> isNamedUserLoggedIn(@RequestParam("username") final String user, @RequestParam("isAnonymous") final boolean isAnonymous) {
        return new ResponseWithResult<Boolean>(userService.isUserLoggedIn(user, false), true, null);
    }

    @RequestMapping("isUserLoggedIn.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> isUserLoggedIn(@RequestParam("username") final String user, @RequestParam("isAnonymous") final boolean isAnonymous) {
        return new ResponseWithResult<Boolean>(userService.isUserLoggedIn(user, isAnonymous), true, null);
    }

    @RequestMapping("checkValidSession.json")
    public
    @ResponseBody
    ResponseWithResult<Boolean> checkValidSession(@RequestParam("session") final String session) {
        return new ResponseWithResult<Boolean>(userService.checkValidSession(session), true, null);
    }

    @RequestMapping("getRecentSearchHistory.json")
    public
    @ResponseBody
    ResponseWithItems<String> getRecentSearchHistory(
            @RequestParam(value = "session") final String session,
            @RequestParam(value = "maxResults", required = false, defaultValue = "10") final int maxResults) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<String>(
                    historyService.getRecentSearchesForUserAsStrings(
                            user.getId().toString(), maxResults),
                    true, null);

        } catch (final UserSessionDoesNotExistException e) {
            LOGGER.error(UNABLE_TO_GET_RECENT_SEARCH_HISTORY_ERROR, e);
            return new ResponseWithItems<String>(new LinkedList<String>(), false, UNABLE_TO_GET_RECENT_SEARCH_HISTORY_ERROR);
        }
    }

    @RequestMapping("deleteAnonymous.json")
    public
    @ResponseBody
    ResponseWithSuccessError deleteAnonymous(
            @RequestParam(value = "session", required = false) final String session) {
        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData userData = userSession.getUserData();
            userService.deleteAnonymousUserWithUserData(userData);
            return new ResponseWithSuccessError(true, null);
        } catch (final UserSessionDoesNotExistException e) {
            LOGGER.error(USER_NOT_DELETED_ERROR, e);
            return new ResponseWithSuccessError(false, USER_NOT_DELETED_ERROR);
        }
    }

    @RequestMapping("deleteNamed.json")
    public
    @ResponseBody
    ResponseWithSuccessError deleteNamed(
            @RequestParam(value = "session", required = false) final String session) {
        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData userData = userSession.getUserData();
            userService.deleteNamedUserWithUserData(userData);
            return new ResponseWithSuccessError(true, null);
        } catch (final UserSessionDoesNotExistException e) {
            LOGGER.error(USER_NOT_DELETED_ERROR, e);
            return new ResponseWithSuccessError(false, USER_NOT_DELETED_ERROR);
        }
    }

    @RequestMapping("getProperties.json")
    public
    @ResponseBody
    ResponseWithResult<Map<String, Object>> getProperties(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "paths[]", required = false) final List<String> paths) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData userData = userSession.getUserData();
            userService.refreshSession(userSession);

            Map<String, Object> items = new HashMap<String, Object>();
            if (paths != null) {
                for (final String path : paths) {
                    Object result;
                    try {
                        result = propertiesService.getPropertiesViaPath(userData, path);
                        if (result != null) {
                            items.put(path, result);
                        }
                    } catch (Exception e) {
                        LOGGER.error(PROPERTY_PATH_LOOKUP_ERROR, e);
                    }
                }
            } else {
                items = propertiesService.getUserProperties(userData).asMap();
            }
            return new ResponseWithResult<Map<String, Object>>(items, true, null);
        } catch (final Exception e) {
            LOGGER.error(PROPERTY_PATH_LOOKUP_ERROR, e);
            return new ResponseWithResult<Map<String, Object>>(null, false, PROPERTY_PATH_LOOKUP_ERROR);
        }
    }

    @RequestMapping("setProperties.json")
    public
    @ResponseBody
    ResponseWithItems<String> setProperties(
            @RequestBody final PropertyRequestData requestData) {

        try {
            final String session = requestData.getSession();
            final HashMap<String, Object> data = requestData.getData();

            final UserSession userSession = userService.getUserSession(session);
            final UserData userData = userSession.getUserData();
            userService.refreshSession(userSession);

            final List<String> results = new LinkedList<String>();

            for (final Map.Entry<String, Object> entry : data.entrySet()) {
                try {
                    propertiesService.setPropertiesViaPath(userData, entry.getKey(), entry.getValue());
                    results.add(entry.getKey());
                } catch (Exception e) {
                    LOGGER.error(PROPERTY_PATH_LOOKUP_ERROR, e);
                }
            }

            return new ResponseWithItems<String>(results, true, null);
        } catch (final UserSessionDoesNotExistException e) {
            LOGGER.error(PROPERTY_PATH_LOOKUP_ERROR, e);
            return new ResponseWithItems<String>(new LinkedList<String>(), false, PROPERTY_PATH_LOOKUP_ERROR);
        }
    }
}