package com.autonomy.find.api.database;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import lombok.Data;

@Data
@Entity
@SuppressWarnings("serial")
@Table(name = "find.userproperties")
public class UserProperties implements Serializable {

    @Id
    @OneToOne
    ( cascade = CascadeType.ALL
    , fetch = FetchType.EAGER
    , targetEntity = UserData.class )
    @JoinColumn(name = "userdata_fk")
    private UserData userData = null;

    @Column(name = "content")
    private String content = null;

    @Id
    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    public UserProperties() { }

    public UserProperties(
            final UserData userData,
            final Timestamp currentTime) {
        this.userData = userData;
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
    }

    @SuppressWarnings("unchecked")
	public Map<String, Object> asMap()
            throws IOException, JsonParseException, JsonMappingException {
    	final ObjectMapper jsonMapper = new ObjectMapper();
		return (Map<String, Object>)jsonMapper.readValue(this.content, Map.class);
    }

    public UserProperties fromMap(
    		final Map<String, Object> model)
    throws JsonGenerationException, JsonMappingException, IOException {
    	final ObjectMapper jsonMapper = new ObjectMapper();
    	this.content = jsonMapper.writeValueAsString(model);
    	return this;
    }
    
    public static UserProperties createFromMap(
    		final UserData userData,
    		final Timestamp currentTime,
    		final Map<String, Object> model)
    throws JsonGenerationException, JsonMappingException, IOException {
      return (new UserProperties(userData, currentTime).fromMap(model));
    }
}