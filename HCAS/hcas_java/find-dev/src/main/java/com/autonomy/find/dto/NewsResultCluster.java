package com.autonomy.find.dto;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.aci.client.annotations.IdolProcessorField;

import lombok.Data;

@Data
@IdolDocument("autn:cluster")
public class NewsResultCluster {
  private String title;
  private String category;
  private float score;
  private List<String> terms = new LinkedList<String>();
  private List<ResultDocument> documents = new LinkedList<ResultDocument>();

  @IdolField("autn:title")
  public NewsResultCluster setTitle(final String value) {
    this.title = value;
    return this;
  }

  public NewsResultCluster setCategory(final String value) {
    this.category = value;
    return this;
  }
 
  @IdolField("autn:score")
  public NewsResultCluster setScore(final float value) {
    this.score = value;
    return this;
  }

  @IdolField("autn:term")
  public NewsResultCluster addTerm(final String value) {
    this.terms.add(value);
    return this;
  }

  @IdolProcessorField("autn:doc")
  public NewsResultCluster addDocument(final NewsResultDocument value) {
      this.documents.add(value);
      return this;
  }

public List<ResultDocument> getDocuments() {
	return documents;
}

public String getTitle() {
	return title;
}

public List<String> getTerms() {
	return terms;
}

public void setDocuments(List<ResultDocument> requestMoreDataForDocuments) {
	documents=requestMoreDataForDocuments;
}
}
