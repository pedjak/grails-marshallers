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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration
import org.grails.plugins.marshallers.config.MarshallingConfig
import org.grails.plugins.marshallers.config.MarshallingConfigPool
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.codehaus.groovy.grails.commons.GrailsClassUtils

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
class ExtendedConvertersConfigurationInitializer implements ApplicationContextAware {
	
	private ApplicationContext applicationContext
	private GrailsApplication application

    private marshallerClasses = [(XML):GenericDomainClassXMLMarshaller, (JSON):GenericDomainClassJSONMarshaller]
    

    // marshallers registered from Config should take precedence over marshalling configuration 
    // declared within domain classes    
    private final static DC_MARSHALLER_PRIORITY = DefaultConverterConfiguration.DEFAULT_PRIORITY -1
    private final static DEFAULT_CONFIGURATION_NAME = 'default'
    
    void initialize() {
        application=applicationContext.grailsApplication
		ProxyHandler proxyHandler = applicationContext.getBean(ProxyHandler.class)
		
        // collect marshalling configurations declared in domain classes
        def dcMarshallingConfigs = collectMarshallingConfigsFromDomainClasses()
        
        // check if there are marshalling configuration declared in the application config and process them
        [xml: XML, json: JSON].each { type, converterClass ->
            def marshallerCfg = application.config?.grails?.plugins?.marshallers?."${type}"
            def parentConf = processConfig(marshallerCfg, converterClass, type)
            // wire config pools to their parent pool
            parentConf.each { childName, parentName ->
                dcMarshallingConfigs.get(type)?.get(childName)?.parentPool = dcMarshallingConfigs.get(type)?.get(parentName) 
            }
        }
		
        // register marshallers for given domain classes and the declared marshalling configurations
        marshallerClasses.each { type, marshallerClass ->
            
            def typeName = GrailsClassUtils.getShortName(type).toLowerCase()
             
    		dcMarshallingConfigs.get(typeName).each{ name, configPool ->
    			def marshaller = marshallerClass.newInstance(proxyHandler, application, configPool)
                
                def cfg = name == DEFAULT_CONFIGURATION_NAME ? type : ConvertersConfigurationHolder.getNamedConverterConfiguration(name, type)
                if (!cfg) {
                    type.createNamedConfig(name) {
                        cfg = it
                    }
                }
                cfg.registerObjectMarshaller(marshaller, DC_MARSHALLER_PRIORITY)
    		}
		}

    }
    
    /**
     * 
     * @return a map having 'xml' or 'json' for keys and another map as value, where key is
     * a configuration name, and the value is MarshallingConfigPool instance
     */
	private Map collectMarshallingConfigsFromDomainClasses(){
		def nc=[:]
		application.domainClasses.each{
            def clazz = it.clazz
			Closure mc=GCU.getStaticPropertyValue(clazz,'marshalling')
			if(mc){
                MarshallingConfigBuilder builder=new MarshallingConfigBuilder(clazz)                
				mc.delegate=builder
				mc.resolveStrategy=Closure.DELEGATE_FIRST
				mc()
				MarshallingConfig c=builder.config
				['xml', 'json'].each { type ->
                    def map = nc[type]
                    if (map == null) {
                        map = [:]
                        nc[type] = map
                    } 
                    for (name in c.findConfigNames(type)) {
                        def configs = map[name]
                        if (configs == null) {
                            configs = []
                            map[name] = configs
                        } 
                        def list = c.findNamedConfig(type, name)
                        if (list) {
                            configs.addAll(list)
                        } else {
                            configs.addAll(c.findNamedConfig(type, DEFAULT_CONFIGURATION_NAME))
                        } 
                    }
                }
			}
		}
		nc
		def configPools = [:]
        nc.each { type, map ->
            def poolMap = [:]
            configPools[(type)] = poolMap
            map.each { name, configs ->
                poolMap[(name)] = new MarshallingConfigPool(configs)
            }
        }
        configPools
	}
    
    private def processConfig(cfg, Class converterClass, type) {
        def converterCfg = ConvertersConfigurationHolder.getConverterConfiguration(converterClass)
        def parentConf = [:]
        def builder = new ConfigurationBuilder(parentConf: parentConf, type: type, applicationContext: applicationContext, cfg: converterCfg, log: log, converterClass: converterClass, cfgName: DEFAULT_CONFIGURATION_NAME)
        builder.registerSpringMarshallers()
        if (cfg != null && cfg instanceof Closure) {
            cfg.delegate = builder
            cfg.resolveStrategy = Closure.DELEGATE_FIRST
            cfg.call()
        }
        parentConf
    }
	
	void setApplicationContext(ApplicationContext applicationContext){
		this.applicationContext=applicationContext
	}
    
}

