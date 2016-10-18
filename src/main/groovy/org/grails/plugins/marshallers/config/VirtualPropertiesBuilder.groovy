package org.grails.plugins.marshallers.config

class VirtualPropertiesBuilder {
	Map properties=[:]
	
	void methodMissing(String name,args){
		properties[name]=args[0]
	}

}
