package com.autonomy.find.services.search;

import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.processors.StoreStateProcessor;
import com.autonomy.find.util.StateResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StoreStateResultsIterator<T> implements Iterator<T>{
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreStateResultsIterator.class);
    private static final Pattern DOCNUM_REGEX = Pattern.compile(".+-[^\\d]*(\\d+)$");


    protected static final int RESULT_BATCH = 500;

    private final StateResult stateResult;
    private final AciService searchService;
    private final String securityInfo;

    private int lastIndex;
    private int maxBatchDocs = RESULT_BATCH;
    private String stateMatchKey = "statematchid";

    public StoreStateResultsIterator(final StateResult stateResult,
                                     final AciService searchService,
                                     final String securityInfo) {
        this.stateResult = stateResult;
        this.searchService = searchService;
        this.securityInfo = securityInfo;

    }


    @Override
    public boolean hasNext() {
        return stateResult != null && lastIndex < stateResult.getNumhits();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void setMaxBatchDocs(final int maxBatchDocs) {
        if (maxBatchDocs <= 0)  {
            throw new IllegalArgumentException("maxBatchDocs must be greater than 0.");
        }

        this.maxBatchDocs = maxBatchDocs;
    }

    protected boolean setNextQueryParams(final AciParameters params) {
        boolean isSet = false;

        if (stateResult != null && lastIndex < stateResult.getNumhits()) {
            final int remaining = stateResult.getNumhits() - lastIndex;
            final int endIndex = (remaining < maxBatchDocs) ? lastIndex + remaining - 1 : lastIndex + maxBatchDocs - 1;
            final String stateId = String.format("%1$s[%2$d-%3$d]", stateResult.getStateId(), lastIndex, endIndex);
            params.add(stateMatchKey, stateId);
            params.add("maxresults", maxBatchDocs);

            if (StringUtils.isNotBlank(securityInfo)) {
                params.add("securityinfo", securityInfo);
            }

            lastIndex = endIndex + 1;

            isSet = true;
        }

        return isSet;
    }

    protected void setStateMatchKey(final String stateMatchKey) {
        this.stateMatchKey = stateMatchKey;
    }

    protected T executeQuery(final AciParameters params, final AbstractStAXProcessor<T> processor) {
        return searchService.executeAction(params, processor);
    }

    public void destroy() {
        /*
        if (stateResult != null) {
            LOGGER.debug("Expiring storedStateId [" + stateResult.getStateId() + "].");

            final AciParameters params = new AciParameters("tokenmanagement");
            params.add("tokenaction", "expire");
            params.add("stateid", stateResult.getStateId());

            searchService.executeAction(params, new StoreStateProcessor());
        }
        */
    }

    protected static Logger getLogger() {
        return LOGGER;
    }



}
