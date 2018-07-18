package com.autonomy.find.api.database;

import java.sql.Timestamp;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
@Entity
@Table(name = "find.clusterterms")
public class ClusterTerms {

    @Id
    @Column(name = "clustertitle")
    private String clusterTitle = null;

    @Column(name = "terms")
    private String terms = null;

    @Column(name = "date_added")
    private Timestamp dateAdded = null;

    @Column(name = "date_modified")
    private Timestamp dateModified = null;

    public ClusterTerms() {
        /* Intentionally empty */
    }

    public ClusterTerms(final String clusterTitle, final Timestamp currentTime, final String... terms) {
        setClusterTitle(clusterTitle).setTerms(terms).initialiseDates(new Timestamp(currentTime.getTime()));
    }

    public ClusterTerms(final String clusterTitle, final Timestamp currentTime, final Collection<String> terms) {
        setClusterTitle(clusterTitle).setTerms(terms).initialiseDates(new Timestamp(currentTime.getTime()));
    }

    public ClusterTerms setClusterTitle(final String value) {
        this.clusterTitle = value;
        return this;
    }

    public ClusterTerms setTerms(final String... value) {
        this.terms = StringUtils.join(value, " ");
        return this;
    }

    public ClusterTerms setTerms(final Collection<String> value) {
        this.terms = StringUtils.join(value, " ");
        return this;
    }

    private ClusterTerms initialiseDates(final Timestamp currentTime) {
        this.dateAdded = new Timestamp(currentTime.getTime());
        this.dateModified = new Timestamp(currentTime.getTime());
        return this;
    }

	public String getTerms() {
		return terms;
	}

	public String getClusterTitle() {		// TODO Auto-generated method stub
		return clusterTitle;
	}
}
