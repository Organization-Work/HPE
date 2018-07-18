package autn.voronoi;

import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.AciServiceException;
import com.autonomy.aci.client.services.Processor;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.transport.AciResponseInputStream;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.database.Databases;
import com.autonomy.common.io.IOUtils;
import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static autn.voronoi.DateUtil.formatDate;

@Controller
public class ThemeTracker {
    @Qualifier("themeTrackerAciService")
    @Autowired
    private AciService themeTrackerAciService = null;

    @Qualifier("themeTrackerCategoryDREAciService")
    @Autowired
    private AciService themeTrackerCategoryDREAciService = null;

    private static final String INTERVAL = "604800";

    @Qualifier("clustersDatabase")
    @Autowired
    private Databases clustersDatabase;

    @Qualifier("clustersMinScore")
    @Autowired
    private Double clustersMinScore;

    public Double getClustersMinScore() {
        return clustersMinScore;
    }

    public void setClustersMinScore(Double clustersMinScore) {
        this.clustersMinScore = clustersMinScore;
    }

    public Databases getClustersDatabase() {
        return clustersDatabase;
    }

    public void setClustersDatabase(Databases clustersDatabase) {
        this.clustersDatabase = clustersDatabase;
    }

    public AciService getThemeTrackerAciService() {
        return themeTrackerAciService;
    }

    public void setThemeTrackerAciService(AciService themeTrackerAciService) {
        this.themeTrackerAciService = themeTrackerAciService;
    }

    public AciService getThemeTrackerCategoryDREAciService() {
        return themeTrackerCategoryDREAciService;
    }

    public void setThemeTrackerCategoryDREAciService(AciService themeTrackerCategoryDREAciService) {
        this.themeTrackerCategoryDREAciService = themeTrackerCategoryDREAciService;
    }

    @RequestMapping(value="/p/themeClusters.json")
    public @ResponseBody
    Map<String, List<Cluster>> themeClusters(
        @RequestParam final long startDate,
        @RequestParam(defaultValue = INTERVAL) final long interval,
        @RequestParam final String jobName
    ) throws AciServiceException {
//    http://10.2.1.8:9120/action=ClusterSGDataServe&startdate=1339680114&sourcejobname=MDN_CEN&StructuredXML=true
//    http://10.2.1.8:9120/action=clustersgpicserve&sourcejobname=MDN_CEN&startdate=1339680114&enddate=1340284914

        final AciParameters params = new AciParameters("ClusterSGDataServe");
        params.add("startdate", startDate);
        params.add("enddate", startDate + interval);
        params.add("sourcejobname", jobName);
        params.add("StructuredXML", true);

        final List<Cluster> clusters = themeTrackerAciService.executeAction(params, new ClusterStaxProcessor());

        return Collections.singletonMap("clusters", clusters);
    }

    @RequestMapping(value="/p/themeImage.json")
    public void themeImage(
        @RequestParam final long startDate,
		@RequestParam(defaultValue = INTERVAL) final long interval,
        @RequestParam final String jobName,
        final HttpServletResponse response

    ) throws AciServiceException, IOException {
//    http://10.2.1.8:9120/action=ClusterSGDataServe&startdate=1339680114&sourcejobname=MDN_CEN&StructuredXML=true
//    http://10.2.1.8:9120/action=clustersgpicserve&sourcejobname=MDN_CEN&startdate=1339680114&enddate=1340284914

        final AciParameters params = new AciParameters("clustersgpicserve");
        params.add("startdate", startDate);
        params.add("enddate", startDate + interval);
        params.add("sourcejobname", jobName);

        final ServletOutputStream outputStream = response.getOutputStream();

        themeTrackerAciService.executeAction(params, new Processor<Boolean>() {
            public Boolean process(AciResponseInputStream aciResponse) {
                try
                {
                    response.setContentType(aciResponse.getContentType());
                    IOUtils.copyLarge(aciResponse, outputStream);
                } catch (IOException e) {
                    throw new ProcessorException("Error fetching image", e);
                }
                return true;
            }
        });

        outputStream.flush();
        outputStream.close();
    }
    
    @RequestMapping(value="/p/themeDocuments.json")
    public @ResponseBody List<ClusterDoc> themeDocuments(
        @RequestBody final Cluster cluster
    ) throws AciServiceException {
        return getSGDocs(cluster, 3).docs;
    }

