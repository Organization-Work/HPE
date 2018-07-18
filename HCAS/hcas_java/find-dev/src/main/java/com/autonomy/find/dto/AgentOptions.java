package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.find.util.CollUtils;

import lombok.Data;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@IdolDocument("autn:agent")
public class AgentOptions {

	private static final Logger LOGGER = LoggerFactory.getLogger(AgentOptions.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String XML_AID = "autn:aid";
    private static final String XML_NAME = "autn:agentname";
    private static final String XML_CONCEPTS = "find_concepts";
    private static final String XML_DOCUMENTS = "autn:reference";
    private static final String XML_UNREADONLY = "find_unreadonly";
    private static final String XML_DATABASES = "find_databases";
    private static final String XML_STARTDATE = "find_startdate";
	private static final String XML_FILTERS = "find_filters";
	private static final String XML_DISPCHARS = "find_dispchars";
	private static final String XML_MINSCORE = "find_minscore";
    private static final String XML_CATEGORY_ID = "find_category_id";

    private String aid;
    private String name;
    @JsonIgnore
    private String newName;
	// defaults to true so legacy agents will retain unreadOnly behaviour
	private boolean unreadOnly = true;
    private List<String> concepts = new LinkedList<String>();
    private List<String> documents = new LinkedList<String>();
    private List<String> databases = new LinkedList<String>();
    private Long startDate;
	private Map<String, List<String>> filters = new LinkedHashMap<>();
	private Double minScore;
	private Integer dispChars;
    private String categoryId;
    @JsonIgnore
    private List<String> queryExtension = new LinkedList<>();

    public AgentOptions() {
    /* Constructor for XML Processor */
    }

    public AgentOptions(
			final boolean unreadOnly,
			final String aid,
			final String name,
			final String newName,
			final List<String> concepts,
			final List<String> documents,
			final List<String> databases,
			final Long startDate,
			final Map<String, List<String>> filters,
			final Double minScore,
			final Integer dispChars,
            final String categoryId
	) {
		this.unreadOnly = unreadOnly;
        this.aid = aid;
        this.name = name;
        this.newName = newName;
        this.concepts = concepts;
        this.documents = documents;
        this.databases = databases;
        this.startDate = startDate;
		this.filters = filters;
		this.minScore = minScore;
		this.dispChars = dispChars;
        this.categoryId = categoryId;
	}

    @JsonIgnore
    public String getTraining() {
        final List<String> additions = new LinkedList<>(this.queryExtension);
        additions.addAll(this.concepts);
        return additions.isEmpty() ? ""
            : "(" + CollUtils.intersperse(") AND (", additions) + ")";
    }

    public boolean hasCategoryId() {
        return StringUtils.isNotBlank(this.categoryId);
    }

    public boolean hasAid() {
        return (this.aid != null);
    }

    public boolean hasName() {
        return (this.name != null);
    }

    public boolean hasNewName() {
        return (this.newName != null);
    }

    public boolean hasConcepts() {
        return hasList(this.concepts);
    }

    public boolean hasDocuments() {
        return hasList(this.documents);
    }

    public boolean hasDatabases() {
        return hasList(this.databases);
    }

    public boolean hasStartDate() {
        return (this.startDate != null);
    }

    public boolean hasTraining() {
        return hasConcepts();
    }

    private static <A> boolean hasList(final List<A> list) {
        return !(list == null || list.isEmpty());
    }

    @IdolField(XML_AID)
    public AgentOptions setAID(final String aid) {
        this.aid = aid;
        return this;
    }

    @IdolField(XML_NAME)
    public AgentOptions setName(final String name) {
        this.name = name;
        return this;
    }

    @IdolField(XML_STARTDATE)
    public AgentOptions setStartDate(final Long startDate) {
        this.startDate = startDate;
        return this;
    }

    @IdolField(XML_DOCUMENTS)
    public AgentOptions addDocument(final String document) {
        this.documents.add(document);
        return this;
    }

    @IdolField(XML_DATABASES)
    public AgentOptions setDatabases(final String databasesJSON) {
        try {
            this.databases = mapper.readValue(databasesJSON, this.databases.getClass());
        }
        catch (final Exception e) {
			LOGGER.error("Error parsing databases", e);
            this.databases = new LinkedList<>();
        }
        return this;
    }

    @IdolField(XML_CONCEPTS)
    public AgentOptions setTraining(final String trainingJSON) {
        try {
            this.concepts = mapper.readValue(trainingJSON, this.concepts.getClass());
        } catch (final Exception e) {
			LOGGER.error("Error parsing training", e);
            this.concepts = new LinkedList<>();
        }
        return this;
    }

	@IdolField(XML_FILTERS)
	public AgentOptions setFilters(final String filtersJSON) {
		try {
			this.filters = mapper.readValue(filtersJSON, this.filters.getClass());
		} catch (final Exception e) {
			LOGGER.error("Error parsing filters", e);
			this.filters = new LinkedHashMap<>();
		}
		return this;
	}


	@IdolField(XML_UNREADONLY)
	public AgentOptions setUnreadOnly(final String unreadOnly){
		this.unreadOnly = Boolean.valueOf(unreadOnly);
		return this;
	}

	@IdolField(XML_MINSCORE)
	public AgentOptions setMinScore(final String minScore){
		this.minScore = NumberUtils.toDouble(minScore);
		return this;
	}

	@IdolField(XML_DISPCHARS)
	public AgentOptions setDispChars(final String dispChars){
		this.dispChars = NumberUtils.toInt(dispChars);
		return this;
	}

    @IdolField(XML_CATEGORY_ID)
    public AgentOptions setCategoryId(final String categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public AgentOptions addQueryExtension(final String ext) {
        this.queryExtension.add(ext);
        return this;
    }

    public AgentOptions addConcept(final String concept) {
        this.concepts.add(concept);
        return this;
    }

    public AgentOptions addConcepts(final List<String> concepts) {
        this.concepts.addAll(concepts);
        return this;
    }

    public AgentOptions addDocuments(final List<String> documents) {
        this.documents.addAll(documents);
        return this;
    }

    public AgentOptions addDatabase(final String database) {
        this.databases.add(database);
        return this;
    }

    public AgentOptions addDatabases(final List<String> databases) {
        this.databases.addAll(databases);
        return this;
    }

    public AgentOptions setNewName(final String newName) {
        this.newName = newName;
        return this;
    }

    public AgentOptions setConcepts(final List<String> concepts) {
        this.concepts = concepts;
        return this;
    }

    public AgentOptions setDocuments(final List<String> documents) {
        this.documents = documents;
        return this;
    }

    public AgentOptions setDatabases(final List<String> databases) {
        this.databases = databases;
        return this;
    }

	public String getName() {
		return name;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public Object getAid() {
		return aid;
	}

	public Double getMinScore() {
		return minScore;
	}

	public List<String>  getDatabases() {
		return databases;
	}

	public Long getStartDate() {
		return startDate;
	}

	public List<String> getConcepts() {
		return concepts;
	}

	public Map<String, List<String>> getFilters() {
		return filters;
	}

	public boolean isUnreadOnly() {
		return unreadOnly;
	}

	public Integer getDispChars() {
		return dispChars;
	}

	public List<String> getDocuments() {
		return documents;
	}

	public Object getNewName() {
		return newName;
	}

}
