package org.grails.plugins.marshallers;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

/**
 * @author Predrag Knezevic
 */
public class JsonMarshallerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "JsonMarshaller";

    public JsonMarshallerArtefactHandler() {
        super(TYPE, JsonMarshallerGrailsClass.class, DefaultJsonMarshallerGrailsClass.class, TYPE);
    }
    
}
