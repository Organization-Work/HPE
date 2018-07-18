package com.autonomy.find.dto;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

@Data
public class NewsResult {

  private final String type;
  private final List<NewsResultCluster> clusters = new LinkedList<NewsResultCluster>();

  public NewsResult(final String type) {
    this.type = type;
  }

  public NewsResult addClusters(final List<NewsResultCluster> clusters) {
    this.clusters.addAll(clusters);
    return this;
  }

public List<NewsResultCluster> getClusters() {
	return clusters;
}

public String getType() {
	return type;
}
}
