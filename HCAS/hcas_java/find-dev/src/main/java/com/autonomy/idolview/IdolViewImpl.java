package com.autonomy.idolview;

import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldListProcessor;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.database.Databases;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.aci.content.printfields.PrintFields;
import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import com.autonomy.idolview.processors.DateTagProcessor;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@Service
public class IdolViewImpl implements IdolView {
    @Autowired
    private FilterManager filterManager;

    @Autowired
    private MessageSource msgSource;

    @Autowired
    @Qualifier("queryPrintFields")
    private PrintFields printFields;

    @Autowired
    @Qualifier("databases")
    private Databases databases;

    @Autowired
    @Qualifier("contentAciService")
    private AciService contentAciService;

    @Override
    public Collection<Filter> filters() {
        return filterManager.getFilters();
    }

    @Override
    public List<QuerySummaryElement> aqg(final RequestDetails filterSpec) {
        final FieldText fieldText = buildFieldText(filterSpec);

        final AciParameters params = new AciParameters("query");
        params.add("sort", "documentcount");
        params.add("text", StringUtils.defaultIfEmpty(filterSpec.text, "*"));
        params.add("querysummary", true);
        params.add("maxresults", filterSpec.maxValues);
        params.add("print", "noresults");
        params.add("databasematch", databases);

        if (fieldText != null) {
            params.add("fieldtext", fieldText);
        }

        final QueryResponse response = contentAciService.executeAction(params, new HierarchicalQueryResponseProcessor());
        return response.getAdvancedQuerySummary();
    }

