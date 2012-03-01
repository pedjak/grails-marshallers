package org.grails.plugins.marshallers.config

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

class MarshallingConfig {

	def config=[:];

	def getConfig(type,name){
		return config[type]!=null?config[type][name]!=null?config[type][name]:[:]:[:];
	}

	def getConfigNamesForContentType(type){
		return config[type]!=null?config[type].collect {key,value->key}:[]
	}



	static def getForClass(Class domainClass){
		def mc=GCU.getStaticPropertyValue(domainClass,'marshalling');
		MarshallingConfigBuilder delegate=new MarshallingConfigBuilder();
		mc.setDelegate(delegate)
		mc.call()
		MarshallingConfig c=new MarshallingConfig(config:delegate.config);
		return c;
	}
}
