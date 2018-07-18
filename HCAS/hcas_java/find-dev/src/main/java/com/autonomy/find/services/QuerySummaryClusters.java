package com.autonomy.find.services;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.find.services.QuerySummaryClusters.Cluster;
import com.autonomy.find.services.QuerySummaryClusters.QuerySummaryResult;

public final class QuerySummaryClusters {

	public static QuerySummaryResult structureSummaryElements(
			final List<QuerySummaryElement> elements, final int maxChildren,
			final String database) {

		final List<Cluster> result = new LinkedList<Cluster>();
		final List<Cluster> mainResult = new LinkedList<Cluster>();
		final Map<Integer, Cluster> clusters = new HashMap<Integer, Cluster>();
		Cluster existingCluster, elementCluster;

		// For each element of elements
		for (final QuerySummaryElement element : elements) {

			// mainResults are those with -1 cluster ids
			if (element.getCluster() == -1) {
				mainResult.add(Cluster.fromSummaryElement(element, database));
				continue;
			}

			// Create a cluster from the element
			elementCluster = Cluster.fromSummaryElement(element, database);

			// Look up the cluster id in our map of clusters
			existingCluster = clusters.get(element.getCluster());

			// If we found one
			if (existingCluster != null) {
				existingCluster.addChild(elementCluster);
			}
			// If the cluster was not present
			else {
				// Place it in the map
				clusters.put(element.getCluster(), elementCluster);
				// Push it to the rootCluster's children
				result.add(elementCluster);
			}
		}

		for (final Cluster cluster : result) {
			cluster.limitChildren(maxChildren);
		}

		Collections.sort(result);

		return QuerySummaryResult.create(mainResult, result);
	}

	@Data
	public static class QuerySummaryResult {
		private List<Cluster> mainClusters;
		private List<Cluster> clusters;
		public static QuerySummaryResult create() {
			return new QuerySummaryResult(null, null);
		}
		public static QuerySummaryResult create(final List<Cluster> mainClusters, final List<Cluster> clusters) {
			return new QuerySummaryResult(mainClusters, clusters);
		}
		public QuerySummaryResult(final List<Cluster> mainClusters, final List<Cluster> clusters) {
			this.mainClusters = (mainClusters == null)? new LinkedList<Cluster>() : mainClusters;
			this.clusters = (clusters == null)? new LinkedList<Cluster>() : clusters;
		}
		public List<Cluster> getClusters() {
			// TODO Auto-generated method stub
			return null;
		}
		public List<Cluster> getMainClusters() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class Cluster implements Comparable<Cluster> {
		@Getter
		private final String name;
		@Getter
		private final String database;
		@Getter
		private final int size;
		@Getter
		private final List<Cluster> children;

		public Cluster(final String name, final String database, final int size) {
			this.name = name;
			this.database = database;
			this.size = size;
			this.children = new LinkedList<Cluster>();
		}

		public void addChild(final Cluster child) {
			if (child == null) {
				return;
			}
			this.children.add(child);
		}

		public void limitChildren(final int maxChildren) {
			Collections.sort(this.children);
			if (this.children.size() > maxChildren) {
				this.children.subList(maxChildren, this.children.size()).clear();
			}
			for (final Cluster child : this.children) {
				child.limitChildren(maxChildren);
			}
		}

		public static Cluster fromSummaryElement(final QuerySummaryElement element,
				final String database) {
			return new Cluster(element.getValue(), database, element.getPDocs());
		}

		public int compareTo(final Cluster obj) {
			return obj.size - this.size;
		}
		
		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		public List<Cluster> getChildren() {
			return children;
		}

		public String getName() {
			return name;
		}
	}
}