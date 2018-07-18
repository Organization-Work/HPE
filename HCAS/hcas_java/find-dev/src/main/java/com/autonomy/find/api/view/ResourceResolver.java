package com.autonomy.find.api.view;

import org.springframework.core.io.ClassPathResource;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

public class ResourceResolver implements URIResolver{
    @Override
    public Source resolve(final String href, final String base) throws TransformerException {
        if (href != null) {
            final ClassPathResource resource = new ClassPathResource(href);
            try {
                return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
            } catch (final IOException e) {
                throw new TransformerException("Failed to load resource [" + href + "]", e);
            }
        }

        return null;
    }
}
