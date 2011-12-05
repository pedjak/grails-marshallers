
package org.grails.plugins.marshallers

import grails.converters.JSON;
import grails.converters.XML;
import grails.util.GrailsConfig;

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
            def marshallerCfg = GrailsConfig.get("grails.plugins.marshallers.${type}")
            if (marshallerCfg != null) {
                processConfig(marshallerCfg, converterClass, type)
            }
        }
    }
    
    public void processConfig(Closure cfg, Class converterClass, type) {
        def converterCfg = ConvertersConfigurationHolder.getConverterConfiguration(converterClass)
        def builder = new ConfigurationBuilder(type: type, applicationContext: applicationContext, cfg: converterCfg, log: LOG, converterClass: converterClass, cfgName: "default")
        builder.registerSpringMarshallers()
        cfg.delegate = builder
        cfg.resolveStrategy = Closure.DELEGATE_FIRST;
        cfg.call()
    }
}

