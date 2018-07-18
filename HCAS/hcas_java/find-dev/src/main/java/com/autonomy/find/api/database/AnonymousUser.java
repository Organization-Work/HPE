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
@Table(name = "find.anonymoususer")
public class AnonymousUser implements Serializable {
	
	@Id
    @Column(name = "guid")
    private String guid = "";
	
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = UserData.class)
    @JoinColumn(name = "userdata_fk")
    private UserData userData = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    public AnonymousUser() {
        
    }

    public AnonymousUser(final UserData userData, final String guid, final Timestamp currentTime) {
        this.userData = userData;
        this.guid = guid;
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
    }

	public String getGuid() {
		return guid;
	}

}
