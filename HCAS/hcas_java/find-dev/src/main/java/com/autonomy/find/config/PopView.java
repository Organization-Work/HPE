package com.autonomy.find.config;

import lombok.Data;

@Data
public class PopView {
    private PopSummaryView summary;
    private PopDetailView detail;

	public void setSummary(PopSummaryView summary) {
		this.summary = summary;
	}
	public void setDetail(PopDetailView detail) {
		this.detail = detail;
	}
    public PopSummaryView getSummary() {
		return summary;
	}
    public PopDetailView getDetail() {
		return detail;
	}
}
