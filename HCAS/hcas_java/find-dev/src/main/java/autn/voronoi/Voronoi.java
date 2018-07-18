package autn.voronoi;

import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.actions.idol.term.TermExpandProcessor;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.AciServiceException;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.AciServiceImpl;
import com.autonomy.aci.client.services.impl.DocumentProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.database.Databases;
import com.autonomy.aci.content.identifier.reference.ReferencesBuilder;
import com.autonomy.aci.content.printfields.PrintFields;
import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.*;
import java.io.IOException;
import java.util.*;

import static autn.voronoi.DateUtil.formatDate;

@Service
public class Voronoi implements VoronoiProxy {
    private final XPath xPath = XPathFactory.newInstance().newXPath();
    private final XPathExpression xElement, xTotalHits, xTitle, xHit, xReference, xSummary, xDREContent, xFields,
            xValue, xUser, xProfile, xProfileHit, xPid, xCreated;

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityAciService;

    @Autowired
    @Qualifier("communityAgentStoreAciService")
    private AciService communityAgentStoreAciService;

    @Autowired
    @Qualifier("communityDatabase")
    private Databases communityDatabase;

    public static class Engine {
        AciService aciService;
        Databases databases;
        int aqgResults;
        String sentimentField;
        String combine;
        XPathExpression xPositivity;
        private String readerTpl;
        private String[] docFields = new String[]{};
        private Set<String> docFieldsSet;

        public Engine(final AciServiceImpl aciService, final Databases databases, final int aqgResults, final String sentimentField, final String combine) throws XPathExpressionException {
            this.aciService = aciService;
            this.databases = databases;
            this.aqgResults = aqgResults;
            this.sentimentField = StringUtils.trimToNull(sentimentField);
            this.combine = combine;
            xPositivity = StringUtils.isBlank(sentimentField) ? null
                : XPathFactory.newInstance().newXPath().compile("content/DOCUMENT/" + sentimentField);
        }

        public Engine(final AciServiceImpl aciService, final Databases databases, final int aqgResults) throws XPathExpressionException {
            this(aciService, databases, aqgResults, null, null);
        }

        public void setReaderTpl(final String readerTpl) {
            this.readerTpl = StringUtils.defaultIfEmpty(readerTpl, null);
        }

        public void setDocFields(final String docFields) {
            this.docFields = StringUtils.split(docFields.toUpperCase(Locale.US), "+,");
            this.docFieldsSet = new HashSet<String>(Arrays.asList(this.docFields));
        }
    }

    @Autowired
    private Engine[] engines;

    {
        try {
            xElement = xPath.compile("/autnresponse/responsedata/qs/element[@cluster>=0]");
            xReference = xPath.compile("reference");
            xDREContent = xPath.compile("content/DOCUMENT/DRECONTENT");
            xFields = xPath.compile("content/DOCUMENT/*");
            xTitle = xPath.compile("title");
            xSummary = xPath.compile("summary");
            xHit = xPath.compile("/autnresponse/responsedata/hit");
            xTotalHits = xPath.compile("/autnresponse/responsedata/totalhits");
            xValue = xPath.compile("/autnresponse/responsedata/field/value");
            xUser = xPath.compile("/autnresponse/responsedata/profile/results/hit/content/DOCUMENT/USERNAME");
            xProfile = xPath.compile("/autnresponse/responsedata/profile");
            xProfileHit = xPath.compile("results/hit");
            xPid = xPath.compile("pid");
            xCreated = xPath.compile("created");
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Invalid XPath", e);
        }
    }

    private Engine getEngine(final int idx) {
        return engines[idx];
    }

    @Override
    public Map<String,Object> getContent(
            final String reference,
            final int engine
    ) throws AciServiceException, XPathExpressionException {
        final Engine config = getEngine(engine);
        if (StringUtils.isEmpty(reference)) {
            throw new IllegalArgumentException("Invalid reference");
        }

        final AciParameters params = new AciParameters("getcontent");
        params.add("reference", new ReferencesBuilder(reference));
        params.add("print", "field");
        params.add("printfields", "DRECONTENT");
        params.add("outputencoding", "UTF8");
        params.add("databasematch", config.databases);

        final Document document = config.aciService.executeAction(params, new DocumentProcessor());

        final NodeList hits = (NodeList) xHit.evaluate(document, XPathConstants.NODESET);
        final List<String> sections = new ArrayList<String>();

        for (int ii = 0, max = hits.getLength() ; ii < max; ++ii) {
            final Node node = hits.item(ii);
            final String content = ((String) xDREContent.evaluate(node, XPathConstants.STRING));
            sections.add(content);
        }

        final HashMap<String, Object> toReturn = new HashMap<String, Object>();
        toReturn.put("sections", sections);
        return toReturn;
    }

