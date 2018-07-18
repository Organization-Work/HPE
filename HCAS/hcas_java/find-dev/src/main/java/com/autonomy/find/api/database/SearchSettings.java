package com.autonomy.find.api.database;

import lombok.Data;

import javax.persistence.*;

import java.io.Serializable;

@NamedQueries({
    @NamedQuery(name = "SearchSettings.getUserSettings",
            query = "from SearchSettings setting where setting.owner = :owner")
})

@Data
@Entity
@Table(name = "find.searchsettings",
       uniqueConstraints = @UniqueConstraint(columnNames={"owner"}))
@SequenceGenerator(name = "sequence", sequenceName = "find.searchsettings_id_seq")
public class SearchSettings implements Serializable {

    @Id
    @Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    private Integer id;

    @Column(name = "owner")
    private String owner;

    @Column(name = "combine")
    private String combine;

    @Column(name = "summary")
    private String summary;

    public SearchSettings() {

    };

    public SearchSettings(final String owner) {
        this.owner = owner;
    }

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary2) {
		this.summary=summary2;
	}

	public void setCombine(String combine2) {
		this.combine=combine2;
	}

	public String getCombine() {
		return combine;
	}

	public Integer getId() {
		return id;
	}

}
