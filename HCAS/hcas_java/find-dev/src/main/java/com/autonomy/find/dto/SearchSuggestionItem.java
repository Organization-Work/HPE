package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolField;

public class SearchSuggestionItem {

  private String title;
  
  @IdolField("autn:title")
  public void setTitle(final String value) { this.title = value; }
  public String getTitle() { return this.title; }
}
