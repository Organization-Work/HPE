package com.autonomy.find.services;

import static autn.voronoi.DateUtil.formatDate;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import autn.voronoi.VoronoiProxy;

import com.autonomy.aci.actions.DontCareAsLongAsItsNotAnErrorProcessor;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.database.Databases;
import com.autonomy.aci.content.identifier.reference.Reference;
import com.autonomy.common.io.IOUtils;
import com.autonomy.find.api.database.ClusterTerms;
import com.autonomy.find.api.exceptions.DocumentNotFoundException;
import com.autonomy.find.api.exceptions.TermsNotFoundException;
import com.autonomy.find.config.FindConfig;
import com.autonomy.find.config.FindNewsConfig;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.CategoryDetail;
import com.autonomy.find.dto.ClusterName;
import com.autonomy.find.dto.NewsResult;
import com.autonomy.find.dto.NewsResultCluster;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.util.TimeHelper;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;

@Service
public class NewsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsService.class);

    private static final String HEADLINE = "headline",
    		CLUSTER_SHOW_JOBS = "ClusterShowJobs",
    		GET_QUERY_TAG_VALUES = "getquerytagvalues",
    		QUERY = "query",
    		TEXT = "text",
    		FIELD_NAME = "fieldname",
    		AUTN_DATE = "autn_date", 
    		DATE_PERIOD = "dateperiod", 
    		DAY = "day", 
    		DOCUMENT_COUNT = "documentcount", 
    		SORT = "sort", 
    		REVERSE_DATE = "reversedate", 
    		DATABASE_MATCH = "databasematch",
    		MATCH_REFERENCE = "matchreference", 
    		MIN_SCORE = "minscore",
    		MAX_SCORE = "maxscore", 
    		MAX_DATE = "maxdate",
            MIN_DATE = "mindate", START = "start", 
            MAX_RESULTS = "maxresults", 
            SUMMARY = "summary", 
            CONTEXT = "context";

    private static final float CLUSTERS_MINSCORE = 60;

    private static final int DEFAULT_DAYS = 7;

    private static final int DAY_LENGTH = 3600 * 24;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FindNewsConfig config;

    @Autowired
    private FindConfig findConfig;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    @Qualifier("searchAciService")
    private AciService searchAciService;

	@Autowired
	@Qualifier("category.databases")
	private Databases categoryDREDatabases;

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityAci;

    @Autowired
    @Qualifier("categoryAciService")
    private AciService categoryAci;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private UserService userService;

    @Autowired
    private TimeHelper timeHelper;
    
    @Autowired
    private SearchService searchService;

    @Autowired
    private SearchConfig searchConfig;

    @Autowired
    private VoronoiProxy voronoi;

    final private long AUTO_REFRESH_INTERVAL = 600000L; // 10 minutes

    /**
     * Retrieve the currently active categories
     *
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Cacheable(cacheName = "NewsService.categories",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public List<ClusterName> getCategories() throws JsonParseException,
            JsonMappingException, IOException {
        if (!findConfig.showingNews()) { return new LinkedList<>(); }
        // Retrieve the available jobs
        final List<ClusterName> clusterJobs = categoryAci.executeAction(
                new AciParameters(CLUSTER_SHOW_JOBS),
                processorFactory.listProcessorForClass(ClusterName.class));

        // Get the cluster job->name map
        final Map<String, CategoryDetail> categoryDetails = getCategoryDetails();

        final List<ClusterName> results = new LinkedList<ClusterName>();

        // Filter and map the names
        for (final ClusterName clusterName : clusterJobs) {
            final String name = clusterName.getName();
            if (categoryDetails.containsKey(name)) {
                final CategoryDetail category = categoryDetails.get(name);
                clusterName.setDisplayName(category.getDisplayName());
                results.add(clusterName);
            }
        }

        return results;
    }

    /**
     * Loads the cluster names file and returns the JSON parsed version as a Map
     *
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    private Map<String, CategoryDetail> getCategoryDetails()
            throws JsonParseException, JsonMappingException, IOException {
        return (mapper.readValue(loadCategoryDetailsJSON(),
                new TypeReference<HashMap<String, CategoryDetail>>() {
                }));
    }

    /**
     * Loads the cluster names JSON file using the file specified in the category
     * config
     *
     * @return
     * @throws IOException
     */
    private String loadCategoryDetailsJSON() throws IOException {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(
                config.getClusterNamesFile()));
    }


    public Map<String,Object> queryTopicDocs(
            final List<String> query,
            final int pageSize,
            final int pageNum,
            final int daysBack
    ) {
        try {
            return voronoi.query(pageNum, pageSize, null,
                    query.toArray(new String[]{}), false, false, 0,
                    timeHelper.calculateDaysAgo(daysBack), null, false, 60.0);
        } catch (XPathExpressionException e) {
            return new HashMap<>();
        }
    }


    /**
     * @param types
     * @param categories
     * @param maxResults
     * @param headlines
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    @Cacheable(cacheName = "NewsService.news",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public List<NewsResult> getNews(final List<String> types,
                                    final List<String> categories, final int maxResults,
                                    final boolean headlines, final Long startDate, final Long endDate,
                                    final Long interval, final boolean cacheTerms) throws JsonParseException,
            JsonMappingException, IOException {

        if (!findConfig.showingNews()) { return new LinkedList<>(); }
        final Map<String, CategoryDetail> categoryDetails = getCategoryDetails();
        final List<NewsResult> results = new LinkedList<NewsResult>();
        final int clusterMaxResults = maxResults * (headlines ? 2 : 1);

        final List<String> actualCategories = categories.isEmpty() ? getAllCategories(categoryDetails) : categories;

        // For each type
        for (final String type : types) {
            // Generate a news result for that type
            final NewsResult newsResult = new NewsResult(type);
            // Retrieve the news using that type
            getNewsForType(actualCategories, categoryDetails, clusterMaxResults,
                    startDate, endDate, interval, newsResult);
            // Store the results
            results.add(newsResult);
        }

        // If a headline type is requested
        if (headlines) {
            calculateHeadlines(results, maxResults);
        }

        if (cacheTerms) {
            cacheResultTerms(results, false);
        }
        
        return results;
    }

    /**
     * @param results
     * @param forceRefresh
     */
    private void cacheResultTerms(final List<NewsResult> results,
                                  final boolean forceRefresh) {
        final Set<String> titles = getTermCacheTitles();

        for (final NewsResult result : results) {
            for (final NewsResultCluster cluster : result.getClusters()) {
                final String clusterTitle = cluster.getTitle();
                if (forceRefresh || !titles.contains(clusterTitle)) {

                    // Open a connection
                    final Session db = sessionFactory.openSession();
                    db.beginTransaction();

                    // Submit the cluster term for addition/update
                    db.saveOrUpdate(new ClusterTerms(clusterTitle, timeHelper.getCurrentTimestamp(), cluster.getTerms()));

                    // Commit and Close the connection
                    db.getTransaction().commit();
                    db.close();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Cacheable(cacheName = "NewsService.cluster_terms",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public String getTermsForClusterTitle(final String title)
            throws TermsNotFoundException {
        if (!findConfig.showingNews()) { return null; }
        final List<ClusterTerms> maybeClusterTerms;
        final Session db = sessionFactory.openSession();
        try {
            maybeClusterTerms = (List<ClusterTerms>) db
                    .createQuery(" from ClusterTerms where clustertitle = :title ")
                    .setString("title", title).list();
        } catch (final Exception e) {
            LOGGER.error("Failed to retrieve cluster terms", e);
            return "";
        } finally {
            db.close();
        }
        if (maybeClusterTerms.isEmpty()) {
            throw new TermsNotFoundException();
        }
        return maybeClusterTerms.get(0).getTerms();
    }

    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<ClusterTerms> getTermCache() {

        final Session db = sessionFactory.openSession();
        try {
            return (List<ClusterTerms>) db.createQuery(" from ClusterTerms ").list();
        } catch (final Exception e) {
            LOGGER.error("Failed to retrieve cluster terms", e);
            return new LinkedList<ClusterTerms>();
        } finally {
            db.close();
        }
    }

    /**
     * @return
     */
    @Cacheable(cacheName = "NewsService.term_cache_titles",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public Set<String> getTermCacheTitles() {
        if (!findConfig.showingNews()) { return new LinkedHashSet<>(); }

        final HashSet<String> termSet = new HashSet<String>();

        for (final ClusterTerms term : getTermCache()) {
            termSet.add(term.getClusterTitle());
        }

        return termSet;
    }

    /**
     * @param categoryDetails
     * @return
     */
    private List<String> getAllCategories(
            final Map<String, CategoryDetail> categoryDetails) {
        return new LinkedList<String>(categoryDetails.keySet());
    }

    /**
     * @param results
     */
    private void calculateHeadlines(final List<NewsResult> results,
                                    final int maxResults) {

        final NewsResult headline = new NewsResult(HEADLINE);
        final List<NewsResultCluster> headlineClusters = new LinkedList<NewsResultCluster>();
        final Set<String> clusterTitles = new HashSet<String>();

        process:
        for (final NewsResult news : results) {
            for (final NewsResultCluster cluster : news.getClusters()) {
                if (headlineClusters.size() > maxResults) {
                    break process;
                }
                if (clusterTitles.contains(cluster.getTitle())) {
                    headlineClusters.add(cluster);
                } else {
                    clusterTitles.add(cluster.getTitle());
                }
            }
        }

        // Remove the headline clusters from the popular/breaking
        for (final NewsResultCluster cluster : headlineClusters) {
            for (final NewsResult news : results) {
                news.getClusters().remove(cluster);
            }
        }

        headline.addClusters(headlineClusters);
        results.add(0, headline);
    }

    /**
     * @param categories
     * @param categoryDetails
     * @param clusterMaxResults
     * @param newsResult
     */
    private void getNewsForType(final List<String> categories,
                                final Map<String, CategoryDetail> categoryDetails,
                                final int clusterMaxResults, final Long startDate, final Long endDate,
                                final Long interval, final NewsResult newsResult) {

        // For each category name
        for (final String categoryName : categories) {
            if (categoryDetails.containsKey(categoryName)) {

                // Look up the category associated with that name
                final CategoryDetail category = categoryDetails.get(categoryName);

                // If the category has that type
                if (category.hasJob(newsResult.getType())) {
                    final String job = category.getJob(newsResult.getType());
                    try {
                        newsResult.addClusters(getClusterResults(categoryName, 
                                job, clusterMaxResults, startDate, endDate, interval));
                    } catch (final AciErrorException e) {
                        LOGGER.error("Failed to retrieve cluster results", e);
                        /* Intentionally empty */
                    }
                }
            }
        }
    }

    /**
     * @param job
     * @param clusterMaxResults
     * @return
     */
    private List<NewsResultCluster> getClusterResults(final String categoryID,
                                                      final String job,
                                                      final int clusterMaxResults, final Long startDate, final Long endDate,
                                                      final Long interval) throws AciErrorException {

        final AciParameters params = new AciParameters("ClusterResults");
        params.add("SourceJobName", job);
        params.add("NumClusters", clusterMaxResults);
        params.add("NumResults", config.getNumResults());
        params.add("DREAnyLanguage", config.getClusterAnyLanguage());
        params.add("DREOutputEncoding", config.getClusterOutputEncoding());
        params.add("maxterms", 10);
        params.add("XMLMeta", true);

        if (startDate != null) {
            params.add("startDate", startDate);
        }
        if (endDate != null) {
            params.add("endDate", endDate);
        }
        if (interval != null) {
            params.add("interval", interval);
        }

        final List<NewsResultCluster> results = categoryAci.executeAction(params,
                processorFactory.listProcessorForClass(NewsResultCluster.class));

        for (final NewsResultCluster result : results) {
            final List<String> clusterTitleTerms = new LinkedList<String>(Arrays.asList(result.getTitle().split("\\s*,\\s*")));
            result.setDocuments(requestMoreDataForDocuments(result.getDocuments()));
            for (final ResultDocument doc : result.getDocuments()) {
                doc.addExtraTerms(clusterTitleTerms);
            }
            result.setCategory(categoryID);
        }
        
        return results;
    }

    private List<ResultDocument> requestMoreDataForDocuments(final List<ResultDocument> docs) {
    	final List<ResultDocument> documents = new LinkedList<ResultDocument>();
    	for (final ResultDocument doc : docs) {
    		documents.add(requestMoreDataForDocument(doc));
    	}
    	return documents;
    }
    
    private ResultDocument requestMoreDataForDocument(final ResultDocument doc) {
    	try {
	        return documentDataForReference(doc.getReference());
        }
        catch (DocumentNotFoundException e) {
        	return doc;
        }
    	
    }
    
    /**
     * 
	 * @param reference
	 * @return
     */
    final ResultDocument documentDataForReference(final String reference) throws DocumentNotFoundException {
      final AciParameters params = new AciParameters("GETCONTENT");
      params.add("reference", new Reference(reference));
	  params.add("databasematch", categoryDREDatabases);

      try {
          final List<ResultDocument> result = searchAciService.executeAction(params, processorFactory.listProcessorForClass(ResultDocument.class));
          return result.get(0);
      }
      catch (final Exception e) {
          throw new DocumentNotFoundException("With reference: %s", reference);
      }
    }
    
    private String documentsToReference(final List<String> documents) {

        final StringBuilder reference = new StringBuilder();
        boolean first = true;
        for (final String document : documents) {
            if (!first) {
                reference.append("+");
            }

            reference.append(new Reference(document).toString());
            first = false;
        }
        return reference.toString();
    }

    /**
     * @param username
     * @param documents
     */
    public void addProfileDocuments(
            final String username,
            final List<String> documents
    ) throws ProcessorException {

        final AciParameters params = new AciParameters("ProfileUser");
        params.add("UserName", username);
        params.add("Mode", "Reference");
        params.add("Document", documentsToReference(documents));

        communityAci.executeAction(params,
                new DontCareAsLongAsItsNotAnErrorProcessor());
    }

    /**
     * 
     * @param clusterTitle
     * @param endDate
     * @param _days
     * @return
     * @throws TermsNotFoundException
     */
    @Cacheable(cacheName = "NewsService.theme_timeline",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL   //cache is automatically refreshed every 10 minutes
    )    
    public List<DateTag> getThemeTimeline(final String clusterTitle, Long endDate, Integer _days) throws TermsNotFoundException {
        // http://10.2.1.91:9002/action=getquerytagvalues&fieldname=autn_date&dateperiod=day&documentcount=true&sort=reversedate&mindate=-29&databasematch=moreover&text=syria

        if (!findConfig.showingNews()) { return new LinkedList<>(); }
        final String clusterMetaTerms = getTermsForClusterTitle(clusterTitle);
        return getTimeline(clusterMetaTerms, endDate, _days);
    }
    
    /**
     * 
     * @param queryText
     * @param endDate
     * @param _days
     * @return
     */
    //@Cacheable(cacheName="NewsService.search_timeline", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public List<DateTag> getSearchTimeline(final String queryText, Long endDate, Integer _days) {

    	final String concepts = StringUtils.join(searchService.getRelatedConcepts(queryText), " ");
    	return getTimeline(concepts, endDate, _days);
    }
    
    private List<DateTag> getTimeline(final String text, Long endDate, Integer _days) {
    	// http://10.2.1.91:9002/action=getquerytagvalues&fieldname=autn_date&dateperiod=day&documentcount=true&sort=reversedate&mindate=-29&databasematch=moreover&text=syria
    	
        final int days = (_days == null) ? DEFAULT_DAYS : _days;
        final long endDateEpoch = endDate == null ? new Date().getTime() / 1000 : endDate;

        final AciParameters params = new AciParameters(GET_QUERY_TAG_VALUES);
        params.add(FIELD_NAME, AUTN_DATE);
        params.add(DATE_PERIOD, DAY);
        params.add(DOCUMENT_COUNT, true);
        params.add(DATABASE_MATCH, categoryDREDatabases);
        params.add(MIN_SCORE, CLUSTERS_MINSCORE);
        params.add(SORT, REVERSE_DATE);
        params.add(MAX_DATE, formatDate(endDateEpoch));
        params.add(MIN_DATE, formatDate(endDateEpoch - days * DAY_LENGTH));
        params.add(TEXT, text);

        return searchAciService.executeAction(params, new DateTagProcessor());
    }

    public List<Integer> getThemeTimelineCountList(final String clusterTitle, final Long endDate, final Integer _days) throws TermsNotFoundException {

        final int days = (_days == null) ? DEFAULT_DAYS : _days;
        final long endDateEpoch = (endDate == null) ? (new Date().getTime() / 1000) : endDate;
        final List<DateTag> dateTags = getThemeTimeline(clusterTitle, endDateEpoch, days);

        return getTimelineCountList(dateTags, days, endDateEpoch);
    }
    
    public List<Integer> getSearchTimelineCountList(final String queryText, final Long endDate, final Integer _days) {

        final int days = (_days == null) ? DEFAULT_DAYS : _days;
        final long endDateEpoch = (endDate == null) ? (new Date().getTime() / 1000) : endDate;
        final List<DateTag> dateTags = getSearchTimeline(queryText, endDateEpoch, days);
        
        return getTimelineCountList(dateTags, days, endDateEpoch);
    }

	private List<Integer> getTimelineCountList(final List<DateTag> dateTags,
			final int days, final long endDateEpoch) {
		final long startDateEpoch = (endDateEpoch - days * DAY_LENGTH);
        final List<Integer> results = new ArrayList<Integer>();
        
        // Pre-populate the results with zeros
        for (int index = 0; index < days; index += 1) {
            results.add(0);
        }

        // For all the results we did get
        for (final DateTag dateTag : dateTags) {
            // Calculate where the entry should occur in the results array
            final int index = (int) ((dateTag.epoch - startDateEpoch) / DAY_LENGTH);
            if (index >= 0 || index < days) {
                results.set(index, dateTag.count);
            }
        }

        return results;
	}

    public Map<String, Object> themeTimelineDocs(TimelineDocRequest docRequest) {
        // if it's too slow to send the terms, consider sending the cluster
        // details
        // instead,
        // at the cost of having to do another query
        final AciParameters params = new AciParameters(QUERY);
        final int skip = docRequest.pageNum * docRequest.pageSize;
        params.add(START, 1 + skip);
        params.add(MAX_RESULTS, skip + docRequest.pageSize);
        params.add(TEXT, StringUtils.join(docRequest.terms, " "));
        params.add(DATABASE_MATCH, categoryDREDatabases);
        params.add(MIN_SCORE, CLUSTERS_MINSCORE);
        params.add(MAX_DATE, formatDate(docRequest.endEpoch));
        params.add(MIN_DATE, formatDate(docRequest.startEpoch));
        params.add(SUMMARY, CONTEXT);

		final List<ClusterDoc> docs = searchAciService.executeAction(params,
				processorFactory.listProcessorForClass(ClusterDoc.class));

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("docs", docs);
        return map;
    }

    private ClusterSGDocs getSGDocs(final DCluster cluster, final int numresults) {
        final AciParameters params = new AciParameters("ClusterSGDocsServe");
        params.add("startdate", cluster.fromDate);
        params.add("enddate", cluster.toDate - 1);
        params.add("sourcejobname", cluster.jobName);
        params.add("cluster", cluster.id);
        // there don't seem to be print options etc
        // there's a numresults parameter, but it doesn't seem to work (sets the
        // autn:numdocs but doesn't trim the response)
        params.add("NumResults", numresults);

        return searchAciService.executeAction(params, new ClusterDocsProcessor());
    }

	@IdolDocument("autn:hit")
    public static class ClusterDoc {
        public String title, ref, summary;
        public float score;

        ClusterDoc() {
        }

        ClusterDoc(String title, String ref, String summary, float score) {
            this.title = title;
            this.ref = ref;
            this.summary = summary;
            this.score = score;
        }

		@IdolField("autn:title")
		public void setTitle(final String title) {
			this.title = title;
		}

		@IdolField("autn:reference")
		public void setRef(final String ref) {
			this.ref = ref;
		}

		@IdolField("autn:summary")
		public void setSummary(final String summary) {
			this.summary = summary;
		}

		@IdolField("autn:score")
		public void setScore(final float score) {
			this.score = score;
		}
	}

    public static final class DCluster {
        public String title;
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public String jobName;
        public long fromDate, toDate;
        public int numDocs, x1, x2, y1, y2, id;

        DCluster() {
        }

        DCluster(String title, long fromDate, long toDate, int numDocs, int x1,
                 int x2, int y1, int y2, int id) {
            this.title = title;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.numDocs = numDocs;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
            this.id = id;
        }
    }

    static class ClusterSGDocs {
        public List<ClusterDoc> docs;
        public String title;
        public List<String> terms;

        ClusterSGDocs(List<ClusterDoc> docs, String title, List<String> terms) {
            this.docs = docs;
            this.title = title;
            this.terms = terms;
        }
    }

    static class TimelineDocRequest {
        public List<String> terms;
        public long startEpoch, endEpoch;
        public int pageNum = 0, pageSize = 6;
    }

    static final class ClusterDocsProcessor extends
            AbstractStAXProcessor<ClusterSGDocs> {
        ClusterDoc processCluster(XMLStreamReader xmlStreamReader)
                throws XMLStreamException {
            String title = "", ref = "", summary = "";
            int score = 0;

            while (xmlStreamReader.hasNext()) {
                final int evtType = xmlStreamReader.next();
                switch (evtType) {
                    case XMLEvent.START_ELEMENT:
                        final String localName = xmlStreamReader.getLocalName();
                        if ("autn:doc".equals(localName)) {
                            score = 0;
                            title = ref = summary = null;
                        } else if ("autn:title".equals(localName)) {
                            title = xmlStreamReader.getElementText();
                        } else if ("autn:ref".equals(localName)) {
                            ref = xmlStreamReader.getElementText();
                        } else if ("autn:summary".equals(localName)) {
                            summary = xmlStreamReader.getElementText();
                        } else if ("autn:score".equals(localName)) {
                            score = Integer.parseInt(xmlStreamReader.getElementText());
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        if ("autn:doc".equals(xmlStreamReader.getLocalName())) {
                            return new ClusterDoc(title, ref, summary, score);
                        }
                        break;
                }
            }

            throw new ProcessorException("Did not find a closing autn:doc");
        }

        @Override
        public ClusterSGDocs process(XMLStreamReader xmlStreamReader) {
            try {
                final List<ClusterDoc> clusters = new ArrayList<ClusterDoc>();
                final List<String> terms = new ArrayList<String>();
                String title = null, localName = null;
                boolean inAutnDocs = false;

                while (xmlStreamReader.hasNext()) {
                    final int evtType = xmlStreamReader.next();
                    switch (evtType) {
                        case XMLEvent.START_ELEMENT:
                            localName = xmlStreamReader.getLocalName();
                            if ("autn:docs".equals(localName)) {
                                inAutnDocs = true;
                            } else if (inAutnDocs && "autn:doc".equals(localName)) {
                                clusters.add(processCluster(xmlStreamReader));
                            } else if ("autn:term".equals(localName)) {
                                terms.add(xmlStreamReader.getElementText());
                            } else if (title == null && "autn:title".equals(localName)) {
                                title = xmlStreamReader.getElementText();
                            }
                            break;
                        case XMLEvent.END_ELEMENT:
                            localName = xmlStreamReader.getLocalName();
                            if ("autn:docs".equals(localName)) {
                                inAutnDocs = false;
                            } else if ("autn:cluster".equals(localName)) {
                                return new ClusterSGDocs(clusters, title, terms);
                            }
                            break;
                    }
                }

                throw new ProcessorException("Did not find a closing autn:cluster");
            } catch (XMLStreamException e) {
                throw new ProcessorException("Error reading XML", e);
            }
        }
    }

    public static class DateTag {
        public long epoch;
        public int count;

        DateTag() {
        }

        DateTag(long epoch, int count) {
            this.epoch = epoch;
            this.count = count;
        }
    }

    static final class DateTagProcessor extends
            AbstractStAXProcessor<List<DateTag>> {

		private final SimpleDateFormat format;

		{
//			We need to parse the date string (always UTC) since the value is an autn:date, which isn't always epoch seconds
//			e.g. for pre-epoch values
//			<autn:value date="23:00:00 30/03/1970">7686000</autn:value>
//			<autn:value date="23:00:00 28/02/1970">5094000</autn:value>
//			<autn:value date="00:00:00 01/12/1969">4294857120</autn:value>
			this.format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
			this.format.setTimeZone(TimeZone.getTimeZone("UTC"));
		}

        @Override
        public List<DateTag> process(XMLStreamReader xmlStreamReader) {
            try {
                final List<DateTag> values = new ArrayList<DateTag>();

                while (xmlStreamReader.hasNext()) {
                    final int evtType = xmlStreamReader.next();
                    switch (evtType) {
                        case XMLEvent.START_ELEMENT:
                            final String localName = xmlStreamReader.getLocalName();
                            if ("autn:value".equals(localName)) {
                                final int count = Integer.parseInt(xmlStreamReader
                                        .getAttributeValue(null, "count"));
								final String date = xmlStreamReader.getAttributeValue(null, "date");

								try {
									final long epoch = Math.round(format.parse(date).getTime() / 1000);
									values.add(new DateTag(epoch, count));
								} catch (ParseException e) {
									throw new ProcessorException(String.format("Invalid date '%s'", date));
								}
                            }
                            break;
                    }
                }

                return values;
            } catch (XMLStreamException e) {
                throw new ProcessorException("Error reading XML", e);
            }
        }
    }
}
