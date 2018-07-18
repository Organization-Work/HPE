package com.autonomy.find.api.database;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;


import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "find.usercategory")
@SequenceGenerator(name = "sequence", sequenceName = "find.usercategory_id_seq")
public class UserCategory {
	
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    @Column(name = "id")
    private Integer id = null;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = UserData.class)
    @JoinColumn(name = "userdata_fk")
    private UserData userData = null;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, targetEntity = Category.class)
    @JoinColumn(name = "category_fk")
    private Category category = null;
    
    @Column(name = "index")
    private Integer index = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;
    
}
