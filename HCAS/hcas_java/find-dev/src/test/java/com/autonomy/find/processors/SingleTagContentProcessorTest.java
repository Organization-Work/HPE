package com.autonomy.find.processors;

import com.autonomy.test.unit.TestUtils;
import static junit.framework.Assert.assertEquals;

import com.google.gson.internal.Pair;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.List;

import static com.autonomy.find.util.CollUtils.list;

public class SingleTagContentProcessorTest {

    private static XMLStreamReader getStream() throws XMLStreamException {
        return TestUtils.getResourceAsXMLStreamReader("/com/autonomy/find/processors/singleTagTestContent.xml");
    }

    @Test
    public void testProcess() throws Exception {

        @SuppressWarnings("unchecked")
        final List<Pair<String, String>> checks = list(
                new Pair<>("autn:boolean", "demo content"),
                new Pair<>("response", "SUCCESS"),
                new Pair<>("action", "CATEGORYGETTRAINING")
        );

        for (final Pair<String, String> pair : checks) {
            assertEquals(pair.second, new SingleTagContentProcessor(pair.first).process(getStream()));
        }
    }
}
