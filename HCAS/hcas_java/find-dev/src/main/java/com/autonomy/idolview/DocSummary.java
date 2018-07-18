package com.autonomy.idolview;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.actions.idol.query.DocumentField;
import com.autonomy.aci.actions.idol.query.QueryResponse;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/DocSummary.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class DocSummary {
    // essentially, we want to limit the size of the response, so we create a wrapper around QueryResponse object
    public int maxResults;
    public ArrayList<Doc> docs;

    public DocSummary(final QueryResponse response) {
        maxResults = response.getNumHits();

        final List<Document> documents = response.getDocuments();
        docs = new ArrayList<Doc>(documents.size());

        for (final Document document : documents) {
            docs.add(new Doc(document));
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private static class Doc {
        public Map<String,List<DocumentField>> fields;
        public Date date;
        public String title;
        public String ref;
        public String summary;

        private Doc(final Document doc) {
            fields = doc.getDocumentFields();
            title = doc.getTitle();
            date = doc.getDate();
            ref = doc.getReference();
            summary = doc.getSummary();
        }
    }
}
