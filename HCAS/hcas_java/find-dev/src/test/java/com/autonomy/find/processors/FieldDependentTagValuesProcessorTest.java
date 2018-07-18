package com.autonomy.find.processors;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/test/java/com/autonomy/find/processors/FieldDependentTagValuesProcessorTest.java#3 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */

import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.test.unit.TestUtils;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class FieldDependentTagValuesProcessorTest {

    private static XMLStreamReader getStream() throws XMLStreamException {
        return TestUtils.getResourceAsXMLStreamReader("/com/autonomy/find/processors/fieldDependentTagValuesProcessorTest.xml");
    }

    @Test
    public void testProcess() throws Exception {
        final List<FieldPair> pairs = (new FieldDependentTagValuesProcessor()).process(getStream());
        assertEquals(35, pairs.size());
        final FieldPair first = pairs.get(0);
        assertEquals("XML/PATIENT/ETHNICITY", first.getPrimaryField());
        assertEquals("XML/PATIENT/RELIGION", first.getSecondaryField());
        assertEquals("WHITE", first.getPrimaryValue());
        assertEquals("NOT SPECIFIED", first.getSecondaryValue());
        assertEquals(18.0, first.getCount());
    }

    @Test(expected = AciErrorException.class)
    public void testProcessError() throws Exception {
        (new FieldDependentTagValuesProcessor()).process(
            TestUtils.getResourceAsXMLStreamReader("/com/autonomy/find/processors/fieldDependentTagValuesProcessorTestError.xml"));
    }
}
