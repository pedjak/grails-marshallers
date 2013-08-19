package org.grails.plugins.marshallers.config
import grails.test.mixin.*

import org.grails.plugins.marshallers.test.MarshallerUnitSpecMixin

import spock.lang.Specification
@TestMixin(MarshallerUnitSpecMixin)
class MarshallingConfigSpec extends Specification {
	
	static def testConfig={
		xml{
			'default' {
				identifier 'some','id'
				elementName 'elName'
				attributes 'some', 'some1'
				ignore 'ig', 'big'
			}
			named {
				identifier {val,xml->println val}
				attributes 'some', 'some1'
				ignore 'ig', 'big'
				serializer {
					taxonomies {val,xml-> println val }
				}
				virtual {
					rootNodes { form,xml ->println form	}
				}
			}
		}
		json {
			'default'{ ignore 'some', 'some1' }
		}
	}


	def "test app"(){
		when:true
		then:
		grailsApplication!=null
	}


	def "can build marshalling config"(){
		given:
		def cl={
			identifier 'uuid','id'
			shouldOutputClass true
			json {
				shouldOutputClass false
				export{
					identifier 'id'
					virtual {
						some {
						}
						other {
						}
					}
					restrictedExport { 
						identifier 'uuid'

					 }
				}
			}
			xml{ elementName 'test' }
			some {}
		}
		MarshallingConfigBuilder bldr=new MarshallingConfigBuilder()
		cl.delegate=bldr
		cl.resolveStrategy = Closure.DELEGATE_FIRST
		when:
		cl()
		def config=bldr.config
		then:
		config.shouldOutputClass
		config.identifier==['uuid', 'id']
		config.children.size()==3
		config.children[0].type =='json'
		config.children[0].children[0].identifier==['id']
		config.children[0].children[0].type=='json'
		config.children[0].children[0].name=='export'
		config.children[1].elementName=='test'
		config.findConfigNames('json')==['default',	'export','restrictedExport','some'] as Set
		config.findConfigNames('xml')==['default', 'some'] as Set
		config.findNamedConfig('json','export')!=null
		def dc=config.findNamedConfig('json','default')
		dc!=null
		dc.identifier==['uuid', 'id']
		!dc.shouldOutputClass
		config.findNamedConfig('json','export').identifier==['id']
		config.findNamedConfig('json','export').virtual.size()==2
		config.findNamedConfig('json','restrictedExport').identifier==['uuid']
	}

	def "can build marshalling config without type specified"(){
		given:
		def cl={
			export{
				identifier 'id'
				restrictedExport { identifier 'uuid' }
			}
		}
		MarshallingConfigBuilder bldr=new MarshallingConfigBuilder()
		cl.delegate=bldr
		cl.resolveStrategy = Closure.DELEGATE_FIRST
		when:
		cl()
		def config=bldr.config
		then:
		config.findNamedConfig('json','export').identifier==['id']
		config.findNamedConfig('json','restrictedExport').identifier==['uuid']
	}

	def "can build marshalling config with default configuration"(){
		given:
		def cl={ identifier 'id' }
		MarshallingConfigBuilder bldr=new MarshallingConfigBuilder()
		cl.delegate=bldr
		cl.resolveStrategy = Closure.DELEGATE_FIRST
		when:
		cl()
		def config=bldr.config
		then:
		config.findNamedConfig('json','default').identifier==['id']
		
	}
	
	def "can build marshalling config for empty config"(){
		given:
		def cl={ json{smart{}} }
		MarshallingConfigBuilder bldr=new MarshallingConfigBuilder()
		cl.delegate=bldr
		cl.resolveStrategy = Closure.DELEGATE_FIRST
		when:
		cl()
		def config=bldr.config
		then:
		config.findNamedConfig('json','smart')!=null
		
	}
}


