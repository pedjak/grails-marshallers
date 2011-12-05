package org.grails.plugins.marshallers;

import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
public class DefaultJsonMarshallerGrailsClass extends AbstractInjectableGrailsClass
        implements JsonMarshallerGrailsClass {

    public DefaultJsonMarshallerGrailsClass(Class<?> clazz) {
        super(clazz, JsonMarshallerArtefactHandler.TYPE);
    }

}
