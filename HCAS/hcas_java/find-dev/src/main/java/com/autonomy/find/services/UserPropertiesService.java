package com.autonomy.find.services;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.autonomy.find.api.database.UserData;
import com.autonomy.find.api.database.UserProperties;

import com.autonomy.find.util.MapTreeWalker;
import com.autonomy.find.util.PathLexer;
import com.autonomy.find.util.TimeHelper;
import com.autonomy.find.util.TreeWalker;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.find.api.exceptions.PropertyNotFoundException;
import com.autonomy.find.config.SearchConfig;

@Service
public class UserPropertiesService {

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


  public UserProperties getUserProperties(
      final UserData userData)
  throws PropertyNotFoundException {
    final Session db = findSessionFactory.openSession();
    try {
      final UserProperties properties = (UserProperties)db
        .createQuery("from UserProperties where userData = :userData order by date_added desc")
        .setParameter("userData", userData)
        .setMaxResults(1)
        .uniqueResult();

      if (properties != null) {
        return properties;
      }
    }
    finally {
      db.close();
    }
    throw new PropertyNotFoundException();
  }


  public Object getPropertiesViaPath(
      final UserData userData,
      final String pathString)
  throws PropertyNotFoundException,
  		 JsonParseException,
  		 JsonMappingException,
  		 IOException {
    final Map<String, Object> properties = getUserProperties(userData).asMap();
    final TreeWalker editor = MapTreeWalker.forMap(properties);
    final List<String> path = PathLexer.lex(pathString);
    return editor.select(path);
  }


  public void setPropertiesViaPath(
      final UserData userData,
      final String pathString,
      final Object data)
  throws PropertyNotFoundException,
  		 JsonParseException,
  		 JsonMappingException,
  		 IOException {

    final Session db = findSessionFactory.openSession();

    try {
      db.beginTransaction();

	  UserProperties properties = null;
      final Timestamp time = new Timestamp(System.currentTimeMillis());
      try {
    	  properties = getUserProperties(userData);
      }
      catch (final Exception e) {
    	  /* Intentionally empty */
      }
      final Map<String, Object> model = (properties == null) ? new HashMap<String, Object>() : properties.asMap();
      final TreeWalker editor = MapTreeWalker.forMap(model);
      final List<String> path = PathLexer.lex(pathString);
      editor.update(path, data);
      db.save(UserProperties.createFromMap(userData, time, model));

      db.getTransaction().commit();
    }
    finally {
      db.close();
    }
  }
}
