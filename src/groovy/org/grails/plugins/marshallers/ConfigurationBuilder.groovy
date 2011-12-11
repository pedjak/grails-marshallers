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


import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder;
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration;
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller;
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller;

import groovy.lang.Closure;

import org.codehaus.groovy.grails.web.converters.Converter;

import org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller;

import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

/**
 * @author Predrag Knezevic
 * @version $Date: $
 */
class ConfigurationBuilder {
    private static String CONFIGURATION_FIELD = "configuration"
    
    def log
    def applicationContext
    def cfg
    def converterClass
    def cfgName
    def type
    
    def ensureCfg() {
        if (! (cfg instanceof DefaultConverterConfiguration)) {
            cfg = new DefaultConverterConfiguration(cfg)
            ConvertersConfigurationHolder.setDefaultConfiguration(converterClass, cfg)
        }
    }
    def registerSpringMarshallers() {
        // find all marshallers with the given configuration
        applicationContext.grailsApplication."${type}MarshallerClasses"*.clazz.findAll { clazz ->
            def cfg = GCU.getStaticPropertyValue(clazz, CONFIGURATION_FIELD)
            def found = cfg == null && cfgName == "default" 
            if (!found) {
                found = cfg instanceof Collection ? cfg.contains(cfgName) : (cfg == cfgName)
            }
            found
        }.each { register(it) }
    }
    
    def register(Object marshaller) {
        ensureCfg()
        if (!cfg.orderedObjectMarshallers.contains(marshaller)) {
            cfg.registerObjectMarshaller(marshaller)
            log.debug "Registered marshaller ${marshaller.class.name}"
        }
    }
    def register(Class clazz) {
        def marshaller = applicationContext.containsBean(clazz.name) ? applicationContext.getBean(clazz.name) : clazz.newInstance()
        register(marshaller)
    }
    
    def register(String marshallerName) {
        def marshaller = applicationContext.containsBean(marshallerName) ? applicationContext.getBean(marshallerName) : null
        if (marshaller == null) {
            log.warn("Cannot find and register marshaller with the name ${marshallerName}")
        } else {
            register(marshaller)
        }
    }
    
    def register(Class clazz, Closure cl) {
        ensureCfg()
        cfg.registerObjectMarshaller(clazz, cl)
        log.debug "Registered closure marshaller for ${clazz.name}" 
    }
    
    def register(Class clazz, String elementName, Closure cl) {
        ensureCfg()
        cfg.registerObjectMarshaller(new NameAwareClosureObjectMarshaller(clazz, elementName, cl))
        log.debug "Registered closure marshaller for ${clazz.name}"
    }
    
    def invokeMethod(String name, args) {
        if (args.size() != 1) {
            log.error "the number of params can be only one for ${name}"
        } else if (! args[0] instanceof Closure) {
            log.error "${name} accepts only closure as param"
        } else {
            log.info "create config with name ${name} under ${cfgName}"
            def newCfg = ConvertersConfigurationHolder.getNamedConverterConfiguration(name, converterClass)
            if (newCfg == null) {
                newCfg = new DefaultConverterConfiguration(cfg)
                ConvertersConfigurationHolder.setNamedConverterConfiguration(converterClass, name, newCfg)                
            }
            def builder = new ConfigurationBuilder(type: type, applicationContext: applicationContext, cfg: newCfg, converterClass: converterClass, log: log, cfgName: name)
            builder.registerSpringMarshallers()
            def closure = args[0]
            closure.delegate = builder
            closure.resolveStrategy = Closure.DELEGATE_FIRST;
            closure.call()
        }
    }
}

class NameAwareClosureObjectMarshaller<T extends Converter> extends ClosureOjectMarshaller<T> implements NameAwareMarshaller {
    
    def elementName
    
    def NameAwareClosureObjectMarshaller(Class clazz, String elementName, Closure closure) {
        super(clazz, closure)
        this.elementName = elementName        
    }
    
    String getElementName(Object o) {
        elementName
    }
    
}
