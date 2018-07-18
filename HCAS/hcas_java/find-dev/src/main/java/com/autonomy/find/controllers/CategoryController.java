package com.autonomy.find.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.aci.actions.idol.clustering.Cluster;
import com.autonomy.find.services.CategoryService;

@Controller
@RequestMapping("/p/ajax/clusters")
public class CategoryController {

	@Autowired
	private CategoryService category;

	/**
	 * Retrieves the breaking news items
	 * @return
	 */
	@RequestMapping("/getBreakingNews.json")
	public @ResponseBody
	List<Cluster> getBreakingNews() {
		return category.getBreakingNews().getClusters();
	}

	/**
	 * Retrieves the popular news items
	 * @return
	 */
	@RequestMapping("/getPopularNews.json")
	public @ResponseBody
	List<Cluster> getPopularNews() {
		return category.getPopularNews().getClusters();
	}
}
