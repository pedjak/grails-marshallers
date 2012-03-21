package org.grails.plugins.marshallers
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import grails.converters.XML

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.support.proxy.ProxyHandler;
import org.codehaus.groovy.grails.web.converters.ConverterUtil;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException;
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;

import org.codehaus.groovy.grails.web.converters.marshaller.json.DomainClassMarshaller;
import org.grails.plugins.marshallers.config.MarshallingConfig;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

class GenericDomainClassXMLMarshaller implements ObjectMarshaller<XML>,NameAwareMarshaller {
	private static Log LOG = LogFactory.getLog(GenericDomainClassXMLMarshaller.class);
	private String configName;
	private final boolean includeVersion=false;
	private ProxyHandler proxyHandler;

	public GenericDomainClassXMLMarshaller(String configName, ProxyHandler proxyHandler){
		LOG.debug("Registered xml domain class marshaller for $configName");
		this.configName=configName;
		this.proxyHandler=proxyHandler;
	}

	@Override
	public boolean supports(Object object) {
		def clazz=object.getClass();
		boolean s= ConverterUtil.isDomainClass(clazz) && GCU.getStaticPropertyValue(clazz,'marshalling');
		LOG.debug("Support for $object  is $s")
		return s;
	}

	@Override
	public void marshalObject(Object value, XML xml)	throws ConverterException {
		LOG.debug("Marshalling of $value started")
		Class clazz = value.getClass();
		GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
		def mc=MarshallingConfig.getForClass(clazz).getConfig('xml',configName);
		BeanWrapper beanWrapper = new BeanWrapperImpl(value);
		if(mc.ignoreIdentifier==null || !mc.ignoreIdentifier){
			if(mc.identifier){
				if(mc.identifier.size()==1 && mc.identifier[0] instanceof Closure){
					mc.identifier[0].call(value,xml)
				}else{
					mc.identifier.each{
						def	val = beanWrapper.getPropertyValue(it);
						xml.attribute(it,String.valueOf(val));
					}
				}
			}else{
				GrailsDomainClassProperty id = domainClass.getIdentifier();
				Object idValue = beanWrapper.getPropertyValue(id.getName());
				if (idValue != null) xml.attribute("id", String.valueOf(idValue));
			}
		}
		if (includeVersion) {
			Object versionValue = beanWrapper.getPropertyValue(domainClass.getVersion().getName());
			xml.attribute("version", String.valueOf(versionValue));
		}
		if(mc.attribute){
			mc.attribute.each{prop->
				LOG.debug("Trying to write field as xml attribute: $prop on $value")
				Object val = beanWrapper.getPropertyValue(prop);
				if(val!=null){
					xml.attribute(prop, String.valueOf(val))
				}
			}
		}

		GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties();

		for (GrailsDomainClassProperty property : properties) {
			if(!isIn(mc,'identifier',property.getName()) && !isIn(mc,'ignore',property.getName()) && !isIn(mc,'attribute',property.getName())){
				def serializers=mc?.serializer
				if(serializers && serializers[property.name]){
					Object val = beanWrapper.getPropertyValue(property.getName());
					serializers[property.name].call(val,xml)
				}else{
					LOG.debug("Trying to write field as xml element: $property.name on $value")
					writeElement(xml, property, beanWrapper,mc);
				}
			}
		}
		if(mc.virtual){
			mc.virtual.each{prop,callable->
				xml.startNode(prop)
				mc.virtual[prop].call(value,xml)
				xml.end()
			}
		}
	}


	private writeElement(XML xml, GrailsDomainClassProperty property, BeanWrapper beanWrapper,mc) {
		xml.startNode(property.getName());
		if (!property.isAssociation()) {
			// Write non-relation property
			Object val = beanWrapper.getPropertyValue(property.getName());
			xml.convertAnother(val);
		}
		else {
			Object referenceObject = beanWrapper.getPropertyValue(property.getName());
			if (isIn(mc,'deep',property.getName())) {
				renderDeep(referenceObject, xml)
			}
			else {
				if (referenceObject != null) {
					GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass();

					// Embedded are now always fully rendered
					if (referencedDomainClass == null || property.isEmbedded() || GCU.isJdk5Enum(property.getType())) {
						xml.convertAnother(referenceObject);
					}
					else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
						asShortObject(referenceObject, xml, referencedDomainClass.getIdentifier(), referencedDomainClass);
					}
					else {
						GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier();
						@SuppressWarnings("unused")
								String refPropertyName = referencedDomainClass.getPropertyName();
						if (referenceObject instanceof Collection) {
							Collection o = (Collection) referenceObject;
							for (Object el : o) {
								xml.startNode(xml.getElementName(el));
								asShortObject(el, xml, referencedIdProperty, referencedDomainClass);
								xml.end();
							}
						}
						else if (referenceObject instanceof Map) {
							Map<Object, Object> map = (Map<Object, Object>) referenceObject;
							for (Map.Entry<Object, Object> entry : map.entrySet()) {
								String key = String.valueOf(entry.getKey());
								Object o = entry.getValue();
								xml.startNode("entry").attribute("key", key);
								asShortObject(o, xml, referencedIdProperty, referencedDomainClass);
								xml.end();
							}
						}
					}
				}
			}
		}
		xml.end()
	}

	private void renderDeep(referenceObject, XML xml) {
		if (referenceObject != null) {
			referenceObject = proxyHandler.unwrapIfProxy(referenceObject);
			if (referenceObject instanceof SortedMap) {
				referenceObject = new TreeMap((SortedMap) referenceObject);
			}
			else if (referenceObject instanceof SortedSet) {
				referenceObject = new TreeSet((SortedSet) referenceObject);
			}
			else if (referenceObject instanceof Set) {
				referenceObject = new HashSet((Set) referenceObject);
			}
			else if (referenceObject instanceof Map) {
				referenceObject = new HashMap((Map) referenceObject);
			}
			else if (referenceObject instanceof Collection) {
				referenceObject = new ArrayList((Collection) referenceObject);
			}
			xml.convertAnother(referenceObject);
		}
	}



	protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty,
	@SuppressWarnings("unused") GrailsDomainClass referencedDomainClass) throws ConverterException {
		def refClassConfig=MarshallingConfig.getForClass(referencedDomainClass.clazz)?.getConfig('xml',configName);
		if(refClassConfig && refClassConfig.identifier){
			if(refClassConfig.identifier.size()==1 && refClassConfig.identifier[0] instanceof Closure){
				refClassConfig.identifier[0].call(refObj,xml)
			}else{
				def wrapper=new BeanWrapperImpl(refObj);
				refClassConfig.identifier.each{
					def	val = wrapper.getPropertyValue(it);
					xml.attribute(it,String.valueOf(val));
				}
			}

		}else{
			Object idValue;
			if(proxyHandler instanceof EntityProxyHandler) {
				idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj);
				if(idValue == null) {
					idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
				}
			}
			else {
				idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName());
			}
			xml.attribute("id",String.valueOf(idValue));
		}
	}

	private boolean isIn(config,configName,fieldName){
		return config[configName]!=null?config[configName].find{it==fieldName}!=null:false;

	}

	@Override
	public String getElementName(Object value) {
		Class clazz = value.getClass();
		GrailsDomainClass domainClass = ConverterUtil.getDomainClass(clazz.getName());
		def mc=MarshallingConfig.getForClass(clazz).getConfig('xml',configName);
		return mc.elementName?:domainClass.logicalPropertyName;
	}
}
