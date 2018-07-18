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
@Table(name = "find.nameduser")
public class NamedUser implements Serializable {
	
	@Id
    @Column(name = "username")
    private String username = "";
	
    @Column(name = "password")
    private String password = "";
    
    @Column(name = "salt")
    private String salt = "";
	
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = UserData.class)
    @JoinColumn(name = "userdata_fk")
    private UserData userData = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    public NamedUser() {
        
    }

    public NamedUser(final String username, final String password, final String salt, final UserData userData, final Timestamp currentTime) {
        this.username = username;
        this.password = password;
        this.salt = salt;
        this.userData = userData;
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
    }

	public String getPassword() {
		return password;
	}

	public String getSalt() {
		return salt;
	}

	public UserData getUserData() {
		// TODO Auto-generated method stub
		return userData;
	}

}
