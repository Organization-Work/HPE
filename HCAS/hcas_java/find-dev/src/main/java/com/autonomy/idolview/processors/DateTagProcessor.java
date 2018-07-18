package com.autonomy.idolview.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.idolview.DateCounts;
import com.autonomy.idolview.DatePeriod;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.TimeZone;

/**
* $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/processors/DateTagProcessor.java#2 $
* <p/>
* Copyright (c) 2012, Autonomy Systems Ltd.
* <p/>
* Last modified by $Author: tungj $ on $Date: 2013/11/08 $
*/
public final class DateTagProcessor extends AbstractStAXProcessor<DateCounts> {
    private final DatePeriod period;
	private final SimpleDateFormat format;

    public DateTagProcessor(final DatePeriod period) {
        this.period = period;

//		We need to parse the date string (always UTC) since the value is an autn:date, which isn't always epoch seconds
// 		e.g. for pre-epoch values
//		<autn:value date="23:00:00 30/03/1970">7686000</autn:value>
//		<autn:value date="23:00:00 28/02/1970">5094000</autn:value>
//		<autn:value date="00:00:00 01/12/1969">4294857120</autn:value>
		this.format = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
		this.format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public DateCounts process(final XMLStreamReader xmlStreamReader) {
        try {
            final DateCounts counts = new DateCounts(period);
            final LinkedHashMap<Long,Integer> dates = counts.getDates();

            while(xmlStreamReader.hasNext()) {
                final int evtType = xmlStreamReader.next();
                switch (evtType) {
                    case XMLEvent.START_ELEMENT:
                        final String localName = xmlStreamReader.getLocalName();
                        if ("autn:value".equals(localName)) {
							final int count = Integer.parseInt(xmlStreamReader.getAttributeValue(null, "count"));
							final String date = xmlStreamReader.getAttributeValue(null, "date");

							try {
								final long epoch = Math.round(format.parse(date).getTime() / 1000);
								dates.put(epoch, count);
							} catch (ParseException e) {
								throw new ProcessorException(String.format("Invalid date '%s'", date));
							}
                        }
                        break;
                }
            }

            return counts;
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }
}
