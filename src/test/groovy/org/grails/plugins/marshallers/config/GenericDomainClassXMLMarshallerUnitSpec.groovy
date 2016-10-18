package org.grails.plugins.marshallers.config
import grails.converters.JSON
import grails.converters.XML
import grails.persistence.Entity
import grails.test.mixin.*

import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.grails.web.converters.exceptions.ConverterException
import org.grails.plugins.marshallers.ExtendedConvertersConfigurationInitializer
import org.grails.plugins.marshallers.GenericDomainClassJSONMarshaller
import org.grails.plugins.marshallers.GenericDomainClassXMLMarshaller
import org.grails.plugins.marshallers.JsonMarshallerArtefactHandler
import org.grails.web.converters.marshaller.NameAwareMarshaller
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.plugins.marshallers.XmlMarshallerArtefactHandler
import org.grails.plugins.marshallers.test.MarshallerUnitSpecMixin

import spock.lang.Shared
import spock.lang.Specification
/**
 * 
 * @author dhalupa
 *
 */

@Mock([Invoice, Item])
class GenericDomainClassXMLMarshallerUnitSpec extends Specification {

	def invoice, item1, item2

	def setup(){
		Invoice.marshalling = { }
		Item.marshalling = {  }
		grailsApplication.registerArtefactHandler(new JsonMarshallerArtefactHandler())
		grailsApplication.registerArtefactHandler(new XmlMarshallerArtefactHandler())
		defineBeans {
			convertersConfigurationInitializer(ConvertersConfigurationInitializer)
			extendedConvertersConfigurationInitializer(ExtendedConvertersConfigurationInitializer)
		}
		invoice=new Invoice(name:'Some name',admin:true,created:new Date()).save()
		item1=new Item(name:'Item1',amount:2.5)
		item2=new Item(name:'Item2',amount:22.5)
		invoice.addToItems(item1)
		invoice.addToItems(item2)
		invoice.save()
	}

	def "setting shouldOutputIdentifier to false should suppress output of identifier"(){
		given:
		Invoice.marshalling = { shouldOutputIdentifier false }
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.admin.text()
		!m.id.text()
	}

	def "putting a property to a list of ignores should suppress property serialization"(){
		given:
		Invoice.marshalling = { ignore 'admin','created' }
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		!m.admin.text()
		!m.created.text()
		m.@id.text()
	}

    def "putting a property to a list of includes should suppress other properties serialization"(){
        given:
        Invoice.marshalling = { include 'name' }
        initialize()
        when:
        def	j=new XML(invoice)
        def m=new XmlSlurper().parseText(j.toString())
        then:
        m.name.text()
        !m.admin.text()
        !m.created.text()
        m.@id.text()
    }
	
	def "putting a property to a list of attributes output value as attribute"(){
		given:
		Invoice.marshalling = { attribute 'admin','created' }
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.@admin.text()
		m.@created.text()
		!m.admin.text()
		!m.created.text()
		m.@id.text()
	}
	
	def "specifying element name should output accordingly"(){
		given:
		Invoice.marshalling = { elementName 'inv' }
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.name()=='inv'
		m.admin.text()
		m.@id.text()
	}

	def "setting shouldOutputClass and shouldOutputVersion to true should output class and version info"(){
		given:
		Invoice.marshalling = {
			shouldOutputClass true
			shouldOutputVersion true
		}
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.@class.text()
		m.@version.text()
	}

	def "specifying vritual property should output value"(){
		given:
		Invoice.marshalling = {
			virtual{
				custom {value,xml-> xml.convertAnother('custom value')		}
			}
		}
		initialize()
		when:
		def	j=new XML(invoice)
		println j.toString()
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.custom.text()=='custom value'
	}

	def "specifying virtual property with context passed should output value"(){
		given:
		GenericDomainClassXMLMarshaller.marshallingContext.myCtx='context value'
		Invoice.marshalling = {
			virtual{
				custom {value,xml,ctx-> xml.convertAnother('context value')		}
			}
		}
		initialize()
		when:
		def	j=new XML(invoice)
		println j.toString()
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.custom.text()=='context value'
	}

	def "specifying serializer should output customly serialized property value"(){
		given:
		Invoice.marshalling = {
			serializer{
				name {value,xml-> xml.convertAnother('custom name')	}
			}
		}
		initialize()
		when:
		def	j=new XML(invoice)
		println j.toString()
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.name.text()=='custom name'
	}

