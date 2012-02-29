package org.grails.plugins.marshallers
import grails.converters.JSON

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;

import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller;

class GenericDomainClassJSONMarshaller implements ObjectMarshaller<JSON> {
	private static Log LOG = LogFactory.getLog(GenericDomainClassJSONMarshaller.class);
	private String configName;

	public GenericDomainClassJSONMarshaller(String configName){
		LOG.debug("Registered json domain class marshaller for $configName");
		this.configName=configName;
	}

	@Override
	public boolean supports(Object object) {
		def clazz=object.getClass();
		return ConverterUtil.isDomainClass(clazz) && GCU.getStaticPropertyValue(clazz,'marshalling');
	}

	@Override
	public void marshalObject(Object object,JSON converter)	throws ConverterException {
	}
}
