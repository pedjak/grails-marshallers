/*******************************************************************************
 * Copyright 2011 Predrag Knezevic
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.grails.plugins.marshallers

import grails.converters.JSON
import grails.converters.XML
import groovy.util.logging.Log4j;

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.plugins.marshallers.config.MarshallingConfig
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
@Log4j
class ExtendedConvertersConfigurationInitializer implements ApplicationContextAware {
	
	private ApplicationContext applicationContext
	private GrailsApplication application

   
   
    void initialize() {
        application=applicationContext.grailsApplication
		ProxyHandler proxyHandler = applicationContext.getBean(ProxyHandler.class)
		
		def configNames=buildConfigNames()
		
		configNames.xml.flatten().each{name->
			Map configCache=buildNamedConfigCache('xml', name)
			if(name=='default'){
				XML.registerObjectMarshaller(new GenericDomainClassXMLMarshaller(proxyHandler, application,configCache))
			}else{
				XML.createNamedConfig(name) {
					it.registerObjectMarshaller(new GenericDomainClassXMLMarshaller(proxyHandler, application,configCache))
				}
			}
		}
		configNames.json.flatten().each{name->
			Map configCache=buildNamedConfigCache('json', name)
			if(name=='default'){
				JSON.registerObjectMarshaller(new GenericDomainClassJSONMarshaller(proxyHandler,application,configCache))
			}else{
				JSON.createNamedConfig(name) {
					it.registerObjectMarshaller(new GenericDomainClassJSONMarshaller(proxyHandler,application,configCache));
				}
			}
		}
        [xml: XML, json: JSON].each { type, converterClass ->
            def marshallerCfg = application.config?.grails?.plugins?.marshallers?."${type}"
            processConfig(marshallerCfg, converterClass, type)            
        }
    }
    
   
	private Map buildConfigNames(){
		MarshallingConfigBuilder builder=new MarshallingConfigBuilder()
		def nc=[xml:[] as Set,json:[] as Set]
		application.domainClasses.each{
			Closure mc=GCU.getStaticPropertyValue(it.clazz,'marshalling')
			if(mc){
				mc.delegate=builder
				mc.resolveStrategy=Closure.DELEGATE_FIRST
				mc()
				MarshallingConfig c=builder.config
				['xml', 'json'].each {type->nc[type] << c.findConfigNames(type)}
			}
		}
		nc
		
	}
    
    private void processConfig(cfg, Class converterClass, type) {
        def converterCfg = ConvertersConfigurationHolder.getConverterConfiguration(converterClass)
        def builder = new ConfigurationBuilder(type: type, applicationContext: applicationContext, cfg: converterCfg, log: log, converterClass: converterClass, cfgName: "default")
        builder.registerSpringMarshallers()
        if (cfg != null && cfg instanceof Closure) {
            cfg.delegate = builder
            cfg.resolveStrategy = Closure.DELEGATE_FIRST
            cfg.call()
        }
    }
	
	private Map buildNamedConfigCache(String type,String configName){
		Map configCache=[:]
		application.domainClasses.each { domainClass ->
			Closure mc=GCU.getStaticPropertyValue(domainClass.clazz,'marshalling');
			if(mc){
				MarshallingConfigBuilder builder=new MarshallingConfigBuilder();
				mc.delegate=builder
				mc()
				def namedConfig=builder.config.findNamedConfig(type,configName)
				if(namedConfig){
					configCache[domainClass.clazz]=namedConfig
				}else{
					configCache[domainClass.clazz]=builder.config.findNamedConfig(type,'default')
				}
			}
		}
		configCache
	}
	
	void setApplicationContext(ApplicationContext applicationContext){
		this.applicationContext=applicationContext
	}

    
}

