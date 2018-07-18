package com.autonomy.find.api.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Data;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "find.category")
@SequenceGenerator(name = "sequence", sequenceName = "find.category_id_seq")
public class Category {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
	@Column(name = "id")
	private Integer id = null;

	@Column(name = "value")
	private String value = "";

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

}
