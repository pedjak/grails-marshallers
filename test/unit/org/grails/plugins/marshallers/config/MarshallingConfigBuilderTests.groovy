package org.grails.plugins.marshallers.config

import org.grails.plugins.marshallers.GenericDomainClassJSONMarshaller;
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder;

import grails.test.*;
import static org.junit.Assert.*;

class MarshallingConfigBuilderTests extends GrailsUnitTestCase {

	static MarshallingConfigBuilder builder;

	static def testConfig={
		xml{
			'default' elementName:'elName',attributes:['some', 'some1'], ignore:['ig', 'big']
			named attributes:['some', 'some1'], ignore:['ig', 'big']
		}
		json {
			'default' ignore:['some', 'some1']
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
		def c= builder.config
		assertEquals(c.getConfig('xml','default').elementName,'elName')
		assertEquals(c.getConfig('xml','default').attributes.size(),2)
		assertNull(c.getConfig('some','default'))
		assertNull(c.getConfig('xml', 'some'))
		assertEquals(c.getConfigNamesForContentType('some').size(),0)
		assertEquals(c.getConfigNamesForContentType('xml').size(),2)
		assertEquals(c.getConfigNamesForContentType('json').size(),1)
	}

	void test(){
		def c=GenericDomainClassJSONMarshaller.class.getDeclaredConstructor(String.class);
		def o=c.newInstance('default');
		println o;
	}
}
