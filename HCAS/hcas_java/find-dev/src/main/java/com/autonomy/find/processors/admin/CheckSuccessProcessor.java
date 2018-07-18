package com.autonomy.find.processors.admin;

import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;

import javax.xml.stream.XMLStreamReader;


public abstract class CheckSuccessProcessor<T> extends AbstractStAXProcessor<T> {

    public final static String RESPONSE = "response";
    public final static String RESPONSE_DATA = "responsedata";
    public final static String RESPONSE_SUCCESS = "SUCCESS";

    @Override
    public abstract T process(XMLStreamReader xmlStreamReader);

    public boolean isElement(String expected, String actual) {
        return expected.toLowerCase().equals(actual.toLowerCase());
    }
    public boolean isResponseElement(String element) {
        return element.toLowerCase().equals(RESPONSE.toLowerCase());
    }
    public boolean isSuccess(String actual) {
        return RESPONSE_SUCCESS.toLowerCase().equals(actual.toLowerCase());
    }
    public boolean isResponseData(String actual) {
        return RESPONSE_DATA.toLowerCase().equals(actual.toLowerCase());
    }

}