    private ClusterSGDocs getSGDocs(Cluster cluster, int numresults) {
        final AciParameters params = new AciParameters("ClusterSGDocsServe");
        params.add("startdate", cluster.fromDate);
        params.add("enddate", cluster.toDate-1);
        params.add("sourcejobname", cluster.jobName);
        params.add("cluster", cluster.id);
        // there don't seem to be print options etc
        // there's a numresults parameter, but it doesn't seem to work (sets the autn:numdocs but doesn't trim the response)
        params.add("NumResults", numresults);

        return themeTrackerAciService.executeAction(params, new ClusterDocsProcessor());
    }

    static class ClusterList extends ArrayList<Cluster> {
        public ClusterList() { super(); }
    }

    static final class Cluster {
        public String title;
        @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
        public String jobName;
        public long fromDate, toDate;
        public int numDocs, x1, x2, y1, y2, id;

        Cluster() {}

        Cluster(String title, long fromDate, long toDate, int numDocs, int x1, int x2, int y1, int y2, int id) {
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

    static final class ClusterStaxProcessor extends AbstractStAXProcessor<List<Cluster>> {
        @Override
        public List<Cluster> process(XMLStreamReader xmlStreamReader) {
            try {
                final List<Cluster> clusters = new ArrayList<Cluster>();
                String title = null;
                long fromDate, toDate;
                int numDocs, x1, x2, y1, y2, id;
                fromDate = toDate = numDocs = x1 = x2 = y1 = y2 = id = 0;

                while(xmlStreamReader.hasNext()) {
                    final int evtType = xmlStreamReader.next();
                    switch (evtType) {
                        case XMLEvent.START_ELEMENT:
                            final String localName = xmlStreamReader.getLocalName();
                            if ("autn:cluster".equals(localName)) {
                                fromDate = toDate = numDocs = x1 = x2 = y1 = y2 = id = 0;
                                title = null;
                            }
                            else if ("autn:title".equals(localName)) {
                                title = xmlStreamReader.getElementText();
                            }
                            else if ("autn:fromdate".equals(localName)) {
                                fromDate = Long.parseLong(xmlStreamReader.getElementText());
                            }
                            else if ("autn:todate".equals(localName)) {
                                toDate = Long.parseLong(xmlStreamReader.getElementText());
                            }
                            else if ("autn:numdocs".equals(localName)) {
                                numDocs = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            else if ("autn:x1".equals(localName)) {
                                x1 = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            else if ("autn:x2".equals(localName)) {
                                x2 = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            else if ("autn:y1".equals(localName)) {
                                y1 = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            else if ("autn:y2".equals(localName)) {
                                y2 = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            else if ("autn:id".equals(localName)) {
                                id = Integer.parseInt(xmlStreamReader.getElementText());
                            }
                            break;
                        case XMLEvent.END_ELEMENT:
                            if ("autn:cluster".equals(xmlStreamReader.getLocalName())) {
                                clusters.add(new Cluster(title, fromDate, toDate, numDocs, x1, x2, y1, y2, id));
                            }
                            break;
                    }
                }

                return clusters;
            } catch (XMLStreamException e) {
                throw new ProcessorException("Error reading XML", e);
            }
        }
    }

    static class ClusterDoc {
        public String title, ref, summary;
        public float score;

        ClusterDoc() {}

        ClusterDoc(String title, String ref, String summary, float score) {
            this.title = title;
            this.ref = ref;
            this.summary = summary;
            this.score = score;
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

    static final class ClusterDocsProcessor extends AbstractStAXProcessor<ClusterSGDocs> {
        ClusterDoc processCluster(XMLStreamReader xmlStreamReader) throws XMLStreamException {
            String title = "", ref = "", summary = "";
            int score = 0;

            while(xmlStreamReader.hasNext()) {
                final int evtType = xmlStreamReader.next();
                switch (evtType) {
                    case XMLEvent.START_ELEMENT:
                        final String localName = xmlStreamReader.getLocalName();
                        if ("autn:doc".equals(localName)) {
                            score = 0;
                            title = ref = summary = null;
                        }
                        else if ("autn:title".equals(localName)) {
                            title = xmlStreamReader.getElementText();
                        }
                        else if ("autn:ref".equals(localName)) {
                            ref = xmlStreamReader.getElementText();
                        }
                        else if ("autn:summary".equals(localName)) {
                            summary = xmlStreamReader.getElementText();
                        }
                        else if ("autn:score".equals(localName)) {
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

                while(xmlStreamReader.hasNext()) {
                    final int evtType = xmlStreamReader.next();
                    switch (evtType) {
                        case XMLEvent.START_ELEMENT:
                            localName = xmlStreamReader.getLocalName();
                            if ("autn:docs".equals(localName)) {
                                inAutnDocs = true;
                            }
                            else if (inAutnDocs && "autn:doc".equals(localName)) {
                                clusters.add(processCluster(xmlStreamReader));
                            }
                            else if ("autn:term".equals(localName)) {
                                terms.add(xmlStreamReader.getElementText());
                            }
                            else if (title == null && "autn:title".equals(localName)) {
                                title = xmlStreamReader.getElementText();
                            }
                            break;
                        case XMLEvent.END_ELEMENT:
                            localName = xmlStreamReader.getLocalName();
                            if ("autn:docs".equals(localName)) {
                                inAutnDocs = false;
                            }
                            else if ("autn:cluster".equals(localName)) {
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

    @RequestMapping(value="/p/themeTimeline.json")
    public @ResponseBody Map<String, Object> themeTimeline(
        @RequestBody final Cluster cluster,
        @RequestParam(required = false) Long endDate,
        @RequestParam(defaultValue = "day") String dateperiod,
        @RequestParam(defaultValue = "30") int days) {
        // http://10.2.1.91:9002/action=getquerytagvalues&fieldname=autn_date&dateperiod=day&documentcount=true&sort=reversedate&mindate=-29&databasematch=moreover&text=syria

        final ClusterSGDocs clusterMeta = getSGDocs(cluster, 0);

        final long endDateEpoch = endDate == null ? new Date().getTime() / 1000 : endDate;

        final AciParameters params = new AciParameters("getquerytagvalues");
        params.add("text", StringUtils.join(clusterMeta.terms, " "));
        params.add("fieldname", "autn_date");
        params.add("dateperiod", dateperiod);
        params.add("documentcount", true);
        params.add("databasematch", clustersDatabase);
        params.add("minscore", clustersMinScore);
        params.add("sort", "reversedate");
        params.add("maxdate", formatDate(endDateEpoch));
        params.add("mindate", (endDateEpoch - days * 3600 * 24) + "e");

        final List<DateTag> tags = themeTrackerCategoryDREAciService.executeAction(params, new DateTagProcessor());

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("dates", tags);
        map.put("terms", clusterMeta.terms);
        return map;
    }

    @RequestMapping(value="/p/themeTimelineDocs.json")
    public @ResponseBody Map<String, Object> themeTimelineDocs(
        @RequestBody TimelineDocRequest docRequest
    ){
        // if it's too slow to send the terms, consider sending the cluster details instead,
        // at the cost of having to do another query  
        final AciParameters params = new AciParameters("query");
        final int skip = docRequest.pageNum * docRequest.pageSize;
        params.add("start", 1 + skip);
        params.add("maxresults", skip + docRequest.pageSize);
        params.add("text", StringUtils.join(docRequest.terms, " "));
        params.add("databasematch", clustersDatabase);
        params.add("minscore", clustersMinScore);
        params.add("maxdate", formatDate(docRequest.endEpoch));
        params.add("mindate", formatDate(docRequest.startEpoch));
        params.add("summary", "context");

        final QueryResponse queryResponse = themeTrackerCategoryDREAciService.executeAction(params, new HierarchicalQueryResponseProcessor());

        final List<ClusterDoc> docs = new ArrayList<ClusterDoc>();

        for (Document doc : queryResponse.getDocuments()) {
            docs.add(new ClusterDoc(doc.getTitle(), doc.getReference(), doc.getSummary(), doc.getWeight()));
        }

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("docs", docs);
        return map;
    }

    static class TimelineDocRequest {
        public List<String> terms;
        public long startEpoch, endEpoch;
        public int pageNum = 0, pageSize = 6;
    }

    static class DateTag {
        public long epoch;
        public int count;

        DateTag() {}

        DateTag(long epoch, int count) {
            this.epoch = epoch;
            this.count = count;
        }
    }

    static final class DateTagProcessor extends AbstractStAXProcessor<List<DateTag>> {

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

                while(xmlStreamReader.hasNext()) {
                    final int evtType = xmlStreamReader.next();
                    switch (evtType) {
                        case XMLEvent.START_ELEMENT:
                            final String localName = xmlStreamReader.getLocalName();
                            if ("autn:value".equals(localName)) {
                                final int count = Integer.parseInt(xmlStreamReader.getAttributeValue(null, "count"));
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
