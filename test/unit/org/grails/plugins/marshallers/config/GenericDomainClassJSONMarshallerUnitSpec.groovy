package org.grails.plugins.marshallers.config
import grails.converters.JSON
import grails.persistence.Entity
import grails.test.mixin.*

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer;
import org.grails.plugins.marshallers.ExtendedConvertersConfigurationInitializer;
import org.grails.plugins.marshallers.JsonMarshallerArtefactHandler
import org.grails.plugins.marshallers.XmlMarshallerArtefactHandler
import org.grails.plugins.marshallers.test.MarshallerUnitSpecMixin

import spock.lang.Shared;
import spock.lang.Specification

/**
 * 
 * @author dhalupa
 *
 */
@Mock([Invoice, Item])
class GenericDomainClassJSONMarshallerUnitSpec extends Specification {

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

	def "setting shouldOutputIdentifier to false should supress output of identifier"(){
		given:
		Invoice.marshalling = { shouldOutputIdentifier false }
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.admin!=null
		m.id==null
	}

	def "putting a property to a list of ignores should supress property serialization"(){
		given:
		Invoice.marshalling = { ignore 'admin','created' }
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.admin==null
		m.created==null
		m.id!=null
	}

	def "setting shouldOutputClass and shouldOutputVersion to true should output class and version info"(){
		given:
		Invoice.marshalling = {
			shouldOutputClass true
			shouldOutputVersion true
		}
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.class!=null
		m.version!=null
	}

	def "specifying virtual property should output value"(){
		given:
		Invoice.marshalling = {
			virtual{
				custom {value,json-> json.value('custom value')		}
			}
		}
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.custom=='custom value'
	}

	def "specifying serializer should output customly serialized property value"(){
		given:
		Invoice.marshalling = {
			serializer{
				name {value,json-> json.value('My custom name')		}
			}
		}
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.name=='My custom name'
	}

	def "specifying association in the deep list should serialize complete instances"(){
		given:
		Invoice.marshalling = { deep 'items' }
		initialize()
		when:
		def	j=new JSON(invoice)
		def m=JSON.parse(j.toString())
		then:
		m.items.size()==2
		m.items[0].name
		m.items[1].name
		m.items[0].amount
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
		JSON.use('named'){
			j=new JSON(invoice)
		}
		def m=JSON.parse(j.toString())
		then:
		m.items.size()==2
		m.items[0].name
		m.items[1].name
		m.items[0].amount
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
		JSON.use('named'){
			j=new JSON(invoice)
		}
		def m=JSON.parse(j.toString())
		then:
		m.items.size()==2
		!m.items[0].name
		!m.items[1].name
		m.items[0].amount
	}
	
	private def initialize(){
		grailsApplication.mainContext.convertersConfigurationInitializer.initialize(grailsApplication)
		grailsApplication.mainContext.extendedConvertersConfigurationInitializer.initialize()
	}
}
@Entity
class Invoice {
	private static _m={}
	String name
	boolean admin
	Date created


	static hasMany = [items: Item]

	static def getMarshalling(){
		return _m
	}

	static def setMarshalling(value){
		_m=value
	}
}
@Entity
class Item {
	static _m={}


	float amount
	String name

	static def getMarshalling(){
		return _m
	}

	static def setMarshalling(value){
		_m=value
	}
}
