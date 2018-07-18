package com.autonomy.vertica.test;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.autonomy.vertica.fields.Field;
//import com.autonomy.vertica.fields.FilterFields;
import com.autonomy.vertica.templates.FilterTemplate;

public class VerticaTest {
    public static void main(String[] args) {
        ApplicationContext context =
                new ClassPathXmlApplicationContext("/**/beans.xml");
        
        FilterTemplate filterTemplate = (FilterTemplate) context.getBean("filterTemplate");
       // System.out.println(filterTemplate.getTotalHits());


    /*
     *Get JDBC connection to vertica *
     */
       
        /*List<FilterFields> dlist = filterTemplate.listDemographics();
        ObjectMapper mapper = new ObjectMapper();

        for (FilterFields record : dlist) {
            System.out.println("+++++++++++++++++++ in the for loop ++++++++++++++++++");
            System.out.println("Field Values... " + record.getSubjectID() + " " + record.getEthnicityDescr());
            try {
                System.out.println("Value " + dlist);
                //Convert object to JSON string
                String jsonInString = mapper.writeValueAsString(record);
                System.out.println("First Json print... " + jsonInString);

                //Convert object to JSON string and pretty print
                jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
                System.out.println("Second Json print... " + jsonInString);
            }
            catch(JsonGenerationException e){
                e.printStackTrace();
            }catch(JsonMappingException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
        }*/
        
      /*  Field record = filterTemplate.listParametricFields();
        ObjectMapper mapper = new ObjectMapper();

       //for (Field record : dlist) {
            System.out.println("+++++++++++++++++++ in the for loop ++++++++++++++++++");
            System.out.println("Field Values... " + record.getNumValues() + " " + record.getTotalValues());
            try {
                //System.out.println("Value " + dlist);
                //Convert object to JSON string
                String jsonInString = mapper.writeValueAsString(record);
                System.out.println("First Json print... " + jsonInString);

                //Convert object to JSON string and pretty print
                jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record);
                System.out.println("Second Json print... " + jsonInString);
            }
            catch(JsonGenerationException e){
                e.printStackTrace();
            }catch(JsonMappingException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
    //}*/
    }
}