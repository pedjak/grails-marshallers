package org.grails.plugins.marshallers;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

/**
 * @author Predrag Knezevic
 */
public class XmlMarshallerArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "XmlMarshaller";

    public XmlMarshallerArtefactHandler() {
        super(TYPE, XmlMarshallerGrailsClass.class, DefaultXmlMarshallerGrailsClass.class, TYPE);
    }

    
}
