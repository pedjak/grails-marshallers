package org.grails.plugins.marshallers.config

import java.util.Map;


class MarshallingConfigBuilder {
	def static singleValueAttrs=['elementName']
	def config=[:];
	def methodMissing(String name,args){
		if(args.size()==1 && args[0] instanceof Closure){
			MarshallingConfigBuilder builder=new MarshallingConfigBuilder();
			def closure = args[0]
			closure.delegate = builder
			closure.resolveStrategy = Closure.DELEGATE_FIRST;
			closure.call();
			config[name]=builder.config;
		}else{
			boolean singleValue=singleValueAttrs.find {it==name}!=null;
			if(singleValue){
				if(args.size()==1){
					config[name]=args[0];
				}else{
					throw new IllegalStateException('Illegal syntax for '+ name)
				}
			}else{
				config[name]=args;
			}
		}
	}
}
