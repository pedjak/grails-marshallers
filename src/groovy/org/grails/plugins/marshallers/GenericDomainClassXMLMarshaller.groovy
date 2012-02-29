package org.grails.plugins.marshallers
import grails.converters.XML

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;

import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller;

class GenericDomainClassXMLMarshaller implements ObjectMarshaller<XML> {
	private static Log LOG = LogFactory.getLog(GenericDomainClassXMLMarshaller.class);
	private String configName;

	public GenericDomainClassXMLMarshaller(String configName){
		LOG.debug("Registered xml domain class marshaller for $configName");
		this.configName=configName;
	}

	@Override
	public boolean supports(Object object) {
		def clazz=object.getClass();
		return ConverterUtil.isDomainClass(clazz) && GCU.getStaticPropertyValue(clazz,'marshalling');
	}

	@Override
	public void marshalObject(Object value, XML converter)	throws ConverterException {
		Class clazz = value.getClass();
		GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
		
	}
}
