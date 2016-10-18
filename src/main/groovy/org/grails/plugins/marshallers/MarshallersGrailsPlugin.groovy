package org.grails.plugins.marshallers
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

import grails.plugins.Plugin
import grails.util.GrailsClassUtils as GCU;
import grails.core.support.proxy.DefaultProxyHandler;
import grails.core.support.proxy.ProxyHandler;
import org.grails.plugins.marshallers.ExtendedConvertersConfigurationInitializer
import org.grails.plugins.marshallers.GenericDomainClassJSONMarshaller;
import org.grails.plugins.marshallers.GenericDomainClassXMLMarshaller;
import org.grails.plugins.marshallers.XmlMarshallerArtefactHandler
import org.grails.plugins.marshallers.JsonMarshallerArtefactHandler
import org.grails.plugins.marshallers.config.MarshallingConfig;
import org.grails.plugins.marshallers.config.MarshallingConfigBuilder;
import grails.converters.XML;
import grails.converters.JSON;


class MarshallersGrailsPlugin extends Plugin {
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.2.0 > *"
    // resources that are excluded from plugin packaging

    def dependsOn = [converters: grailsVersion]

    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def scm = [url: "https://github.com/pedjak/grails-marshallers"]
    def licence = "APACHE"

    def artefacts = [
            XmlMarshallerArtefactHandler,
            JsonMarshallerArtefactHandler
    ]

    def watchedResources = [
            'file:./grails-app/domain/*.groovy',
            'file:./grails-app/conf/Config.groovy'
    ]

    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"

    def developers = [[name: "Denis Halupa", email: "denis.halupa@gmail.com"], [name: "Angel Ruiz", email: "aruizca@gmail.com"], [name: "Mansi Arora", email: "mansi.arora@tothenew.com"]]

    def title = "Easy Custom XML and JSON Marshalling for Grails Converters"
    def description = '''\\
Easy registration and usage of custom XML and JSON marshallers supporting hierarchical configurations.

Further documentation can be found on the GitHub repo.
'''

    // URL to the plugin's documentation
    def documentation = "https://github.com/pedjak/grails-marshallers"

    def doWithSpring = {

        extendedConvertersConfigurationInitializer(ExtendedConvertersConfigurationInitializer)
        ["xml", "json"].each { type ->
            application."${type}MarshallerClasses".each { marshallerClass ->
                "${marshallerClass.fullName}"(marshallerClass.clazz) { bean ->
                    bean.autowire = "byName"
                }
            }
        }
    }

    def doWithDynamicMethods = { applicationContext ->
        applicationContext.extendedConvertersConfigurationInitializer.initialize()
        log.debug "Marshallers Plugin configured successfully"
    }

    def onChange = { event ->
        event.ctx.extendedConvertersConfigurationInitializer.initialize()
    }
}
