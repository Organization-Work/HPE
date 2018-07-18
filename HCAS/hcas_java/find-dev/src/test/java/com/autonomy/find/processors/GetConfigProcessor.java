package com.autonomy.find.processors;

import java.io.InputStream;

import com.autonomy.aci.client.services.Processor;
import com.autonomy.aci.client.transport.AciResponseInputStream;
import com.autonomy.aci.client.transport.impl.AciResponseInputStreamImpl;

public class GetConfigProcessor implements Processor<String> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String process(AciResponseInputStream aciResponse) {
        
        // TODO Auto-generated method stub
        return aciResponse.toString();
    }

    

    
    
    
}