package com.autonomy.find.util;

import com.autonomy.aci.client.transport.AciParameter;
import com.autonomy.find.dto.SearchRequestData;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Queries {

    public static String buildQueryWithExtension(final SearchRequestData requestData) {
        return buildQueryWithExtension(requestData.getQuery(), requestData.getQueryExtension());
    }

    public static String buildQueryWithExtension(final String query, final String extension) {

        final StringBuilder queryString =
                new StringBuilder()
                        .append("(")
                        .append(StringUtils.defaultIfEmpty(query, "*"))
                        .append(")");

        if (StringUtils.isNotBlank(extension)) {
            queryString.append(" AND (").append(extension).append(")");
        }

        return queryString.toString();
    }

    public static List<AciParameter> queryToAciParams(final String queryString) {
        final List<AciParameter> params = new ArrayList<AciParameter>();
        if (StringUtils.isNotEmpty(queryString)) {
            final List<NameValuePair> pairs = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
            for(final NameValuePair pair : pairs) {
                params.add(new AciParameter(pair.getName(), pair.getValue()));
            }
        }

        return params;
    }
}
