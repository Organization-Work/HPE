package com.autonomy.find.api.view;

import net.sf.saxon.AugmentedSource;
import net.sf.saxon.TransformerFactoryImpl;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.w3c.dom.Node;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

public class SaxonXsltView extends XsltView{
    public SaxonXsltView() {
        super();
        super.setTransformerFactoryClass(net.sf.saxon.TransformerFactoryImpl.class);
    }

    @Override
    public void setTransformerFactoryClass(Class transformerFactoryClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Source convertSource(Object source) throws Exception {
        Object srcObj = source;
        if (source instanceof Node) {
            AugmentedSource augmentedSource = AugmentedSource.makeAugmentedSource(new DOMSource((Node) source));
            augmentedSource.setWrapDocument(false);
            srcObj = ((TransformerFactoryImpl) getTransformerFactory()).getConfiguration().buildDocument(augmentedSource);
        }

        return super.convertSource(srcObj);
    }

}
