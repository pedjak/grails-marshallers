package org.grails.plugins.marshallers.config

import org.grails.plugins.marshallers.ActionConfigBuilder



class MarshallingConfigBuilder {

	def static rules=[
		identifier:[
			members:['identifier'],
			rule:{args->args}],
		singleAttr:[
			members:['elementName'],
			rule:{args-> args[0] }
		],
		actions:[
			members:['serializer', 'virtual'],
			rule:{args->
				ActionConfigBuilder builder=new ActionConfigBuilder();
				def closure=args[0]
				closure.delegate = builder
				closure.resolveStrategy = Closure.DELEGATE_FIRST;
				closure.call();
				builder.config;
			}
		]
	]

	def config=[:];

	def methodMissing(String name,args){
		def entry=rules.find{key,value->value.members.contains(name)};
		if(entry!=null){
			config[name]=entry.value.rule.call(args as List)
		}else if(args.size()==1 && args[0] instanceof Closure){
			MarshallingConfigBuilder builder=new MarshallingConfigBuilder();
			def closure = args[0]
			closure.delegate = builder
			closure.resolveStrategy = Closure.DELEGATE_FIRST;
			closure.call();
			config[name]=builder.config;
		}else{
			config[name]=args
		}
	}
}
