package com.autonomy.find.api.database;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "find.usersession")
public class UserSession implements Serializable {
	
    @Id
    @Column(name = "id")
    private String id = "";
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = UserData.class)
    @JoinColumn(name = "userdata_fk")
    private UserData userData = null;
    
    @Column(name = "timeout")
    private Timestamp timeout = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    public UserSession() {
        
    }

    public UserSession(final String id, final UserData userData, final Timestamp timeout, final Timestamp currentTime) {
        this.id = id;
        this.userData = userData;
        this.timeout = new Timestamp(timeout.getTime());
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
    }
    
    public UserData getUserData() {
    	return userData;
    }

	public String getId() {
		return id;
	}

	public void setTimeout(Timestamp createSessionTimeout) {
		this.timeout=createSessionTimeout;
	}

}
