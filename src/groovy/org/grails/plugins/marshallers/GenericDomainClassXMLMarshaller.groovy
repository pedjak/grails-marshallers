

package org.grails.plugins.marshallers
import grails.converters.XML
import groovy.util.logging.Log4j

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.plugins.marshallers.config.MarshallingConfig
import org.grails.plugins.marshallers.config.MarshallingConfigPool
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
/**
 * 
 * @author dhalupa
 *
 */
@Log4j
class GenericDomainClassXMLMarshaller implements ObjectMarshaller<XML>,NameAwareMarshaller {

	private ProxyHandler proxyHandler
	private GrailsApplication application
	private MarshallingConfigPool configPool

	private static Map<Class,Class> attributeEditors=new HashMap<Class,Class>()

	public GenericDomainClassXMLMarshaller(ProxyHandler proxyHandler, GrailsApplication application, MarshallingConfigPool configPool){
		this.proxyHandler=proxyHandler
		this.application = application
		this.configPool = configPool
	}

	@Override
	public boolean supports(Object object) {
		def clazz=proxyHandler.unwrapIfProxy(object).getClass()
		boolean supports = configPool.get(object.getClass()) != null
		if(log.debugEnabled) log.debug("Support for $clazz is $supports")
		return supports
	}

	@Override
	public void marshalObject(Object value, XML xml)	throws ConverterException {
		if (log.debugEnabled) log.debug("Marshalling of $value started")
		Class clazz = value.getClass()
		GrailsDomainClass domainClass = application.getArtefact(DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()))
		MarshallingConfig mc = configPool.get(clazz)
		BeanWrapper beanWrapper = new BeanWrapperImpl(value)
		if(mc.shouldOutputIdentifier){
			if(mc.identifier){
				if(mc.identifier.size()==1 && mc.identifier[0] instanceof Closure){
					mc.identifier[0].call(value,xml)
				}else{
					mc.identifier.each{
						def	val = beanWrapper.getPropertyValue(it)
						if(val!=null){
							xml.attribute(it,val.toString())
						}
					}
				}
			}else{
				GrailsDomainClassProperty id = domainClass.getIdentifier()
				Object idValue = beanWrapper.getPropertyValue(id.getName())
				if (idValue != null) xml.attribute("id", String.valueOf(idValue))
			}
		}
		if (mc.shouldOutputVersion) {
			Object versionValue = beanWrapper.getPropertyValue(domainClass.getVersion().getName())
			xml.attribute("version", String.valueOf(versionValue))
		}
		
		if (mc.shouldOutputClass) {
			xml.attribute("class", clazz.getName())
		}

		mc.attribute?.each{prop->
			if (log.debugEnabled) log.debug("Trying to write field as xml attribute: $prop on $value")
			Object val = beanWrapper.getPropertyValue(prop)
			if(val!=null){
				def editorEntry=attributeEditors.find{ it.key.isAssignableFrom(val.getClass())}
				if(editorEntry){
					def editor=editorEntry.value.newInstance()
					editor.setValue(val)
					xml.attribute(prop, editor.getAsText())
				}else{
					xml.attribute(prop, val.toString())
				}
			}
		}


		GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties()

