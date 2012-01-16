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

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer;
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration;

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
        [xml: XML, json: JSON].each { type, converterClass ->
            def marshallerCfg = applicationContext.grailsApplication.config?.grails?.plugins?.marshallers?."${type}"
            processConfig(marshallerCfg, converterClass, type)            
        }
    }
    
    public void processConfig(cfg, Class converterClass, type) {
        def converterCfg = ConvertersConfigurationHolder.getConverterConfiguration(converterClass)
        def builder = new ConfigurationBuilder(type: type, applicationContext: applicationContext, cfg: converterCfg, log: LOG, converterClass: converterClass, cfgName: "default")
        builder.registerSpringMarshallers()
        if (cfg != null && cfg instanceof Closure) {
            cfg.delegate = builder
            cfg.resolveStrategy = Closure.DELEGATE_FIRST;
            cfg.call()
        }
    }
}