	def "specifying association in the deep list should serialize complete instances"(){
		given:
		Invoice.marshalling = { deep 'items' }
		initialize()
		when:
		def	j=new XML(invoice)
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.items.item.size()==2
		m.items.item[0].name.text()
		m.items.item[1].name.text()
		m.items.item[0].amount.text()
	}
	
	
	def "named configuration should use default config on association unless specified"(){
		given:
		Invoice.marshalling = {
			named{ 
				deep 'items'
			} 
		}
		initialize()
		when:
		def	j
		XML.use('named'){
			j=new XML(invoice)
		}
		def m=new XmlSlurper().parseText(j.toString())
        println j.toString()
		then:
		m.items.item.size()==2
        m.items.item[0].children().size() == 2
		m.items.item[0].name.text()
		m.items.item[1].name.text()
		m.items.item[0].amount.text()
	}
	
	def "named configuration should use named config on association if specified"(){
		given:
		Invoice.marshalling = {
			named{
				deep 'items'
			}
		}
		Item.marshalling = {
			named{
				ignore 'name'
			}
		}
		initialize()
		when:
		def	j
		XML.use('named'){
			j=new XML(invoice)
		}
		def m=new XmlSlurper().parseText(j.toString())
		then:
		m.items.item.size()==2
		!m.items.item[0].name.text()
		!m.items.item[1].name.text()
		m.items.item[0].amount
		m.items.item[1].amount
	}
	
    def "nested configuration"() {
        given:
        grailsApplication.config.grails.plugins.marshallers.xml = {
            c1 {
                c2 {
                    
                }
            }
        }
        Invoice.marshalling = {
            xml {
                c1 {
                    deep 'items'
                    ignore 'admin'
                    attribute 'name'
                }
            }
        }
        Item.marshalling = {
            xml {
                c2 {
                    ignore 'amount'
                }
                
            }
            
        }
        initialize()
        
        when:
        def rootInvoice
        
        def rootItem
        XML.use('c2') {
            // invoice will be serialized according to rules in c1 conf, because it does not have c2 conf
            rootInvoice= new XmlSlurper().parseText(new XML(invoice).toString())
            
            // serialized according to c2 conf
            rootItem = new XmlSlurper().parseText(new XML(invoice.items.first()).toString())
        }
        
        then:
        rootInvoice.items.item.amount.size() == 0
        rootInvoice.items.item.name.size() == invoice.items.size()
        rootInvoice.admin.size() == 0
        rootInvoice.name.size() == 0
        rootInvoice.'@name'.text() == invoice.name
        
        rootItem.amount.size() == 0
        rootItem.name.size() > 0
        
    }
    
    def "use parent configuration for association and deep = false"() {
        given:
        grailsApplication.config.grails.plugins.marshallers.xml = {
            c1 {
                c2 {
                    
                }
            }
        }
        Invoice.marshalling = {
            xml {
                c2 {
                    attribute 'name'
                }
            }
        }
        Item.marshalling = {
            xml {
                c1 {
                    identifier 'name'
                }
                
            }
            
        }
        initialize()
        
        when:
        def rootInvoice
        
        XML.use('c2') {
            // invoice will be serialized according to rules in c1 conf, because it does not have c2 conf
            rootInvoice= new XmlSlurper().parseText(new XML(invoice).toString())            
        }
        
        then:
        rootInvoice.'@name'.text() == invoice.name
        rootInvoice.items.item*.'@name'*.text() as Set == invoice.items*.name as Set            
    }
    
    def "marshaller registered in config takes precedence over marshalling configuration in domain class"() {
        given:
        grailsApplication.config.grails.plugins.marshallers.xml = {
            c1 {
                register(ItemMarshaller)
            }
        }
        Item.marshalling = {
            xml {
                c1 {
                    identifier 'name'
                }
                
            }
            
        }
        initialize()
        
        when:
        def rootItem
        XML.use('c1') {
            // invoice will be serialized according to rules in c1 conf, because it does not have c2 conf
            rootItem= new XmlSlurper().parseText(new XML(invoice.items.first()).toString())
        }
        
        then:
        rootItem.name() == 'xxx'
    }
    
	private def initialize(){
		grailsApplication.mainContext.convertersConfigurationInitializer.initialize(grailsApplication)
		grailsApplication.mainContext.extendedConvertersConfigurationInitializer.initialize()
	}
}

class ItemMarshaller implements ObjectMarshaller<XML>, NameAwareMarshaller {
    
    @Override
    public boolean supports(Object object) {
        return object.class == Item
    }

    @Override
    public void marshalObject(Object value, XML xml)    throws ConverterException {
        xml.attribute('name', value.name)
    }
    
    @Override
    public String getElementName(Object value) {
        return 'xxx'
    }
}