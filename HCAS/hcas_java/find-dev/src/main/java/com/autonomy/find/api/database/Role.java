package com.autonomy.find.api.database;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;


@Entity
@Table(name = "role", catalog = "find")
@SequenceGenerator(name = "sequence", sequenceName = "userdata_id_seq")
public class Role implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    private Integer id;
    private String Name;
    private Timestamp date_added;
    private Timestamp date_modified;


   public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Timestamp getDate_added() {
        return date_added;
    }

    public void setDate_added(Timestamp date_added) {
        this.date_added = date_added;
    }

    public Timestamp getDate_modified() {
        return date_modified;
    }

    public void setDate_modified(Timestamp date_modified) {
        this.date_modified = date_modified;
    }


    public Role() {
        Date date = new Date();
        Timestamp currentTime = new Timestamp(date.getTime());
        this.setDate_added(new Timestamp(currentTime.getTime()));
        this.setDate_modified(new Timestamp(currentTime.getTime()));
    }

}
