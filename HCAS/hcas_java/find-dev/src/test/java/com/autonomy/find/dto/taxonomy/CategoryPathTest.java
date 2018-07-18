package com.autonomy.find.dto.taxonomy;

import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactoryImpl;
import com.autonomy.aci.client.services.StAXProcessor;
import com.autonomy.test.unit.TestUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public class CategoryPathTest {

    private IdolAnnotationsProcessorFactory factory;

    @Before
    public void setUp() {
        factory = new IdolAnnotationsProcessorFactoryImpl();
    }

    @Test
    public void testGetActualPath() throws XMLStreamException {
        final XMLStreamReader xmlStreamReader = TestUtils.getResourceAsXMLStreamReader("/com/autonomy/find/dto/taxonomy/categories.xml");

        final StAXProcessor<List<CategoryPath>> processor = factory.listProcessorForClass(CategoryPath.class);

        final CategoryPath path = processor.process(xmlStreamReader).get(0);

        assertThat(path.getId(), is("D013317_2"));
        assertThat(path.getName(), is("Strikes, Employee"));

        final List<CategoryName> actualPath = path.getActualPath();


        final CategoryName root = actualPath.get(0);

        assertThat(root.getId(), is("0"));
        assertThat(root.getName(), is("root category"));


        final CategoryName second = actualPath.get(1);

        assertThat(second.getId(), is("MeSH"));
        assertThat(second.getName(), is("MeSH"));


        final CategoryName last = actualPath.get(actualPath.size() - 1);

        assertThat(last.getId(), is("D013317_2"));
        assertThat(last.getName(), is("Strikes, Employee"));


        assertThat(actualPath.size(), is(6));
    }
}
