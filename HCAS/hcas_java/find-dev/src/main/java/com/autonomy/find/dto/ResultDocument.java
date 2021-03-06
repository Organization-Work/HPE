package com.autonomy.find.dto;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.aci.content.identifier.reference.Reference;
import com.autonomy.find.config.DisplayField;

import lombok.Data;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class ResultDocument implements Comparable<ResultDocument> {

    private static final String LINK_FORMAT = "<a href=\"FindNews://%s\">%s</a>";

    private List<String> companies = new ArrayList<String>();
    private List<String> people = new ArrayList<String>();
    private List<String> places = new ArrayList<String>();
    private List<String> extraTerms = new ArrayList<String>();
    private Map<String, List<Integer>> tagDetails = new HashMap<String, List<Integer>>();
    private Map<String, List<String>> meta = new HashMap<String, List<String>>();
    private Map<String, List<String>> displayFields = new LinkedHashMap<String, List<String>>();
	private String linkId;
	private List<String> links;
    private String database = "";
    private String language = "";
    private String reference = "";
    private String source = "";
    private String summary = "";
    private String title = "";
    private float weight = 0;
    private long date = 0;
    private boolean formatted = false;
    private boolean tagged = false;

    @IdolField("COMPANY")
    public ResultDocument addAgentBoolCompany(final String value) {
        companies.add(value);
        return this;
    }

    public ResultDocument addAgentBoolCompanies(final List<String> value) {
        companies.addAll(value);
        return this;
    }

    @IdolField("PERSON")
    public ResultDocument addAgentBoolPerson(final String value) {
        people.add(value);
        return this;
    }

    public ResultDocument addAgentBoolPeople(final List<String> value) {
        people.addAll(value);
        return this;
    }

    @IdolField("PLACE")
    public ResultDocument addAgentBoolPlace(final String value) {
        places.add(value);
        return this;
    }

    public ResultDocument addAgentBoolPlaces(final List<String> value) {
        places.addAll(value);
        return this;
    }

    @IdolField("autn:database")
    public ResultDocument setDatabase(final String value) {
        this.database = value;
        return this;
    }

    @IdolField("autn:language")
    public ResultDocument setLanguage(final String value) {
        this.language = value;
        return this;
    }

    @IdolField("autn:reference")
    public ResultDocument setReference(final String value) {
        this.reference = value.replaceAll("&", "&amp;");
        return this;
    }

    @IdolField("autn:ref")
    public ResultDocument setRef(final String value) {
        return setReference(value);
    }
    

    
    @IdolField("SOURCE")
    public ResultDocument setNewsSource(final String value) {
        this.source = value;
        return this;
    }

    @IdolField("autn:summary")
    public ResultDocument setSummary(final String value) {
        this.summary = value;
        return this;
    }

    @IdolField("DRECONTENT")
    public ResultDocument setDreContent(final String value) {
        if (this.summary == null || "".equals(this.summary)) {
            setSummary(value);
        }
        return this;
    }

    @IdolField("autn:title")
    public ResultDocument setTitle(final String value) {
        this.title = value;
        return this;
    }

    @IdolField("autn:weight")
    public ResultDocument setWeight(final float value) {
        this.weight = value;
        return this;
    }

    @IdolField("autn:date")
    public ResultDocument setDate(final long value) {
        this.date = value;
        return this;
    }

    @IdolField("DREDATE")
    public ResultDocument setDreDate(final long value) {
        return setDate(value);
    }

    public ResultDocument addExtraTerms(final List<String> value) {
        for (final String term : value) {
            extraTerms.add(term);
        }
        return this;
    }

    public @JsonIgnore
    List<String> getExtraTerms() {
        return this.extraTerms;
    }

    public List<String> getTerms() {
        final Set<String> bool = new HashSet<String>(getExtraTerms());
        //bool.addAll(getPlaces());
        //bool.addAll(getPeople());
        //bool.addAll(getCompanies());
        return new LinkedList<String>(bool);
    }

	private Collection<? extends String> getPlaces() {
		return null;
	}

	private Collection<? extends String> getCompanies() {
		// TODO Auto-generated method stub
		return null;
	}

	private Collection<? extends String> getPeople() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUrl() {
		
		try {
			return StringUtils.isEmpty(reference) || reference.startsWith("http")? reference
						: "viewRecord.do?ref=" + URLEncoder.encode(reference, "UTF-8") + "&db=" + URLEncoder.encode(database, "UTF-8");
		} catch (final UnsupportedEncodingException uee) {
			throw new Error("should never happen", uee);
		}
	}

	public String getReference() {
        return reference;
    }

    @Override
    public int compareTo(final ResultDocument o) {
        return (int) (o.weight * 100 - this.weight * 100);
    }

    @JsonIgnore
    public boolean getFormatted() {
        return this.formatted;
    }

    @JsonIgnore
    public boolean getTagged() {
        return this.tagged;
    }

    public ResultDocument formatSummary() {
        String summary = this.getSummary();

        if (this.formatted || summary.length() == 0) {
            return this;
        }
        this.formatted = true;

        // Remove unwanted groups of characters
        summary = summary.split("\\.{3}", 2)[0];
        summary = summary.trim();
        summary = summary.replaceAll("(\\s*-\\s*)+$", "");

        // Replace unwanted punctuation character at end with dots
        summary = summary.replaceAll("[^\\w\\.\\?\\!]$", "...");

        // Add dots if final character is letter
        String end = summary.substring(summary.length() - 1);
        if (end.matches("\\w")) {
            summary = summary.concat("...");
        }

        this.setSummary(summary);
        return this;
    }

    private String getSummary() {
		return summary;
	}

	public ResultDocument tagSummary() {
        return tagSummary(LINK_FORMAT);
    }

    public ResultDocument tagSummary(final String format) {

        if (this.tagged) {
            return this;
        }
        this.tagged = true;

        // Clear the string of tab characters
        String newSummary = this.getSummary().replaceAll("\\t", "    ");

        // Retrieve the captured agent bool terms for that this
        final List<String> terms = this.getTerms();

        // Insert place holders for each term found
        int i = 0;
        for (final String term : terms) {

            newSummary = newSummary.replaceAll("\\b" + Pattern.quote(term) + "\\b", String.format("<\t%d\t>", i));
            i += 1;
        }

        // Replace the placeholders with the appropriate terms
        i = 0;
        for (final String term : terms) {
            newSummary = newSummary.replaceAll(String.format("<\t%d\t>", i), String.format(format, new Reference(term).toString(), term));
            i += 1;
        }
        this.setSummary(newSummary);

        return this;
    }

    private ResultDocument generateTagMatches() {
        this.tagDetails = generateTagMatchesInText(this.summary, getTerms());
        return this;
    }

    public static Map<String, List<Integer>> generateTagMatchesInText (final String _text, final List<String> _terms) {
        String text = _text;
        final Map<String, List<Integer>> matchMap = new HashMap<String, List<Integer>>();
        final List<String> terms = new LinkedList<String>(_terms);
        Collections.sort(terms, new Comparator<String>() {
            public int compare(final String a, final String b) {
                return b.length() - a.length();
            }
        });
        for (final String term : terms) {
            final List<Integer> termLocations = new LinkedList<Integer>();
            final Pattern quote = Pattern.compile("\\b" + Pattern.quote(term) + "\\b", Pattern.CASE_INSENSITIVE);
            final Matcher matcher = quote.matcher(text);
            while (matcher.find()) {
                termLocations.add(matcher.start());
            }
            text = matcher.replaceAll(StringUtils.repeat(" ", term.length()));
            matchMap.put(term, termLocations);
        }
        return matchMap;
    }

    public static <T extends ResultDocument> Collection<T> formatSummaries(final Collection<T> docs) {
        for (final ResultDocument doc : docs) {
            doc.formatSummary();
        }
        return docs;
    }
    
    public static <T extends ResultDocument> Collection<T> tagSummaries(final Collection<T> docs) {
        for (final ResultDocument doc : docs) {
            doc.tagSummary();
        }
        return docs;
    }

    public static <T extends ResultDocument> Collection<T> tagMatches(final Collection<T> docs) {
        for (final ResultDocument doc : docs) {
            doc.generateTagMatches();
        }
        return docs;
    }
    
    public static <T extends ResultDocument> Collection<T> process(
            final Collection<T> docs,
            final boolean format,
            final boolean tag,
            final boolean generateTagData
    ) {
        if (format) { formatSummaries(docs); }
        if (generateTagData) { tagMatches(docs); }
        if (tag) { tagSummaries(docs); }
        return docs;
    }

    /**
     * Builds a result document from a document.
     *
     * Meta data is a map for detailing which of the extra
     * document fields should be included and what names to
     * give them.
     *
     * @param meta
     * @param doc
     * @return
     */
    public static ResultDocument fromDocument(
            final Map<String, String> meta,
			final Map<String, DisplayField> displayFields,
            final Document doc,
			final String linkIdField,
			final String linkToField,
            final List<String> links
    ) {
        final ResultDocument result = new ResultDocument();

        result.setReference(doc.getReference());
        result.setWeight(doc.getWeight());
        result.setDatabase(doc.getDatabase());
        result.setTitle(doc.getTitle());
        result.setSummary(doc.getSummary());
        result.setDate(doc.getDate().getTime());
        result.setLanguage(doc.getLanguage());
        //result.addAgentBoolCompanies(doc.getDocumentFieldValues("COMPANY"));
        //result.addAgentBoolPeople(doc.getDocumentFieldValues("PERSON"));
        //result.addAgentBoolPlaces(doc.getDocumentFieldValues("PLACE"));
        //result.setSource(doc.getDocumentFieldValue("SOURCE"));

        result.meta = new HashMap<String, List<String>>();

        for (Map.Entry<String, String> pair : meta.entrySet()) {
            result.meta.put(pair.getKey(), doc.getDocumentFieldValues(pair.getValue()));
        }
        if(displayFields != null) {
	        for (Map.Entry<String, DisplayField> pair : displayFields.entrySet()) {
	            final List<String> values = new ArrayList<String>();
	
	            for (final String field : pair.getValue().getFields()) {
	                values.addAll(doc.getDocumentFieldValues(field));
	            }
	
	            result.displayFields.put(pair.getKey(), values);
	        }
        }

		if (StringUtils.isNotBlank(linkIdField)) {
			result.setLinkId(doc.getDocumentFieldValue(linkIdField));
		}

        if (links != null && !links.isEmpty()) {
            result.setLinks(links);
        }
		else if (StringUtils.isNotBlank(linkToField)) {
			result.setLinks(doc.getDocumentFieldValues(linkToField));
		}

        return result;
    }

    private void setLinks(List<String> links2) {
    	this.links=links2;
	}

	private void setLinkId(String documentFieldValue) {
		this.linkId=documentFieldValue;
	}

	/**
     * *Maps* fromDocument(meta, $1) over a list of documents.
     *
     *
	 *
	 * @param meta - The extra meta data to include
	 * @param docs - The documents to *map* over
	 * @param linkIdField - The field which identifies a document (for links)
	 * @param linkToField - The field which contains the linkIdFields a document links to
	 * @return List ResultDocument
     */
    public static List<ResultDocument> fromDocuments(
			final Map<String, String> meta,
			final Map<String, DisplayField> displayFields,
			final List<Document> docs,
			final String linkIdField,
			final String linkToField,
            final List<String> links) {
        final List<ResultDocument> results = new LinkedList<ResultDocument>();
        for (final Document doc : docs) {
            results.add(fromDocument(meta, displayFields, doc, linkIdField, linkToField, links));
        }
        return results;
    }
    
    public static List<ResultDocument> fromDocumentsVertica(
			final Map<String, String> meta,
			final Map<String, DisplayField> displayFields,
			final List<com.autonomy.vertica.query.Document> docs,
			final String linkIdField,
			final String linkToField,
            final List<String> links) {
        final List<ResultDocument> results = new LinkedList<ResultDocument>();
        for (final com.autonomy.vertica.query.Document doc : docs) {
            results.add(fromDocumentVertica(meta, displayFields, doc, linkIdField, linkToField, links));
        }
        return results;
    }
    
    public static ResultDocument fromDocumentVertica(
            final Map<String, String> meta,
			final Map<String, DisplayField> displayFields,
            final com.autonomy.vertica.query.Document doc,
			final String linkIdField,
			final String linkToField,
            final List<String> links
    ) {
        final ResultDocument result = new ResultDocument();

        result.setReference(doc.getReference());
        result.setWeight(doc.getWeight());
        result.setDatabase(doc.getDatabase());
        result.setTitle(doc.getTitle());
        result.setSummary(doc.getSummary());
        result.setDate(doc.getDate().getTime());
        result.setLanguage(doc.getLanguage());
        //result.addAgentBoolCompanies(doc.getDocumentFieldValues("COMPANY"));
        //result.addAgentBoolPeople(doc.getDocumentFieldValues("PERSON"));
        //result.addAgentBoolPlaces(doc.getDocumentFieldValues("PLACE"));
        //result.setSource(doc.getDocumentFieldValue("SOURCE"));

        result.meta = new HashMap<String, List<String>>();

        for (Map.Entry<String, String> pair : meta.entrySet()) {
            result.meta.put(pair.getKey(), doc.getDocumentFieldValues(pair.getValue()));
        }
        if(displayFields != null) {
	        for (Map.Entry<String, DisplayField> pair : displayFields.entrySet()) {
	            final List<String> values = new ArrayList<String>();
	
	            for (final String field : pair.getValue().getFields()) {
	                values.addAll(doc.getDocumentFieldValues(field));
	            }
	
	            result.displayFields.put(pair.getKey(), values);
	        }
        }

		if (StringUtils.isNotBlank(linkIdField)) {
			result.setLinkId(doc.getDocumentFieldValue(linkIdField));
		}

        if (links != null && !links.isEmpty()) {
            result.setLinks(links);
        }
		else if (StringUtils.isNotBlank(linkToField)) {
			result.setLinks(doc.getDocumentFieldValues(linkToField));
		}

        return result;
    }

	public Map<String, List<String>> getDisplayFields() {
		return displayFields;
	}
}