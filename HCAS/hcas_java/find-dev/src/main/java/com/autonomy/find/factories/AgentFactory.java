package com.autonomy.find.factories;

import com.autonomy.find.dto.AgentOptions;

import java.util.List;
import java.util.Map;

public class AgentFactory {


    public static AgentOptions agentByAidOrName(
            final String aid,
            final String name
    ) {
        return new AgentOptions(false, aid, name, null, null, null, null, null, null, null, null, null);
    }

    public static AgentOptions agentForCreate(
			final boolean unreadOnly,
			final String name,
			final List<String> concepts,
			final List<String> documents,
			final List<String> databases,
			final Long startDate,
			final Map<String, List<String>> filters,
			final Double minScore,
			final Integer dispChars,
            final String categoryId
	) {
        return new AgentOptions(unreadOnly, null, name, null, concepts, documents, databases, startDate, filters, minScore, dispChars, categoryId);
    }

    public static AgentOptions agentForEdit(
			final boolean unreadOnly,
			final String aid,
			final String name,
			final String newName,
			final List<String> concepts,
			final List<String> documents,
			final List<String> databases,
			final Long startDate,
			final Map<String, List<String>> filters,
            final Double minScore,
            final Integer dispChars,
            final String categoryId
	) {
        return new AgentOptions(unreadOnly, aid, name, newName, concepts, documents, databases, startDate, filters, minScore, dispChars, categoryId);
    }

    public static AgentOptions agentForRename(
            final String aid,
            final String name,
            final String newName
    ) {
        return new AgentOptions(false, aid, name, newName, null, null, null, null, null, null, null, null);
    }
}
