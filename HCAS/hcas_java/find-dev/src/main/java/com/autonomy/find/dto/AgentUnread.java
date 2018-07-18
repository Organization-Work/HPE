package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;

@IdolDocument("autn:agent")
public class AgentUnread {
  private String aid = "";
  private int count = 0;

  @IdolField("autn:aid")
  public void setAID(final String value) {
    this.aid = value;
  }
  public String getAID() {
    return this.aid;
  }

  @IdolField("autn:numhits")
  public void setCount(final int value) {
    this.count = value;
  }
  public int getCount() {
    return this.count;
  }
}
