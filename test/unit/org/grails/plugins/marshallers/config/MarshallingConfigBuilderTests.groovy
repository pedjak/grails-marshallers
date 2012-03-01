package org.grails.plugins.marshallers.config

import org.grails.plugins.marshallers.GenericDomainClassJSONMarshaller;
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder;

import grails.test.*;
import static org.junit.Assert.*;

class MarshallingConfigBuilderTests extends GrailsUnitTestCase {

	static MarshallingConfigBuilder builder;

	static def testConfig={
		xml{
			'default' {
				elementName 'elName'
				attributes 'some', 'some1'
				ignore 'ig', 'big'
			}
			named {
				attributes 'some', 'some1'
				ignore 'ig', 'big'
			}
		}
		json {
			'default'{ ignore 'some', 'some1' }
		}
	}

	protected void setUp() {
		super.setUp()
		builder=new MarshallingConfigBuilder();
	}

	protected void tearDown() {
		super.tearDown()
	}

	void testBuilder() {
		testConfig.setDelegate(builder)
		testConfig.call()
		def c= new MarshallingConfig(config: builder.config);
		println c.config;
		assertEquals(c.getConfig('xml','default').elementName,'elName')
		assertNull(c.getConfig('xml','named').elementName)
		assertEquals(c.getConfig('xml','default').attributes.size(),2)
		assertEquals(c.getConfig('some','default').size(),0)
		assertEquals(c.getConfig('xml', 'some').size(),0)
		assertEquals(c.getConfigNamesForContentType('some').size(),0)
		assertEquals(c.getConfigNamesForContentType('xml').size(),2)
		assertEquals(c.getConfigNamesForContentType('json').size(),1)
	}
}
