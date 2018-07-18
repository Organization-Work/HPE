package com.autonomy.find.api.database;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "find.userdata")
@SequenceGenerator(name = "sequence", sequenceName = "find.userdata_id_seq")
public class UserData implements Serializable {
	
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    @Column(name = "id")
    private Integer id = null;
    
//    @Column(name = "searchDBs")
//    private String[] searchDBs = new String[]{};
//    
//    @Column(name = "suggestionDBs")
//    private String[] suggestionDBs = new String[]{};
//    
//    @Column(name = "displayChars")
//    private Integer displayChars = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    @Column(name = "active")
    private boolean active = true;


    public UserData() {

    }

    public UserData(final Timestamp currentTime) {
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
    }
    
    public Integer getId() {
    	return id;
    }

	public void setActive(boolean b) {
		this.active=b;
	}
    
}