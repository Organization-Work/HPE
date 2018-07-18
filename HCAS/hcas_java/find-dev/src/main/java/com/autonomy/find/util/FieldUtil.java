package com.autonomy.find.util;


import com.autonomy.common.io.IOUtils;
import com.autonomy.find.fields.*;
import com.autonomy.find.fields.FiltersCategory;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldUtil.class);


    public static String getSearchViewFieldnamePrefix(final String viewRootElement) {
        // The prefix depends on how IDOL is configured, it's often DOCUMENT, but in this
        // healthcare demo case, it's DOCUMENTS
        return "^((?:XML|xml)/)?" + viewRootElement + "/";
    }


    /**
     *
     *
     * @param fileName
     * @return
     * @throws java.io.IOException
     */
    public static String loadFieldValuesJSON(final String fileName) throws IOException {
         return IOUtils.toString(FieldUtil.class.getClassLoader().getResourceAsStream(fileName));
    }


    public static List<FilterField> loadFilterFields(final String filename, final String filtersSchema) throws IOException {
        final FilterFields hcaFields;
        try {
            final InputStream fieldsInStream = FieldUtil.class.getClassLoader().getResourceAsStream(filename);
            final URL schemaUrl = FieldUtil.class.getClassLoader().getResource(filtersSchema);


            final JAXBContext jaxbContext = JAXBContext.newInstance(FilterFields.class);
            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final Schema schema = schemaFactory.newSchema(schemaUrl);
            unmarshaller.setSchema(schema);

            hcaFields = (FilterFields) unmarshaller.unmarshal( fieldsInStream );
        } catch (final Throwable e) {
            final String errorMsg = String.format("Error parsing [%1$s] with schema [%2$s]", filename, filtersSchema);
            LOGGER.error(errorMsg, e);
            throw new RuntimeException(errorMsg);
        }

        final List<FilterField> fields = new ArrayList<FilterField>();

        for(final FiltersCategory category : hcaFields.getCategory()) {
            final GroupMeta catMeta = getMetadata(category);

            for(final Object child : category.getGroupOrFilterField()) {
                if (child instanceof FilterField) {
                    final FilterField field = (FilterField) child;
                    field.setParentCategory(catMeta);
                    fields.add(field);

                }  else if (child instanceof FiltersGroup) {
                    final FiltersGroup group = (FiltersGroup) child;
                    final GroupMeta groupMeta = getMetadata(group);

                    for(final FilterField field : group.getFilterField()) {
                        field.setParentCategory(catMeta);
                        field.setParentGroup(groupMeta);
                        fields.add(field);

                    }

                }
            }
        }

        return fields;

    }


    private static GroupMeta getMetadata(final GroupMeta group) {
        final GroupMeta meta = new GroupMeta();
        meta.setName(group.getName());
        meta.setWhenOp(group.getWhenOp());
        meta.setOrdinal(group.getOrdinal());
        meta.setSortByFieldOrdinal(group.isSortByFieldOrdinal());
        return meta;
    }

}
