package org.grails.plugins.marshallers.config

class MarshallingConfig {

	def contentTypes=[];

	def getConfig(type,name){
		return contentTypes.find{it.type==type}?.namedConfigs.find{it.name==name}?.config;
	}

	def getConfigNamesForContentType(type){
		return contentTypes.find {it.type==type}?.namedConfigs.collect {it.name}
	}
}
