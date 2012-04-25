package org.grails.plugins.marshallers

public class ActionConfigBuilder{
	def config=[:]
	def methodMissing(String name,args){
		if(args.size()==1 && args[0] instanceof Closure){
			config[name]=args[0]
		}else{
			throw new IllegalStateException('Illegal syntax encountered while building serializers config: '+ name)
		}
	}
}
