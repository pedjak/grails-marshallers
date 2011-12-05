package org.grails.plugins.marshallers;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
public class DefaultXmlMarshallerGrailsClass extends AbstractInjectableGrailsClass
        implements XmlMarshallerGrailsClass {

    public DefaultXmlMarshallerGrailsClass(Class<?> clazz) {
        super(clazz, XmlMarshallerArtefactHandler.TYPE);
    }

}