    @Override
    @Cacheable(cacheName="IdolView.filter", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public FilterResponse filteredCounts(final RequestDetails filterSpec, final Locale locale) {
        final int toFetchCount = filterSpec.hierarchyCountKeys.size();

        if (toFetchCount == 0) {
            return new FilterResponse(msgSource.getMessage("idolview.filter.select", null, locale), 0);
        }

        // http://10.2.1.90:9002/a=getquerytagvalues&fieldname=place&text=*&documentcount=true&sort=documentcount
        final FilterResponse root = new FilterResponse(msgSource.getMessage("idolview.total", null, locale), 1);
        int idx = 0;
        final Filter[] toFetch = new Filter[toFetchCount];
        for (final String hierarchyCountKey : filterSpec.hierarchyCountKeys) {
            toFetch[idx] = filterManager.getFilter(hierarchyCountKey);
            ++idx;
        }

        final FieldText fieldText = buildFieldText(filterSpec);

        root.size = annotateFieldValues(root, fieldText, toFetch, 0, filterSpec);

        if (root.size == 0) {
            root.name = msgSource.getMessage("idolview.noresults", null, locale);
        }

        if (filterSpec.fetchDates) {
            root.dates = fetchDates(fieldText, filterSpec);
        }

        return root;
    }

    @Override
    @Cacheable(cacheName="IdolView.filteredDocs", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public DocSummary filteredDocuments(final PagedRequestDetails filterSpec) {
        final FieldText fieldText = buildFieldText(filterSpec);
        final int start = filterSpec.page * filterSpec.pageSize;

        final AciParameters params = new AciParameters("query");
        params.add("text", StringUtils.defaultIfEmpty(filterSpec.text, "*"));
        params.add("xmlmeta", true);
        params.add("maxresults", start + filterSpec.pageSize);
        params.add("summary", "context");
        params.add("start", 1 + start);
        params.add("print", "fields");
        params.add("printfields", printFields);
        params.add("databasematch", databases);
        appendFieldTextAndDates(params, filterSpec, fieldText);

        return new DocSummary(contentAciService.executeAction(params, new HierarchicalQueryResponseProcessor()));
    }

    @Override
    @Cacheable(cacheName="IdolView.dates", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public DateCounts dates(final RequestDetails filterSpec) {
        final FieldText fieldText = buildFieldText(filterSpec);
        return fetchDates(fieldText, filterSpec);
    }

    private FieldText buildFieldText(final RequestDetails filterSpec) {
        FieldText fieldText = null;

        for (final Map.Entry<String, String[]> entry: filterSpec.filters.entrySet()){
            final String[] values = entry.getValue();

            for (final String value : values) {
                if (StringUtils.isNotBlank(value)) {
                    final FieldText newFilter = filterManager.getFilter(entry.getKey()).getFieldText(value);
                    fieldText = fieldText == null ? newFilter : fieldText.AND(newFilter);
                }
            }
        }

        return fieldText;
    }

    private int annotateFieldValues(final FieldNode node, final FieldText fieldText, final Filter[] toFetch, final int index, final RequestDetails filterSpec) {
        final Filter currentField = toFetch[index];
        final int maxValues = filterSpec.maxValues;

        // in theory, we could use the maxValues parameter. unfortunately, that then gives us no way to get the total count
        // without another query, so we fetch all the results anyway
        int total = 0, numChildren = 0;

        final List<FieldValue> fieldValues = getFieldValues(fieldText, currentField.idolField, filterSpec).get(0).getFieldValues();

        for (final FieldValue fieldValue : fieldValues) {
            final String name = fieldValue.getValue();
            final int count = fieldValue.getCount();
            total += count;

            if (maxValues >= 0 && numChildren < maxValues) {
                final String processedValue = currentField.processFieldValue(name);
                final FieldNode newNode = new FieldNode(processedValue, count);
                node.children.add(newNode);
                ++numChildren;

                if (index < toFetch.length - 1) {
                    final FieldText newFieldText = currentField.getFieldText(processedValue);
                    annotateFieldValues(newNode, fieldText == null ? newFieldText : fieldText.AND(newFieldText), toFetch, index + 1, filterSpec);
                }
            }
        }

        return total;
    }

    private DateCounts fetchDates(final FieldText fieldText, final RequestDetails filterSpec) {
        // perhaps the default should be configurable?
        final DatePeriod period = DatePeriod.chooseAppropriatePeriod(DatePeriod.day, filterSpec.start, filterSpec.end);

        final AciParameters params = new AciParameters("getquerytagvalues");
        params.add("text", StringUtils.defaultIfEmpty(filterSpec.text, "*"));
        params.add("fieldname", "autn_date");
        params.add("dateperiod", period.name());
        params.add("documentcount", true);
        params.add("sort", "reversedate");
        params.add("databasematch", databases);
        appendFieldTextAndDates(params, filterSpec, fieldText);

        return contentAciService.executeAction(params, new DateTagProcessor(period));
    }

    private List<Field> getFieldValues(final FieldText fieldText, final String fieldname, final RequestDetails filterSpec) {
        final AciParameters params = new AciParameters("getquerytagvalues");
        params.add("documentcount", true);
        params.add("sort", "documentcount");
        params.add("text", StringUtils.defaultIfEmpty(filterSpec.text, "*"));
        params.add("fieldname", fieldname);
        params.add("databasematch", databases);
        appendFieldTextAndDates(params, filterSpec, fieldText);

        return contentAciService.executeAction(params, new FieldListProcessor());
    }

    private static void appendFieldTextAndDates(final AciParameters params, final RequestDetails filterSpec, final FieldText fieldText) {
        if (fieldText != null) {
            params.add("fieldtext", fieldText);
        }

        if (filterSpec.start != null) {
            params.add("mindate", formatDate(filterSpec.start));
        }

        if (filterSpec.end != null) {
            params.add("maxdate", formatDate(filterSpec.end));
        }
    }

	private static String formatDate(final long epochSeconds) {
		if (epochSeconds > 0) {
			return epochSeconds + "e";
		}

		// IDOL doesn't allow negative values in the mindate/maxdate epoch date format
		final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return dateFormat.format(new Date(epochSeconds * 1000));
	}
}
