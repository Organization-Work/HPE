package com.autonomy.find.dto;

import java.util.List;

import com.autonomy.find.services.QuerySummaryClusters;

import lombok.Data;

@Data
public class SearchAjaxResult {
  public List<ResultDocument> searchResults;
  public List<QuerySummaryClusters.Cluster> searchSuggestions;
  public SearchAjaxResult(final List<ResultDocument> searchResults, final List<QuerySummaryClusters.Cluster> searchSuggestions) {
    this.searchResults = searchResults;
    this.searchSuggestions = searchSuggestions;
  }
}