		for (GrailsDomainClassProperty property : properties) {
			if(!mc.identifier?.contains(property.getName()) && !mc.ignore?.contains(property.getName()) && !mc.attribute?.contains(property.getName())){
				def serializers=mc?.serializer
				Object val = beanWrapper.getPropertyValue(property.getName())
				if(serializers && serializers[property.name]){
					xml.startNode(property.name)
					serializers[property.name].call(val,xml)
					xml.end()
				}else{
					if(val){
						if (log.debugEnabled) log.debug("Trying to write field as xml element: $property.name on $value")
						writeElement(xml, property, beanWrapper,mc)
					}
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


	private writeElement(XML xml, GrailsDomainClassProperty property, BeanWrapper beanWrapper,MarshallingConfig mc) {
		xml.startNode(property.getName())
		if (!property.isAssociation()) {
			// Write non-relation property
			Object val = beanWrapper.getPropertyValue(property.getName())
			xml.convertAnother(val)
		}
		else {
			Object referenceObject = beanWrapper.getPropertyValue(property.getName())
			if (mc.deep?.contains(property.getName())) {
				renderDeep(referenceObject, xml)
			}
			else {
				if (referenceObject != null) {
					GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()

					// Embedded are now always fully rendered
					if (referencedDomainClass == null || property.isEmbedded() || GCU.isJdk5Enum(property.getType())) {
						xml.convertAnother(referenceObject)
					}
					else if (property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
						asShortObject(referenceObject, xml, referencedDomainClass.getIdentifier(), referencedDomainClass)
					}
					else {
						GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier()
						@SuppressWarnings("unused")
								String refPropertyName = referencedDomainClass.getPropertyName()
						if (referenceObject instanceof Collection) {
							Collection o = (Collection) referenceObject
							for (Object el : o) {
								xml.startNode(xml.getElementName(el))
								asShortObject(el, xml, referencedIdProperty, referencedDomainClass)
								xml.end()
							}
						}
						else if (referenceObject instanceof Map) {
							Map<Object, Object> map = (Map<Object, Object>) referenceObject
							for (Map.Entry<Object, Object> entry : map.entrySet()) {
								String key = String.valueOf(entry.getKey())
								Object o = entry.getValue()
								xml.startNode("entry").attribute("key", key)
								asShortObject(o, xml, referencedIdProperty, referencedDomainClass)
								xml.end()
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
			referenceObject = proxyHandler.unwrapIfProxy(referenceObject)
			if (referenceObject instanceof SortedMap) {
				referenceObject = new TreeMap((SortedMap) referenceObject)
			}
			else if (referenceObject instanceof SortedSet) {
				referenceObject = new TreeSet((SortedSet) referenceObject)
			}
			else if (referenceObject instanceof Set) {
				referenceObject = new HashSet((Set) referenceObject)
			}
			else if (referenceObject instanceof Map) {
				referenceObject = new HashMap((Map) referenceObject)
			}
			else if (referenceObject instanceof Collection) {
				referenceObject = new ArrayList((Collection) referenceObject)
			}
			xml.convertAnother(referenceObject)
		}
	}



	protected void asShortObject(Object refObj, XML xml, GrailsDomainClassProperty idProperty,
			@SuppressWarnings("unused") GrailsDomainClass referencedDomainClass) throws ConverterException {
		MarshallingConfig refClassConfig = configPool.get(referencedDomainClass.clazz, true)
		if(refClassConfig?.identifier){
			if(refClassConfig.identifier.size()==1 && refClassConfig.identifier[0] instanceof Closure){
				refClassConfig.identifier[0].call(refObj,xml)
			}else{
				def wrapper=new BeanWrapperImpl(refObj)
				refClassConfig.identifier.each{
					def	val = wrapper.getPropertyValue(it)
					xml.attribute(it,String.valueOf(val))
				}
			}

		}else{
			Object idValue
			if(proxyHandler instanceof EntityProxyHandler) {
				idValue = ((EntityProxyHandler) proxyHandler).getProxyIdentifier(refObj)
				if(idValue == null) {
					idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName())
				}
			}
			else {
				idValue = new BeanWrapperImpl(refObj).getPropertyValue(idProperty.getName())
			}
			xml.attribute("id",String.valueOf(idValue))
		}
	}




	public static registerAttributeEditor(Class attrType,Class editorType){
		attributeEditors.put(attrType,editorType)
	}

	@Override
	public String getElementName(Object value) {
		Class clazz = value.getClass()
		GrailsDomainClass domainClass = application.getArtefact(DomainClassArtefactHandler.TYPE, ConverterUtil.trimProxySuffix(clazz.getName()))
		MarshallingConfig mc = configPool.get(clazz, true)
		return mc.elementName?:domainClass.logicalPropertyName
	}
}
