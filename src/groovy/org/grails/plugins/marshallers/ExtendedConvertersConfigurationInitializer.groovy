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

import grails.converters.JSON;
import grails.converters.XML;
import grails.util.GrailsConfig;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer;
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration;
import org.grails.plugins.marshallers.config.MarshallingConfig
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
class ExtendedConvertersConfigurationInitializer extends ConvertersConfigurationInitializer {

	

	@Override
	public void initialize() {
		super.initialize()
		processGrailsConfigurations()
	}

	protected def processGrailsConfigurations() {
		def application=applicationContext.grailsApplication
		ProxyHandler proxyHandler = applicationContext.getBean(ProxyHandler.class);
		MarshallingConfigBuilder delegate=new MarshallingConfigBuilder();
		def namedConfigs=new HashSet<String>();
		application.domainClasses.each{
			def mc=GCU.getStaticPropertyValue(it.clazz,'marshalling');
			if(mc){
				mc.setDelegate(delegate)
				mc.call()
				MarshallingConfig c=new MarshallingConfig(config:delegate.config);
				['xml', 'json'].each {type->namedConfigs<< c.getConfigNamesForContentType(type)}
			}
		}
		namedConfigs.flatten().each{name->
			if(name=='default'){
				XML.registerObjectMarshaller(new GenericDomainClassXMLMarshaller('default',proxyHandler));
				JSON.registerObjectMarshaller(new GenericDomainClassJSONMarshaller('default',proxyHandler));
			}else{
				XML.createNamedConfig(name) {
					it.registerObjectMarshaller(new GenericDomainClassXMLMarshaller(name,proxyHandler));
				}
				JSON.createNamedConfig(name) {
					it.registerObjectMarshaller(new GenericDomainClassJSONMarshaller(name,proxyHandler));
				}
			}
		}
		[xml: XML, json: JSON].each { type, converterClass ->
			def marshallerCfg = GrailsConfig.get("grails.plugins.marshallers.${type}")
			processConfig(marshallerCfg, converterClass, type)
		}
	}

	public void processConfig(Closure cfg, Class converterClass, type) {
		def converterCfg = ConvertersConfigurationHolder.getConverterConfiguration(converterClass)
		def builder = new ConfigurationBuilder(type: type, applicationContext: applicationContext, cfg: converterCfg, log: LOG, converterClass: converterClass, cfgName: "default")
		builder.registerSpringMarshallers()
		if (cfg != null) {
			cfg.delegate = builder
			cfg.resolveStrategy = Closure.DELEGATE_FIRST;
			cfg.call()
		}
	}

	
}

