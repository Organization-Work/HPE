package com.autonomy.find.services;

import autn.voronoi.Voronoi;
import autn.voronoi.VoronoiProxy;
import com.autonomy.aci.actions.DontCareAsLongAsItsNotAnErrorProcessor;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.api.database.AnonymousUser;
import com.autonomy.find.api.database.NamedUser;
import com.autonomy.find.api.database.UserData;
import com.autonomy.find.api.database.UserSession;
import com.autonomy.find.api.exceptions.UserAlreadyExistsException;
import com.autonomy.find.api.exceptions.UserDoesNotExistException;
import com.autonomy.find.api.exceptions.UserNotCreatedException;
import com.autonomy.find.api.exceptions.UserNotLoggedInException;
import com.autonomy.find.api.exceptions.UserNotLoggedOutException;
import com.autonomy.find.api.exceptions.UserNotNamedException;
import com.autonomy.find.api.exceptions.UserSessionDoesNotExistException;
import com.autonomy.find.api.exceptions.UsernamePasswordIncorrectException;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.ProfileTerm;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.F1;
import com.autonomy.find.util.TimeHelper;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.xml.xpath.XPathExpressionException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@Component
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    /* Actions */
    private static final String
            USER_ADD = "useradd",
            USER_EDIT = "useredit",
            USER_COPY = "usercopy",
            USER_DELETE = "userdelete",
            USER_SEARCH = "UserReadUserListDetails";

    /* Attribute keys */
    private static final String
            MAX_USERS = "MaxUsers",
            USERNAME = "username",
            DESTINATION_USERNAME = "destinationusername",
            PASSWORD = "password",
            NEW_PASSWORD = "newpassword",
            SESSION = "session",
            FIELD_SESSION = "fieldfindnews_session",
            ANONYMOUS = "findnews_anonymous",
            FIELD_ANONYMOUS = "fieldfindnews_anonymous",
            FIELD_DATABASES = "fieldfind_databases",
            FIELD_SUGGESTION_DATABASES = "fieldfind_suggestiondatabases",
            FIELD_DISPLAY_CHARACTERS = "fieldfind_displaycharacters",
            MATCH_FIELD_NAME = "matchfieldname",
            MATCH_FIELD_VALUE = "matchfieldvalue";

    /* Other Constants */
    private static final String
            MD5 = "MD5",
            UTF_8 = "UTF-8";

    private static final long
            timeout = 3600000;  //timeout length for user sessions (1 hour)

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityAci;

    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory findSessionFactory;

    @Autowired
    private SearchConfig config;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    @Autowired
    private TimeHelper timeHelper;

    @Autowired
    private VoronoiProxy voronoi;

    /**
     * Creates a new anonymous user and returns the user's generated GUID
     *
     * @return guid for the anonymous user
     * @throws com.autonomy.find.api.exceptions.UserNotCreatedException      - If the user could not be created
     * @throws java.security.NoSuchAlgorithmException     - If MD5 algorithm could not be found.
     * @throws java.io.UnsupportedEncodingException - Password hash could not be UTF-8 encoded
     */
    public String createAnonymousUser() throws UserNotCreatedException, NoSuchAlgorithmException, UnsupportedEncodingException {

        // Generate a guid for the user's username
        String guid;

        do {
            guid = randomGUID();
        }
        while (doesAnonymousUserExist(guid));

        final Timestamp currentTime = timeHelper.getCurrentTimestamp();

        //attempt to create the anonymous user
        final Session session = findSessionFactory.openSession();
        session.beginTransaction();

        final UserData userData = new UserData(currentTime);
        session.save(userData);

        final AnonymousUser user = new AnonymousUser(userData, guid, currentTime);
        session.save(user);

        try {
            communityAci.executeAction(
                    parametersToCreateUser(userData.getId().toString(), "", config),
                    new DontCareAsLongAsItsNotAnErrorProcessor());

            session.getTransaction().commit();

        } catch (final ProcessorException e) {
            LOGGER.error("Error while creating anonymous user", e);
            session.getTransaction().rollback();
            // If an error occurs - the user can't have been created
            throw new UserNotCreatedException("Anonymous user not created.");

        } finally {
            session.close();
        }

        return user.getGuid();
    }

    /**
     * Creates a new named user.
     *
     * @param username - The new user's username string
     * @param password - The user's password string
     * @throws com.autonomy.find.api.exceptions.UserNotCreatedException      - If the user could not be created
     * @throws java.security.NoSuchAlgorithmException     - If MD5 algorithm could not be found.
     * @throws java.io.UnsupportedEncodingException - Password hash could not be UTF-8 encoded
     */
    public void createNamedUser(final String username, final String password)
            throws UserNotCreatedException, NoSuchAlgorithmException, UnsupportedEncodingException {

        try {
            // Attempt to create the named user
            createNamedUser(username, password, randomGUID(), new UserData(timeHelper.getCurrentTimestamp()), false);
        } catch (final ProcessorException e) {
            LOGGER.error("Error while creating named user", e);
            // If an error occurs - the user can't have been created
            throw new UserNotCreatedException("User named: `%s` not created.",
                    username);
        }
    }

    /**
     * Builds a set of aci parameters for the creation of a user
     *
     * @param password - The password to hash
     * @return Aci Parameter Set
     * @throws java.io.UnsupportedEncodingException
     * @throws java.security.NoSuchAlgorithmException
     */
    private static AciParameters parametersToCreateUser(
        final String username,
        final String password,
        final SearchConfig config)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {

        // Build up the action parameters to create the user
        final AciParameters params = new AciParameters(USER_ADD);

        params.put(USERNAME, username);
        params.put(PASSWORD, "");

        // Default user settings
        params.put(FIELD_SESSION, "");
        params.put(FIELD_ANONYMOUS, true);

        // Default webapp user settings
        params.put(FIELD_DATABASES, config.getDatabases());
        params.put(FIELD_SUGGESTION_DATABASES, config.getSuggestionDatabases());
        params.put(FIELD_DISPLAY_CHARACTERS, config.getDisplayCharacters());

        return params;
    }

    /**
     * Hashes the user's password for storage in community
     *
     * @throws java.security.NoSuchAlgorithmException     - If SHA-256 algorithm could not be found.
     * @throws java.io.UnsupportedEncodingException - Password could not be hashed
     */
    private static String hashPassword(final String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        final MessageDigest md = MessageDigest.getInstance(MD5);
        md.update(password.getBytes(UTF_8));
        return Hex.encodeHexString(md.digest());
    }

    /**
     * Attempts to log in an anonymous user.
     *
     * @param anonGUID - the anonymous user's guid
     * @return session - the generated user session
     * @throws com.autonomy.find.api.exceptions.UserNotLoggedInException  - If the user wasn't logged in
     * @throws com.autonomy.find.api.exceptions.UserDoesNotExistException - If the user wasn't found
     */
    public String loginAnonymousUser(final String anonGUID)
            throws UserNotLoggedInException, UserDoesNotExistException {

        // We can only log in an anonymous user that exists
        if (isUserAvailable(anonGUID, true, true)) {

            // Generate a session code for the user
            final String userSessionId = randomGUID();

            try {
                // Attempt to log in anonymous user
                final Session session = findSessionFactory.openSession();
                session.beginTransaction();

                final UserSession userSession = new UserSession(userSessionId, getUserData(anonGUID, true),
                        createSessionTimeout(), timeHelper.getCurrentTimestamp());

                session.save(userSession);
                session.getTransaction().commit();
                session.close();

                return userSession.getId();
            } catch (final ProcessorException e) {
                LOGGER.warn("Error while logging in anonymous user", e);
                // If we couldn't log in the user, raise an exception
                throw new UserNotLoggedInException(
                        "Could not log in anonymous user: `%s`", anonGUID);
            }
        } else {
            // We can't log in a user that doesn't exist
            // So raise an exception
            throw new UserDoesNotExistException(
                    "No anonymous user associated with guid: `%s`.", anonGUID);
        }
    }

    /**
     * Attempts to log in a named user.
     *
     * @param username - the user's username
     * @param password - the user's password
     * @return session - the generated user session
     * @throws com.autonomy.find.api.exceptions.UserNotLoggedInException - If the user was not logged in
     * @throws com.autonomy.find.api.exceptions.UsernamePasswordIncorrectException
     *                                  - If username/password are incorrect
     */
    public String loginNamedUser(final String username, final String password)
            throws UserNotLoggedInException,
            UsernamePasswordIncorrectException {

        // Generate a session code for the user
        final String userSessionId = randomGUID();

        try {
            // Attempt to load the user's password
            final NamedUser user = getNamedUser(username);

            if (passwordsMatch(password, user.getSalt(), user.getPassword())) {
                final Session session = findSessionFactory.openSession();
                session.beginTransaction();

                final UserSession userSession = new UserSession(userSessionId, user.getUserData(), createSessionTimeout(), timeHelper.getCurrentTimestamp());

                session.save(userSession);
                session.getTransaction().commit();
                session.close();

                return userSession.getId();
            } else {
                throw new UsernamePasswordIncorrectException("");
            }

        } catch (final UserDoesNotExistException e) {
            LOGGER.debug("User does not exist, cannot be logged in", e);
            throw new UsernamePasswordIncorrectException("");

        } catch (final ProcessorException e) {
            LOGGER.error("Error while trying to parse login response for user", e);
            throw new UserNotLoggedInException("");
        }
    }

    /**
     * Compares an entered password with a hashed user password for equality.
     *
     * @param password     - user's hashes password
     * @param salt         - user's salt
     * @param userPassword - supplied password
     * @return boolean - true if the hash of the password is equivalent to the user's password hash
     */
    private static boolean passwordsMatch(
            final String password,
            final String salt,
            final String userPassword) {

        try {
            return userPassword.equals(hashPassword(password + salt));
        }
        catch (final Exception e) {
            LOGGER.trace("Error while testing password validity", e);
            return false;
        }
    }

    /**
     * Determines if a user exists given a username and whether or not they are anonymous.
     *
     * @param username    - user's username
     * @param isAnonymous - is the user anonymous
     * @return returns true if user exists otherwise false
     */
    public boolean isUserAvailable(final String username, final boolean isAnonymous, final boolean shouldBeActive) {

        String query;

        if (isAnonymous) {
            query = "from AnonymousUser where guid = :username";
        } else {
            query = "from NamedUser where username = :username";
        }

        if (shouldBeActive) {
            query += " and userData.active = true ";
        }

        final Session session = findSessionFactory.openSession();
        @SuppressWarnings("unchecked")
        final List<Object> user = session.createQuery(query).setString("username", username).setMaxResults(1).list();
        session.close();

        return !user.isEmpty();
    }

    /**
     * Determines if a specific named user exists
     *
     * @param username - user's username
     * @return boolean returns true if user exists otherwise false
     */
    public boolean doesNamedUserExist(final String username) {
        return isUserAvailable(username, false, true);
    }

    /**
     * Determines if a specific named user exists
     *
     * @param username - user's username
     * @return boolean - true if anonymous user exists otherwise false
     */
    public boolean doesAnonymousUserExist(final String username) {
        return isUserAvailable(username, true, true);
    }

    /**
     * @param session  - the session guid
     * @param username - user's username
     * @param password - user's password
     * @throws com.autonomy.find.api.exceptions.UserDoesNotExistException    - Anonymous user does not exist
     * @throws com.autonomy.find.api.exceptions.UserAlreadyExistsException   - Username already exists
     * @throws com.autonomy.find.api.exceptions.UserNotNamedException        - Could not create a named user account
     * @throws java.security.NoSuchAlgorithmException     - If MD5 algorithm could not be found.
     * @throws java.io.UnsupportedEncodingException - Password hash could not be UTF-8 encoded
     */
    public void nameAnonymousUser(
            final String session,
            final String username,
            final String password)
            throws UserDoesNotExistException,
            UserAlreadyExistsException,
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            UserNotNamedException {

        try {
            final UserSession userSession = getUserSession(session);

            if (doesNamedUserExist(username)) {
                throw new UserAlreadyExistsException("Username with name: `%s` already exists", username);
            }

            if (!isAnonymousUser(userSession.getUserData())) {
                throw new UserDoesNotExistException("No anonymous user assoicated with session: `%s`", session);
            }

            createNamedUser(username, password, randomGUID(), userSession.getUserData(), true);
        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new UserNotNamedException("Anonymous user with session: `%s` not named as: `%s`", session, username);
        }
    }

    /**
     * @param sessionId
     * @param username
     * @param password
     * @throws com.autonomy.find.api.exceptions.UserDoesNotExistException
     * @throws com.autonomy.find.api.exceptions.UsernamePasswordIncorrectException
     *
     */
    public void mergeAnonymousWithUser(
            final String sessionId,
            final String username,
            final String password
    ) throws UserDoesNotExistException, UsernamePasswordIncorrectException {

        try {
            final NamedUser user = getNamedUser(username);

            if (passwordsMatch(password, user.getSalt(), user.getPassword())) {

                // TODO copy anonymous data into user
                // TODO delete anonymous user

            } else {
                throw new UsernamePasswordIncorrectException("");
            }

        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Generates a Pseudo-Random GUID
     *
     * @return returns a GUID
     */
    private String randomGUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    /**
     *
     * @param sessionId
     * @return
     * @throws com.autonomy.find.api.exceptions.UserSessionDoesNotExistException
     */
    public List<ResultDocument> getProfileDocuments(final String username) throws UserSessionDoesNotExistException {

        final AciParameters params = new AciParameters("ProfileGetResults");
        params.add("UserName", username);
        params.add("DREXMLMeta", true);
        params.add("DREDatabaseMatch", "News");

        try {
            final List<ResultDocument> docs = communityAci.executeAction(params, processorFactory.listProcessorForClass(ResultDocument.class));
            return docs;
        }
        catch (final ProcessorException e) {
            LOGGER.error("Error while getting profile documents", e);
            return new LinkedList<ResultDocument>();
        }
    }

    /**
     * Retrieves UserData
     *
     * @param username    - the user's username
     * @param isAnonymous - is it an anonymous user
     * @return UserData - the user's UserData
     * @throws com.autonomy.find.api.exceptions.UserDoesNotExistException - If the user wasn't found
     */
    public UserData getUserData(final String username, final boolean isAnonymous) throws UserDoesNotExistException {
        final String query;

        if (isAnonymous) {
            query = "select userData from AnonymousUser where guid = :username";
        } else {
            query = "select userData from NamedUser where username = :username";
        }

        final Session session = findSessionFactory.openSession();
        @SuppressWarnings("unchecked")
        final List<UserData> userDataList = session.createQuery(query).setString("username", username).setMaxResults(1).list();
        session.close();

        if (userDataList.isEmpty()) {
            throw new UserDoesNotExistException("Could not find user for name: `%s`", username);
        } else {
            return userDataList.get(0);
        }
    }

    /**
     * Retrieves NamedUser
     *
     * @param username - the user's username
     * @return NamedUser - the named user
     * @throws com.autonomy.find.api.exceptions.UserDoesNotExistException - If the user wasn't found
     */
    public NamedUser getNamedUser(final String username) throws UserDoesNotExistException {
        final Session session = findSessionFactory.openSession();

        @SuppressWarnings("unchecked")
        final List<NamedUser> userList = session.createQuery("from NamedUser where username = :username")
                .setString("username", username)
                .setMaxResults(1)
                .list();

        session.close();

        if (userList.isEmpty()) {
            throw new UserDoesNotExistException("Could not find user for name: `%s`", username);
        } else {
            return userList.get(0);
        }
    }

    /**
     * Creates a session timeout
     *
     * @return Timestamp - timestamp for the session timeout
     */
    public Timestamp createSessionTimeout() {
        return timeHelper.getCurrentTimestampWithOffset(timeout);
    }

    /**
     * Retrieves UserSession
     *
     * @param sessionId - the session's guid
     * @return UserSession - the user session
     * @throws com.autonomy.find.api.exceptions.UserSessionDoesNotExistException
     *          - If the session wasn't found
     */
    public UserSession getUserSession(final String sessionId) throws UserSessionDoesNotExistException {
        final Session db = findSessionFactory.openSession();

        @SuppressWarnings("unchecked")
        final List<UserSession> userSessionList = db.createQuery("from UserSession where id = :sessionId")
                .setString("sessionId", sessionId)
                .setMaxResults(1)
                .list();

        db.close();

        if (userSessionList.isEmpty()) {
            throw new UserSessionDoesNotExistException("Could not find user session for session: `%s`", sessionId);
        } else {
            return userSessionList.get(0);
        }
    }

    /**
     * Retrieves and refreshes a user's UserSession
     *
     * @param sessionId - the session id
     * @return UserSession - the user session found
     * @throws com.autonomy.find.api.exceptions.UserSessionDoesNotExistException when the user's session isn't found
     */
    public UserSession getAndRefreshUserSession(final String sessionId) throws UserSessionDoesNotExistException {
        final UserSession userSession = getUserSession(sessionId);
        refreshSession(userSession);
        return userSession;
    }

    /**
     * Create a named user
     *
     * @param username            - user's username
     * @param password            - user's password
     * @param salt                - user's salt
     * @param userData            - user's user data
     * @param removeAnonymousUser - if true removes any anonymous users associated with the user's UserData
     * @return NamedUser - returns the newly created user
     * @throws java.security.NoSuchAlgorithmException     - If MD5 algorithm could not be found.
     * @throws java.io.UnsupportedEncodingException - Password hash could not be UTF-8 encoded
     */
    public NamedUser createNamedUser(
            final String username,
            final String password,
            final String salt,
            final UserData userData,
            final boolean removeAnonymousUser
    ) throws NoSuchAlgorithmException, UnsupportedEncodingException, UserNotCreatedException {
        final Session session = findSessionFactory.openSession();
        session.beginTransaction();

        final NamedUser user = new NamedUser(username, hashPassword(password + salt), salt, userData, timeHelper.getCurrentTimestamp());
        session.save(user);

        try {
            if (removeAnonymousUser) {
                session.createQuery("delete from AnonymousUser where userData.id = :userDataId")
                        .setInteger("userDataId", userData.getId())
                        .executeUpdate();
            } else {
                communityAci.executeAction(
                        parametersToCreateUser(user.getUserData().getId().toString(), "", config),
                        new DontCareAsLongAsItsNotAnErrorProcessor());
            }
            session.getTransaction().commit();

        } catch (final ProcessorException e) {
            LOGGER.error("Error while creating named user", e);
            session.getTransaction().rollback();
            // If an error occurs - the user can't have been created
            throw new UserNotCreatedException("Named user not created.");

        } finally {
            session.close();
        }

        return user;
    }

    /**
     * Logs a user out
     *
     * @param sessionId - the session's guid
     * @throws com.autonomy.find.api.exceptions.UserNotLoggedOutException - If the session was not deleted
     */
    public void logout(final String sessionId) throws UserNotLoggedOutException {
        final Session session = findSessionFactory.openSession();
        session.beginTransaction();

        final int rowsDeleted = session.createQuery("delete from UserSession where id = :sessionId")
                .setString("sessionId", sessionId)
                .executeUpdate();

        session.getTransaction().commit();
        session.close();

        if (rowsDeleted == 0) {
            throw new UserNotLoggedOutException("Could not find user session : `%s` to logout.", sessionId);
        }
    }

    /**
     * @param username
     * @param isAnonymous
     * @return true if user is logged in otherwise false
     */
    public boolean isUserLoggedIn(
            final String username,
            final boolean isAnonymous
    ) {
        final String query = String.format("from %s as user, UserSession as session where %s = :username and session.userData = user.userData and session.timeout > :now",
                isAnonymous ? "AnonymousUser" : "NamedUser",
                isAnonymous ? "user.guid" : "user.username");

        final Session session = findSessionFactory.openSession();
        @SuppressWarnings("unchecked")
        final List<Object> user = session.createQuery(query)
                .setString("username", username)
                .setTimestamp("now", new Date(System.currentTimeMillis()))
                .setMaxResults(1)
                .list();
        session.close();

        return !user.isEmpty();
    }

    /**
     * @param sessionId
     * @return true if session is valid otherwise false
     */
    public boolean checkValidSession(
            final String sessionId
    ) {
        final Session session = findSessionFactory.openSession();
        @SuppressWarnings("unchecked")
        final List<Object> result = session.createQuery("from UserSession where id = :sessionId and timeout > :now")
                .setString("sessionId", sessionId)
                .setTimestamp("now", new Date(System.currentTimeMillis()))
                .setMaxResults(1)
                .list();
        session.close();

        return !result.isEmpty();
    }

    public boolean isAnonymousUser(final UserData userData) {
        final Session session = findSessionFactory.openSession();
        @SuppressWarnings("unchecked")
        final List<Object> result = session.createQuery("from AnonymousUser where userData.id = :userDataId")
                .setInteger("userDataId", userData.getId())
                .setMaxResults(1)
                .list();
        session.close();

        return !result.isEmpty();
    }

    @Scheduled(fixedDelay = 3600000)
    public void removeExpiredSessions() {
        final Session session = findSessionFactory.openSession();
        session.beginTransaction();

        final int rowsDeleted = session.createQuery("delete from UserSession where timeout < :now")
                .setTimestamp("now", new Date(System.currentTimeMillis()))
                .executeUpdate();

        session.getTransaction().commit();
        session.close();
    }

    public void refreshSession(final UserSession userSession) {
        final Session db = findSessionFactory.openSession();
        db.beginTransaction();

        userSession.setTimeout(createSessionTimeout());
        db.saveOrUpdate(userSession);

        db.getTransaction().commit();
        db.close();
    }

    /**
     * Use only in an explicit transaction
     * @param userData
     */
    private void deleteSessionsForUserData(final Session db, final UserData userData) {
        // delete all sessions related to the user
		db.createQuery("delete from UserSession where userData = :userData")
		  .setParameter("userData", userData).executeUpdate();
	}

    private UserData connectUserDataWithDBSession(final Session db, final UserData userData) {
    	return (UserData)db.get(UserData.class, userData.getId());
    }
    
    /**
     * Deletes an anonymous user given their user data.
     * Returns true - if the user is deleted (deactivated)
     * Otherwise false - If the user can't be deleted or isn't anonymous
     * 
     * @param userData
     */
	public boolean deleteAnonymousUserWithUserData(final UserData _userData) {
		return deleteUserWithUserData(_userData, false);
	}

	/**
	 * Deletes a named user given their user data.
	 * Returns	True - if the user is deleted
	 *  		False - upon a deletion failure
	 * 
	 * @param _userData
	 * @return
	 */
	public boolean deleteNamedUserWithUserData(final UserData _userData) {
		return deleteUserWithUserData(_userData, true);
	}
	
	/**
	 * 
	 * 
	 * @param _userData
	 * @param shouldBeNamed
	 * @return
	 */
	public boolean deleteUserWithUserData(
			final UserData _userData,
			final boolean shouldBeNamed) {

		//  Predicate:
		//  	user must be named if shouldBeNamed is true
		//  	otherwise user must be anonymous
		if (isAnonymousUser(_userData) == shouldBeNamed) {

			return false;
		}

		//  Begin our database transaction
		final Session db = findSessionFactory.openSession();
		db.beginTransaction();

		try {

			//  Get the user data from the DB to prevent potential errors with false equalities 
			final UserData userData = connectUserDataWithDBSession(db, _userData);

			//  Deactivate the user data
			userData.setActive(false);
			//  Push the changes to the transaction
			db.update(userData);

			//  Force logout all sessions connected with the user data
			deleteSessionsForUserData(db, userData);

			//	If all was successful, attempt to commit the changes.
			db.getTransaction().commit();
			return true;
		}
		catch (final Exception e) {
            LOGGER.error("Error while deleting user data, rolling back transaction", e);
			db.getTransaction().rollback();
		}
		finally {

			db.close();
		}

		return false;
	}

	/**
	 * 
	 * 
	 * @param username
	 * @throws com.autonomy.aci.client.services.ProcessorException
	 */
	public boolean deleteCommunityProfile(final String username)
	throws ProcessorException {
		final AciParameters params = new AciParameters("UserDelete");
		params.add("username", username);
        return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
    }

    public List<ProfileTerm> getProfileTerms(final String username) {
        final AciParameters params = new AciParameters("profileread");
        params.add("username", username);
        params.add("showterms", true);
        return communityAci.executeAction(params, processorFactory.listProcessorForClass(ProfileTerm.class));
    }

    public Voronoi.Cluster getProfileClusters(
            final String username,
            final long daysBack,
            final int childLimit,
            final int engine
    ) throws XPathExpressionException {
        final String terms = termsToQueryText(getProfileTerms(username));
        return voronoi.clusters(terms, childLimit, 100, false, engine, true, timeHelper.calculateDaysAgo(daysBack), null, false, 60.0d);
    }

    public String termsToQueryText(final List<ProfileTerm> terms) {
        return CollUtils.intersperse(" ", CollUtils.map(new F1<ProfileTerm, String>() {
            public String apply(final ProfileTerm term) {
                return term.toString();
            }
        }, terms));
    }

}
