package com.autonomy.find.api.database;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: nathan
 * Date: 20/03/14
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "idol_database", catalog = "find")
@SequenceGenerator(name = "sequence", sequenceName = "userdata_id_seq")
public class IdolDatabase implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    private int id;
    private String Name;
    private Timestamp date_added;
    private Timestamp date_modified;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
