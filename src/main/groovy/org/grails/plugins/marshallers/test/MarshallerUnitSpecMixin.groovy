package org.grails.plugins.marshallers.test

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer;
import org.grails.plugins.marshallers.ExtendedConvertersConfigurationInitializer;
import org.grails.plugins.marshallers.JsonMarshallerArtefactHandler
import org.grails.plugins.marshallers.XmlMarshallerArtefactHandler
import org.junit.Before;
import org.junit.BeforeClass;

import grails.test.mixin.support.GrailsUnitTestMixin;

class MarshallerUnitSpecMixin extends GrailsUnitTestMixin{

	
	def setup(){
		// this has to be called first as there's no declarative way to enforce
		// execution order
		initGrailsApplication()

		grailsApplication.registerArtefactHandler(new JsonMarshallerArtefactHandler())
		grailsApplication.registerArtefactHandler(new XmlMarshallerArtefactHandler())
		defineBeans {
			convertersConfigurationInitializer(ConvertersConfigurationInitializer)
			extendedConvertersConfigurationInitializer(ExtendedConvertersConfigurationInitializer)
		}
		applicationContext.convertersConfigurationInitializer.initialize(grailsApplication)
		applicationContext.extendedConvertersConfigurationInitializer.initialize()
	}

}
