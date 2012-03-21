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
				identifier 'some','id'
				elementName 'elName'
				attributes 'some', 'some1'
				ignore 'ig', 'big'
			}
			named {
				identifier {val,xml->println val}
				attributes 'some', 'some1'
				ignore 'ig', 'big'
				serializer {
					taxonomies {val,xml-> println val }
				}
				virtual {
					rootNodes { form,xml ->println form	}
				}
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
		assertEquals(c.getConfig('xml','default').elementName,'elName')
		assertNull(c.getConfig('xml','named').elementName)
		assertTrue(c.getConfig('xml','named').serializer.taxonomies instanceof Closure)
		assertTrue(c.getConfig('xml','named').virtual.rootNodes instanceof Closure)
		assertTrue(c.getConfig('xml','default').identifier.size()==2)
		assertTrue(c.getConfig('xml','named').identifier[0] instanceof Closure)
		assertEquals(c.getConfig('xml','default').attributes.size(),2)
		assertEquals(c.getConfig('some','default').size(),0)
		assertEquals(c.getConfig('xml', 'some').size(),0)
		assertEquals(c.getConfigNamesForContentType('some').size(),0)
		assertEquals(c.getConfigNamesForContentType('xml').size(),2)
		assertEquals(c.getConfigNamesForContentType('json').size(),1)
	}
}
