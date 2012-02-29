package org.grails.plugins.marshallers.config

import java.util.Map;


class MarshallingConfigBuilder extends BuilderSupport{
	MarshallingConfig config=new MarshallingConfig();

	@Override
	protected void setParent(Object parent, Object child) {
		parent.namedConfigs<<child
	}

	@Override
	protected Object createNode(Object name) {
		def node= [type:name,namedConfigs:[]];
		config.contentTypes<<node;
		return node;
	}

	@Override
	protected Object createNode(Object name, Object value) {
		throw new IllegalStateException('Illegal config specified')
	}

	@Override
	protected Object createNode(Object name, Map attributes) {
		return [name:name,config:attributes]
	}

	@Override
	protected Object createNode(Object name, Map attributes, Object value) {
		throw new IllegalStateException('Illegal config specified')
	}

	public MarshallingConfig getConfig(){
		return config;
	}
}