    @Override
    public Map<String,Object> suggest(
            final int pageNum,
            final int pageSize,
            final String[] reference,
            final String[] exclude,
            final boolean summary,
            final boolean totalResults,
            final int engine,
            final Long minDate,
            final Long maxDate,
            final Double minScore
    ) throws AciServiceException, XPathExpressionException {
        final Engine config = getEngine(engine);
        if (reference.length == 0) {
            throw new IllegalArgumentException("Reference required");
        }
        
        for (final String ref : reference) {
            if (StringUtils.isEmpty(ref)) {
                throw new IllegalArgumentException("Invalid reference");
            }
        }

        final AciParameters params = new AciParameters("suggest");
        params.add("reference", new ReferencesBuilder(reference));

        if (exclude != null && exclude.length > 0) {
            params.add("dontmatchreference", new ReferencesBuilder(exclude));
        }

        if (minDate != null) { params.add("mindate", formatDate(minDate)); }
        if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }
        return fetchDocs(config, pageNum, pageSize, summary, totalResults, params, minScore);
    }

    @Cacheable(cacheName = "voronoi.query", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    @Override
    public Map<String,Object> query(
            final int pageNum,
            final int pageSize,
            final String[] exclude,
            final String[] query,
            final boolean summary,
            final boolean totalResults,
            final int engine,
            final Long minDate,
            final Long maxDate,
            final boolean matchAllTerms,
            final Double minScore
    ) throws AciServiceException, XPathExpressionException {
        final Engine config = getEngine(engine);
        final List<String> labels = new ArrayList<String>();
        for (int ii = 0; ii < query.length; ii++) {
            final String qs = query[ii];
             if (!isStarOrEmpty(qs)) {
                // we have to avoid putting quotes around the first (user-supplied) search text, so the iOS app
                // can provide a string of query terms as query text
                labels.add(ii == 0 ? qs : '"' + qs + '"');
            }
        }
        
        if (labels.isEmpty()) {
            labels.add("*");
        }

        final String actualQuery = new StringBuilder("(")
            .append(StringUtils.join(labels, ") AND ("))
            .append(")").toString();

        final AciParameters params = new AciParameters("query");
        params.add("text", actualQuery);
        params.add("matchallterms", matchAllTerms);

        if (exclude != null && exclude.length > 0) {
            params.add("dontmatchreference", new ReferencesBuilder(exclude));
        }

        if (minDate != null) { params.add("mindate", formatDate(minDate)); }
        if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }
        
        return fetchDocs(config, pageNum, pageSize, summary, totalResults, params, minScore);
    }

    private Map<String, Object> fetchDocs(final Engine config, final int pageNum, final int pageSize, final boolean summary, final boolean totalResults, final AciParameters params, final Double minScore) throws XPathExpressionException {
        final int skip = pageNum * pageSize;
        params.add("start", 1 + skip);
        params.add("maxresults", skip + pageSize);

        final PrintFields printFields = new PrintFields(config.docFields);
        if (config.sentimentField != null) {
            printFields.append(config.sentimentField);
        }

        if (printFields.isEmpty()) {
            params.add("print", "none");
        }
        else {
            params.add("print", "fields");
            params.add("printfields", printFields);
        }

        params.add("sort", "relevance");
        params.add("outputencoding", "UTF8");
        params.add("combine", config.combine);
        params.add("databasematch", config.databases);

        if (minScore != null) {
            params.add("minscore", minScore);
        }

        if (totalResults) {
            params.add("totalresults", true);
            params.add("predict", false);
        }

        if (summary) {
            params.add("highlight", "summaryterms");
            params.add("summary", "context");
            params.add("starttag", "<autn:highlight>");
            params.add("endtag", "</autn:highlight>");
            params.add("characters", 700);
        }

        final Document document = config.aciService.executeAction(params, new DocumentProcessor());

        final NodeList hits = (NodeList) xHit.evaluate(document, XPathConstants.NODESET);
        final String sTotalHits = (String) xTotalHits.evaluate(document, XPathConstants.STRING);
        final Integer totalHits = StringUtils.isBlank(sTotalHits) ? null : Integer.parseInt(sTotalHits);

        final List<AutnDoc> docs = new ArrayList<AutnDoc>();

        for (int ii = 0, max = hits.getLength() ; ii < max; ++ii) {
            final Node node = hits.item(ii);
            final String title = ((String) xTitle.evaluate(node, XPathConstants.STRING)).trim();
            final String ref = (String) xReference.evaluate(node, XPathConstants.STRING);
            final String docSummary = summary ? ((String) xSummary.evaluate(node, XPathConstants.STRING)).trim() : null;
            final String sentiment = config.sentimentField == null ? null : (String) (config.xPositivity.evaluate(node, XPathConstants.STRING));
            final AutnDoc doc = new AutnDoc(ref, title, docSummary, sentiment);
            docs.add(doc);

            if (config.docFields.length > 0) {
                final Map<String, Object> data = doc.data = new HashMap<String, Object>();
                final NodeList fields = (NodeList) xFields.evaluate(node, XPathConstants.NODESET);
                for (int fieldIdx = 0, fieldMax = fields.getLength(); fieldIdx < fieldMax; ++fieldIdx) {
                    final Node fieldNode = fields.item(fieldIdx);
                    // implicit uppercase assumption here
                    final String fieldName = fieldNode.getNodeName();
                    if (config.docFieldsSet.contains(fieldName)) {
                        data.put(fieldName, fieldNode.getTextContent());
                    }
                }
            }
        }

        final HashMap<String, Object> toReturn = new HashMap<String, Object>();
        toReturn.put("docs", docs);
        toReturn.put("totalhits", totalHits);
        toReturn.put("tpl", config.readerTpl);

        return toReturn;
    }

    static class AutnDoc {
        AutnDoc(final String ref, final String title, final String summary, final String sentiment) {
            this.summary = summary;
            this.ref = ref;
            this.title = title;
            this.sentiment = sentiment;
        }

        public String summary, ref, title, sentiment;

        public Map<String, Object> data;
    }

    private void updateClusterSentiment(final Engine config, final Cluster cluster, final String baseQuery) throws XPathExpressionException {
        final StringBuilder queryText = new StringBuilder("(\"").append(cluster.name).append("\")");
        if (!isStarOrEmpty(baseQuery)) {
            // we have to avoid putting quotes around the user-supplied baseQuery, so the iOS app
            // can provide a string of query terms as query text
            queryText.append(" AND (").append(baseQuery).append(")");
        }

        final AciParameters params = new AciParameters("getquerytagvalues");
        params.add("text", queryText);
        params.add("databasematch", config.databases);
        params.add("fieldname", config.sentimentField);
        params.add("combine", config.combine);
        params.add("valuedetails", true);
        params.add("outputencoding", "utf8");

        final Document document = config.aciService.executeAction(params, new DocumentProcessor());
        final NodeList values = (NodeList) xValue.evaluate(document, XPathConstants.NODESET);

        int totalCount = 0;
        int halfVotes = 0;
        for (int ii = 0, max = values.getLength() ; ii < max; ++ii) {
            final Node node = values.item(ii);
            final String text = node.getTextContent();
            final int count = Integer.parseInt(node.getAttributes().getNamedItem("count").getTextContent());
            if ("NEGATIVE".equalsIgnoreCase(text)) {
                totalCount += count;
            }
            else if("POSITIVE".equalsIgnoreCase(text)) {
                totalCount += count;
                halfVotes += 2 * count;
            }
            else if ("NEUTRAL".equalsIgnoreCase(text)) {
                totalCount += count;
                halfVotes += count;
            }
        }

        cluster.sentiment = totalCount == 0 ? 0.5 : 0.5 * halfVotes / totalCount;
    }

    // find the most-probable (highest dococcs) unstemmed word
    @Cacheable(cacheName = "voronoi.findUnstemmedTerm", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    @Override
    public String findUnstemmedTerm(final int engine, final String stemmedTerm) {
        // http://10.2.1.91:9002/a=termexpand&text=cat&expansion=stem&type=dococcs
        final AciParameters params = new AciParameters("termexpand");
        params.add("text", stemmedTerm);
        params.add("expansion", "stem");
        // sort by dococcs
        params.add("type", "dococcs");
        final List<String> terms = getEngine(engine).aciService.executeAction(params, new TermExpandProcessor());
        return terms.isEmpty() ? stemmedTerm : terms.get(0);
    }

    @Override
    @Cacheable(cacheName = "voronoi.querysummary", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public List<QuerySummaryElement> querysummary(
            final String query,
            final int maxResults,
            final int engine,
            final Long minDate,
            final Long maxDate,
            final Double minScore) {
        final Engine config = getEngine(engine);
        final AciParameters params = new AciParameters("query");
        params.add("text", isStarOrEmpty(query) ? "*" : query);
        params.add("querysummary", true);
        params.add("databasematch", config.databases);
        params.add("maxresults", maxResults > 0 ? maxResults : config.aqgResults);
        params.add("combine", config.combine);
        params.add("print", "noresults");
        params.add("outputencoding", "utf8");
        if (minScore != null) { params.add("minscore", minScore); }
        if (minDate != null) { params.add("mindate", formatDate(minDate)); }
        if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }
        return config.aciService.executeAction(params, new HierarchicalQueryResponseProcessor()).getAdvancedQuerySummary();
    }

    @Override
    public Cluster clusters(
            final String query,
            final int childLimit,
            final int maxResults,
            final boolean clusterSentiment,
            final int engine,
            final boolean clusterDedupe,
            final Long minDate,
            final Long maxDate,
            final boolean useDocIds,
            final Double minScore
    ) throws AciServiceException, XPathExpressionException {
        final Engine config = getEngine(engine);
        final Cluster cluster = new Cluster(query, 1, -1);
        cluster.children = new ArrayList<Cluster>();

        final AciParameters params = new AciParameters("query");
        params.add("text", isStarOrEmpty(query) ? "*" : query);
        params.add("querysummary", true);
        params.add("databasematch", config.databases);
        params.add("maxresults", maxResults > 0 ? maxResults : config.aqgResults);
        params.add("combine", config.combine);
        params.add("print", "noresults");
        params.add("outputencoding", "utf8");
        if (minDate != null) { params.add("mindate", formatDate(minDate)); }
        if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }
        if (minScore != null) { params.add("minscore", minScore); }

        final Document document = config.aciService.executeAction(params, new DocumentProcessor());

        final NodeList hits = (NodeList) xElement.evaluate(document, XPathConstants.NODESET);

        final Map<Integer, Cluster> clusters = new HashMap<Integer, Cluster>();

        final boolean fetchSentiment = clusterSentiment && StringUtils.isNotEmpty(config.sentimentField);

        int childClusters = 0;

        for (int jj = 0, max = hits.getLength() ; jj < max; ++jj) {
            final Node node = hits.item(jj);
            final String text = node.getTextContent();
            final int clusterId = Integer.parseInt(node.getAttributes().getNamedItem("cluster").getTextContent());
            final int pdocs = Integer.parseInt(node.getAttributes().getNamedItem("pdocs").getTextContent());

            Cluster existingCluster = clusters.get(clusterId);
            if (!clusterDedupe || existingCluster == null) {
                if (childLimit > 0 && childClusters >= childLimit) {
                    continue;
                }
                existingCluster = new Cluster(text, pdocs, clusterId);
                existingCluster.children = new ArrayList<Cluster>();
                clusters.put(clusterId, existingCluster);
                cluster.children.add(existingCluster);
                childClusters++;

                if (useDocIds) {
                    final Node ids = node.getAttributes().getNamedItem("ids");
                    if (ids == null) {
                        throw new IllegalArgumentException("QuerySummaryIds=true must be enabled on the content engine to use docIds");
                    } else {
                        existingCluster.docIds = ids.getTextContent();
                    }
                }

                if (fetchSentiment) {
                    updateClusterSentiment(config, existingCluster, query);
                }
            }
            else if (childLimit < 0 || existingCluster.children.size() < childLimit){
                final Cluster childCluster = new Cluster(text, pdocs, clusterId);
                existingCluster.children.add(childCluster);
                if (fetchSentiment) {
                    updateClusterSentiment(config, childCluster, query);
                }
            }
        }

        return cluster;
    }

    public class Profile {
        private Profile(final String pid, final long created, final List<AutnDoc> docs) {
            this.pid = pid;
            this.created = created;
            this.docs = docs;
        }

        public String pid;
        public long created;
        public List<AutnDoc> docs;
    }

    @Override
    @Cacheable(cacheName = "voronoi.userSuggestions", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public List<Profile> userSuggestions(final String user, final boolean summary, final Double minScore) throws XPathExpressionException {
        // http://maindemo-community:9200/a=profilegetresults&username=tungj
        final AciParameters params = new AciParameters("profilegetresults");
        params.add("username", user);

        if (summary) {
            params.add("DREhighlight", "summaryterms");
            params.add("DREsummary", "context");
            params.add("DREstarttag", "<autn:highlight>");
            params.add("DREendtag", "</autn:highlight>");
            params.add("DREdatabasematch", communityDatabase);
            params.add("DREcharacters", 700);
        }

        if (minScore != null) { params.add("minscore", minScore); }

        final Document doc = communityAciService.executeAction(params, new DocumentProcessor());
        final NodeList profiles = (NodeList) xProfile.evaluate(doc, XPathConstants.NODESET);

        final int profileCount = profiles.getLength();

        final List<Profile> toReturn = new ArrayList<Profile>(profileCount);

        for (int ii = 0; ii < profileCount; ++ii) {
            final Node profile = profiles.item(ii);
            final String pid = xPid.evaluate(profile);
            final long created = Long.parseLong(xCreated.evaluate(profile));

            final NodeList resultNodes = (NodeList) xProfileHit.evaluate(profile, XPathConstants.NODESET);
            final int resultCount = resultNodes.getLength();
            final List<AutnDoc> docs = new ArrayList<AutnDoc>(resultCount);

            for (int jj = 0; jj < resultCount; ++jj) {
                final Node node = resultNodes.item(jj);
                final String title = ((String) xTitle.evaluate(node, XPathConstants.STRING)).trim();
                final String ref = (String) xReference.evaluate(node, XPathConstants.STRING);
                final String docSummary = xSummary.evaluate(node).trim();
                docs.add(new AutnDoc(ref, title, docSummary, null));
            }

            toReturn.add(new Profile(pid, created,  docs));
        }

        return toReturn;
    }

    public static class TermMeta {
        public int weight;
        public String term;
        // unstemmed form
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public String text;

        private TermMeta(final int weight, final String term) {
            this.weight = weight;
            this.term = term;
        }
    }

    private static class TermMetaProcessor extends AbstractStAXProcessor<List<TermMeta>> {
        private TermMetaProcessor() {
            setErrorProcessor(new ErrorProcessor());
        }

        @Override
        public List<TermMeta> process(final XMLStreamReader xmlStreamReader) {
            try {
                if(isErrorResponse(xmlStreamReader)) {
                    processErrorResponse(xmlStreamReader);
                }

                final List<TermMeta> terms = new ArrayList<TermMeta>();

                while(xmlStreamReader.hasNext()) {
                    if((XMLEvent.START_ELEMENT == xmlStreamReader.next()) && ("autn:term".equalsIgnoreCase(xmlStreamReader.getLocalName()))) {
                        final int weight = Integer.parseInt(xmlStreamReader.getAttributeValue(null, "apcm_weight"));
                        final String term = xmlStreamReader.getElementText();
                        terms.add(new TermMeta(weight, term));
                    }
                }

                return terms;
            }
            catch(XMLStreamException xmlse) {
                throw new ProcessorException("Unable to parse terms", xmlse);
            }
        }
    }

    @Override
    @Cacheable(cacheName = "voronoi.getTermsForProfiles", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public List<TermMeta> getTermsForProfiles(final List<String> profileDocIds, final int maxTerms) throws XPathExpressionException {
//        maindemo-community:9112/action=termgetbest&id=35793&print=fields&printfields=training&outputencoding=utf8&anylanguage=true&databasematch=profile
        final AciParameters params = new AciParameters("termgetbest");
        params.add("id", StringUtils.join(profileDocIds, '+'));
        params.add("print", "printfields");
        params.add("printfields", new PrintFields("training"));
        params.add("anylanguage", true);
        params.add("outputencoding", "UTF8");
        params.add("databasematch", "profile");

        if (maxTerms >= 0) {
            params.add("maxterms", maxTerms);
        }

        return communityAgentStoreAciService.executeAction(params, new TermMetaProcessor());
    }

    // Processor to get the usernames, clusterId and docId of all profiles instead of XPath since XPath is really slow.
    // Generally, it's a bad idea to use docIds since they won't work through DAHs, but this is an exception
    // since agentstore doesn't use a DAH, we want short ids and we're using cluster=true (also non-DAH)
    private static class ClusterProfileProcessor extends AbstractStAXProcessor<List<GraphNode<Set<Integer>>>> {
        private final int maxUsers;
        private final Map<Integer, List<Integer>> clusterIdToDocIds;

        private ClusterProfileProcessor(final int maxUsers, final int maxProfiles) {
            setErrorProcessor(new ErrorProcessor());
            this.maxUsers = maxUsers;
            this.clusterIdToDocIds = new HashMap<Integer, List<Integer>>(maxProfiles);
        }

        public Map<Integer, List<Integer>> getClusterIdToDocIds() {
            return clusterIdToDocIds;
        }

        @Override
        public List<GraphNode<Set<Integer>>> process(final XMLStreamReader xml) throws AciErrorException, ProcessorException {
            try {
                if(isErrorResponse(xml)) {
                    processErrorResponse(xml);
                }

                final List<GraphNode<Set<Integer>>> users = new ArrayList<GraphNode<Set<Integer>>>(maxUsers);
                final HashMap<String, Set<Integer>> usersToClusterIds = new HashMap<String, Set<Integer>>(maxUsers);

                String username = null;
                int clusterId = -1, docId = -1;

                // we assume sort=cluster, so we can use that to reduce map lookups
                List<Integer> docsInCluster = null;
                int lastClusterId = -1;

                boolean inHit = false;

                while(xml.hasNext()) {
                    final int eventType = xml.next();
                    switch (eventType) {
                        case XMLEvent.START_ELEMENT:
                            if (inHit) {
                                final String localName = xml.getLocalName();
                                if ("autn:id".equals(localName)) {
                                    docId = Integer.parseInt(xml.getElementText());
                                }
                                else if("autn:cluster".equals(localName)) {
                                    clusterId = Integer.parseInt(xml.getElementText());
                                }
                                else if("USERNAME".equals(localName)) {
                                    username = xml.getElementText();
                                }
                            } else if ("autn:hit".equals(xml.getLocalName())) {
                                inHit = true;
                            }
                            break;
                        case XMLEvent.END_ELEMENT:
                            if (inHit && "autn:hit".equals(xml.getLocalName())) {
                                inHit = false;

                                if (lastClusterId != clusterId) {
                                    lastClusterId = clusterId;
                                    docsInCluster = clusterIdToDocIds.get(clusterId);
                                    if (docsInCluster == null) {
                                        docsInCluster = new ArrayList<Integer>();
                                        clusterIdToDocIds.put(clusterId, docsInCluster);
                                    }
                                }

                                docsInCluster.add(docId);

                                Set<Integer> clusterIds = usersToClusterIds.get(username);
                                if (clusterIds == null) {
                                    final GraphNode<Set<Integer>> graphNode = new GraphNode<Set<Integer>>(users.size(), username, 1, null, clusterId);
                                    users.add(graphNode);
                                    clusterIds = new HashSet<Integer>();
                                    graphNode.meta = clusterIds;
                                    usersToClusterIds.put(username, clusterIds);
                                }
                                clusterIds.add(clusterId);
                            }
                            break;
                    }
                }

                return users;
            }
            catch(final XMLStreamException xmlse) {
                throw new ProcessorException("Unable to parse cluster profiles", xmlse);
            }
        }
    }

    @Override
    @Cacheable(cacheName = "voronoi.usergraph", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Voronoi.Graph usergraph(
            final String query,
            final int maxUsers,
            final Double minScore) throws AciServiceException, XPathExpressionException {
        // Run http://maindemo-community:9112/action=query&sort=cluster&cluster=true&outputencoding=UTF8&print=fields&printfields=username%2Cname&minscore=20&text=*&maxresults=1000&languagetype=englishUTF8&anylanguage=TRUE&databasematch=profile
        // dedupe users. Links are the clusterIds. Usernames are the deduped usernames.
        // The output is huge, so probably should use a processor.
        // Each user has up to 20 profiles
        final int profilesPerUser = 20;
        final int maxProfiles = profilesPerUser * maxUsers;

        final AciParameters params = new AciParameters("query");
        params.add("text", StringUtils.defaultIfEmpty(query, "*"));
        params.add("sort", "cluster");
        params.add("cluster", true);
        params.add("outputencoding", "UTF8");
        params.add("print", "fields");
        params.add("printfields", new PrintFields("username"));
        if (minScore != null) { params.add("minscore", minScore); }
        params.add("maxresults", maxProfiles);
        params.add("languagetype", "englishUTF8");
        params.add("anylanguage", true);
        params.add("databasematch", "profile");

        final ClusterProfileProcessor processor = new ClusterProfileProcessor(maxUsers, maxProfiles);
        final List<GraphNode<Set<Integer>>> users = communityAgentStoreAciService.executeAction(params, processor);

        final Graph graph = createNodeGraph(false, users, new LinkCreator<Set<Integer>, Integer>() {
            @Override
            public Set<Integer> createLinks(final GraphNode<Set<Integer>> child) {
                return child.meta;
            }
        }, true);

        graph.linkMeta = processor.getClusterIdToDocIds();
        return graph;
    }

    @Override
    @Cacheable(cacheName = "voronoi.nodegraph", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Voronoi.Graph nodegraph(
            final String query,
            final int childLimit,
            final boolean clusterSentiment,
            final int engine,
            final boolean clusterDedupe,
            final int aqgMaxResults,
            final int termsMaxResults,
            final LinkMode linkMode,
            final Long minDate,
            final Long maxDate,
            final Double minScore) throws AciServiceException, XPathExpressionException {
        final Engine config = getEngine(engine);
        final Cluster clusters = clusters(query, childLimit, aqgMaxResults, clusterSentiment, engine, clusterDedupe, minDate, maxDate, linkMode.useDocIds, minScore);

        final List<Cluster> nodeClusters = clusters.children;

        final LinkCreator<String, String> linkCreator;

        switch (linkMode) {
            case references:
                linkCreator = new ReferenceNewsLinkCreator(termsMaxResults, minDate, maxDate, config); break;
            case terms:
                linkCreator = new TermsNewsLinkCreator(termsMaxResults, minDate, maxDate, config); break;
            case querysummary:
            default:
                linkCreator = new QuerySummaryIdsLinkCreator(); break;
        }

        Collections.sort(nodeClusters, new Comparator<Cluster>() {
            @Override
            public int compare(final Cluster o1, final Cluster o2) {
                return (int) Math.signum(o2.size - o1.size);
            }
        });

        final List<GraphNode<String>> graphNodes = new ArrayList<GraphNode<String>>();
        int idx = 0;
        for (final Cluster child : nodeClusters) {
            final GraphNode<String> graphNode = new GraphNode<String>(idx, child.name, child.size, child.sentiment, child.clusterId);
            graphNode.meta = child.docIds;
            graphNodes.add(graphNode);
            ++idx;
        }

        return createNodeGraph(clusterSentiment, graphNodes, linkCreator, false);
    }

    private <NodeType, LinkType> Graph createNodeGraph(final boolean clusterSentiment, final List<GraphNode<NodeType>> nodeClusters, final LinkCreator<NodeType, LinkType> linkCreator, final boolean trackLinks) {
        final Map<LinkType, TreeSet<GraphNode<NodeType>>> linkMap = new HashMap<LinkType, TreeSet<GraphNode<NodeType>>>();
        final List<GraphNode<NodeType>> graphNodes = new ArrayList<GraphNode<NodeType>>();

        int idx = 0;

        for (final GraphNode<NodeType> child : nodeClusters) {
            final Iterable<LinkType> links = linkCreator.createLinks(child);

            final GraphNode<NodeType> graphNode = new GraphNode<NodeType>(idx, child.name, child.size, child.sentiment, child.clusterId);
            graphNodes.add(graphNode);
            ++idx;

            for (final LinkType link : links) {
                TreeSet<GraphNode<NodeType>> linkNodes = linkMap.get(link);
                if (linkNodes == null) {
                    linkMap.put(link, linkNodes = new TreeSet<GraphNode<NodeType>>(new Comparator<GraphNode<NodeType>>() {
                        @Override
                        public int compare(final GraphNode<NodeType> g1, final GraphNode<NodeType> g2) {
                            return g1.idx - g2.idx;
                        }
                    }));
                }
                linkNodes.add(graphNode);
            }
        }

        final Map<GraphLink, Set<LinkType>> links = new HashMap<GraphLink, Set<LinkType>>();
        for (final Map.Entry<LinkType, TreeSet<GraphNode<NodeType>>> entry: linkMap.entrySet()){
            final LinkType sharedLink = entry.getKey();
            final TreeSet<GraphNode<NodeType>> nodes = entry.getValue();
            for (final GraphNode<NodeType> previous : nodes) {
                for (final GraphNode<NodeType> graphNode: nodes.tailSet(previous, false)) {
                    final GraphLink graphLink = new GraphLink(previous, graphNode);
                    Set<LinkType> linkTerms = links.get(graphLink);
                    if (linkTerms == null) {
                        links.put(graphLink, linkTerms = new LinkedHashSet<LinkType>());
                    }
                    linkTerms.add(sharedLink);
                }
            }
        }

        for (final Map.Entry<GraphLink, Set<LinkType>> entry: links.entrySet()){
            final GraphLink graphLink = entry.getKey();
            final Set<LinkType> shared = entry.getValue();
            graphLink.shared = shared.size();
            if (trackLinks) {
                graphLink.links = shared;
            }
        }

        return new Graph<NodeType>(graphNodes, new ArrayList<GraphLink>(links.keySet()), clusterSentiment);
    }

    private interface LinkCreator<NodeType, LinkType> {
        public Iterable<LinkType> createLinks(final GraphNode<NodeType> child);
    }

    private abstract class NewsLinkCreator implements LinkCreator<String, String> {
        protected final int termsMaxResults;
        protected final Long minDate;
        protected final Long maxDate;
        protected final Engine config;

        public NewsLinkCreator(final int termsMaxResults, final Long minDate, final Long maxDate, final Engine config) {
            this.termsMaxResults = termsMaxResults;
            this.minDate = minDate;
            this.maxDate = maxDate;
            this.config = config;
        }
    }

    private class TermsNewsLinkCreator extends NewsLinkCreator {
        private TermsNewsLinkCreator(final int termsMaxResults, final Long minDate, final Long maxDate, final Engine config) {
            super(termsMaxResults, minDate, maxDate, config);
        }

        public List<String> createLinks(final GraphNode child) {
            // fetch the list of terms related to that child
            final AciParameters params = new AciParameters("query");
            params.add("text", child.name);
            params.add("databasematch", config.databases);
            params.add("maxresults", termsMaxResults);
            params.add("combine", config.combine);
            params.add("print", "noresults");
            params.add("outputencoding", "utf8");
            params.add("storestate", true);
            if (minDate != null) { params.add("mindate", formatDate(minDate)); }
            if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }

            final String stateId = config.aciService.executeAction(params, new HierarchicalQueryResponseProcessor()).getState();

            final AciParameters termParams = new AciParameters("termgetbest");
            termParams.add("stateid", stateId);

            return config.aciService.executeAction(termParams, new TermExpandProcessor());
        }

    }

    private class ReferenceNewsLinkCreator extends NewsLinkCreator {
        private ReferenceNewsLinkCreator(final int termsMaxResults, final Long minDate, final Long maxDate, final Engine config) {
            super(termsMaxResults, minDate, maxDate, config);
        }

        public List<String> createLinks(final GraphNode child) {
            // fetch the list of terms related to that child
            final AciParameters params = new AciParameters("query");
            params.add("text", child.name);
            params.add("databasematch", config.databases);
            params.add("maxresults", termsMaxResults);
            params.add("combine", config.combine);
            params.add("print", "none");
            if (minDate != null) { params.add("mindate", formatDate(minDate)); }
            if (maxDate != null) { params.add("maxdate", formatDate(maxDate)); }

            final QueryResponse queryResponse = config.aciService.executeAction(params, new HierarchicalQueryResponseProcessor());
            final List<String> references = new ArrayList<String>(queryResponse.getNumHits());

            for (final com.autonomy.aci.actions.idol.query.Document document : queryResponse.getDocuments()) {
                references.add(document.getReference());
            }

            return references;
        }
    }

    private class QuerySummaryIdsLinkCreator implements LinkCreator<String, String> {
        @Override
        public List<String> createLinks(final GraphNode<String> child) {
            return Arrays.asList(StringUtils.split(child.meta, ","));
        }
    }

    public static class Graph<NodeType> {
        public Graph(final List<? extends GraphNode<NodeType>> nodes, final List<GraphLink> links, final boolean sentiment) {
            this.nodes = nodes;
            this.links = links;
            this.sentiment = sentiment;
        }

        public boolean sentiment;
        public List<? extends GraphNode<NodeType>> nodes;
        public List<GraphLink> links;
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public Object linkMeta;
    }

	public static class GraphNode<NodeType> {
        public String name;
        public double size;
        public Double sentiment;
        protected int idx;
        public int clusterId;
        private NodeType meta;

		public GraphNode(final int idx, final String name, final double size, final Double sentiment, final int clusterId) {
            this.idx = idx;
            this.name = name;
            this.size = size;
            this.sentiment = sentiment;
            this.clusterId = clusterId;
        }
    }

    public static class GraphLink {
        @JsonSerialize(using = GraphNodeSerializer.class)
        public GraphNode source;
        @JsonSerialize(using = GraphNodeSerializer.class)
        public GraphNode target;
        public int shared;
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public Set<?> links;

		public GraphLink(final GraphNode target, final GraphNode source) {
            this.target = target;
            this.source = source;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final GraphLink graphLink = (GraphLink) o;

            return graphLink.source.idx == source.idx && graphLink.target.idx == target.idx;
        }

        @Override
        public int hashCode() {
            int result = ((Integer)source.idx).hashCode();
            result = 31 * result + ((Integer)target.idx).hashCode();
            return result;
        }

        private static class GraphNodeSerializer extends JsonSerializer<GraphNode> {
            @Override
            public void serialize(final GraphNode graphNode, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
                jsonGenerator.writeNumber(graphNode.idx);
            }
        }
    }

    private static boolean isStarOrEmpty(final String query) {
        return StringUtils.isBlank(query) || query.trim().equals("*");
    }

    public static class Cluster {
        public String name;
        public double size;
        public List<Cluster> children;
        public Double sentiment;
        private int clusterId;
        // docIds of the querysummaryelement, if present
        private String docIds;

        public Cluster(final String name, final double size, final int clusterId) {
            this.size = size;
            this.name = name;
            this.clusterId = clusterId;
        }
    }

    public static enum LinkMode {
        terms(false), references(false), querysummary(true);
        final boolean useDocIds;

        LinkMode(final boolean useDocIds) {
            this.useDocIds = useDocIds;
        }
    }
}

