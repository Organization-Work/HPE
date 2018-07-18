package com.autonomy.find.config;

import lombok.Data;

@Data
public class PopDetailView {
    private String xTitle;
    private String yTitle;

	public String getxTitle() {
		return xTitle;
	}
	public void setxTitle(String xTitle) {
		this.xTitle = xTitle;
	}
	public String getyTitle() {
		return yTitle;
	}
	public void setyTitle(String yTitle) {
		this.yTitle = yTitle;
	}
}
