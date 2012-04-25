Custom Grails XML and JSON Marshallers
======================================

The development of this plugin has been inspired by [this blog post](http://jwicz.wordpress.com/2011/07/11/grails-custom-xml-marshaller/)
and a need to use custom marshallers - mostly by REST services whose responses must fulfill already defined formats.

The Grails Converter plugin shipping within the Grails installation provides already
all pieces needed to register and use a custom XML or JSON marshaller, but the way how 
to achieve it is (1) rather undocumented and (2) requires explicit registrations 
somewhere in application code (usually in BootStrap file).

This plugin adds to application XmlMarshaller and JsonMarshaller artefact types allowing
transparent registration of custom XML and/or JSON marshallers defined by the appropriate
artefacts.
 
Creating and Registering
------------------------

Let's suppose that we have a domain class:

    class A {
      String foo
      String bar
    }
    
and we need to serialize the fields as XML attributes.

here is the marshaller that does this for us:

	import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
	class AXmlMarshaller implements ObjectMarshaller<XML> {
		boolean supports(Object object) {
			A.isAssignableFrom(object.class) 
		}

		void marshalObject(obj, xml) {
			xml.attribute("foo", obj.foo)
			xml.attribute("bar", obj.bar)  
		}
	} 

and it will be registered at the application startup.
Note that this is a standard Grails artifact - thus dependency injection works as expected.

Named Configurations
--------------------

A nice feature of the actual converter architecture is support for named configurations 
that can be hierarchical as well. Imagine that you need to serialize in some cases instances of
A with "foo" content only. Let us register the appropriate marshaller 
in the configuration entitled 'restricted':

	import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
	class AXmlMarshaller implements ObjectMarshaller<XML> {
	
		static configuration = "restricted"
		
		boolean supports(Object object) {
			A.isAssignableFrom(object.class) 
		}

		void marshalObject(obj, xml) {
			xml.attribute("foo", obj.foo)
		}
	} 

The static property *configuration* can contain one particular converter configuration 
name or a list of names where the marshaller should be registered to. Further, we need
specify the configuration hierarchy in application Config:

	grails.plugins.marshallers.xml = {
		restricted {
			evenMoreRestricted {
			}
		}
	} 

We always have one default system configuration that is the parent of the named configurations on the 
first level.

Now, when we
need to serialize instances of A restrictively, we would say:

	import import grails.converters.XML
	
	def get_restricted = {
		def a = A.get(params.id)
		XML.use('restricted') {
			render a as XML
		}
	}
	
Register Marshallers Within Application Config
----------------------------------------------

If your marshallers are simple and/or you do not want to have them as artifacts in
the system, you can register them in-place while defining named configurations:

	grails.plugins.marshallers.xml = {
		register A { obj, xml ->
			xml.attribute("foo", obj.foo)
			xml.attribute("bar", obj.bar)
		}
		restricted {
			register A { obj, xml ->
				xml.attribute("foo", obj.foo)				
			}
			evenMoreRestricted {
			}
		}
	} 

Two more registration statements are available:
* register(Class class, Closure elementNameClosure, Closure contentClosure) - needed in a case
when XML element name should be custom, e.g.

 	register A { obj -> "custom" } { obj, xml ->
		xml.attribute("foo", obj.foo)
		xml.attribute("bar", obj.bar)
	}
	
* register(Class class) - when registering marshaller whose logic is in *class* and the class naming 
does not follow marshaller artifact convention, e.g.:

	register CustomAXMLSerializer

Generic Domain Class Marshallers
--------------------------------

Let's assume that you have the following domain classes which has to be serialized
	
	class Author {
     	String name
      	Date dob
      	List books
    }
	
	class Book {
     	String isbn
      	String name
    }
    
and we have the following requirements

* *dob* and *isbn* fields has to be serialized as attributes
* *books* belonging to an *author* has to be serialized as children of *author* xml element
* there is a static method of Author class which fetches popular books for an author. These popular books should also be serialized as children of *author* xml element 

Marshaling configuration for such a case is specified as a static closure of each class
  	
  	class Author {
    	static marshalling={
			xml { 
				export {
					ignoreIdentifier true
					attribute 'dob'
					deep 'books'
					virtual {
						popularBooks {author,xml->
							author.findPopularBooks().each{ popularBook->
								xml.startNode(xml.getElementName(popularBook))
								xml.lookupObjectMarshaller(popularBook).marshalObject(popularBook,xml)
								xml.end()
							}
						}
					}
				}
			}
		}
     
      	String name
      	Date dob
      	List books
    }


	class Book {
		static marshalling={
			xml {
				export {
					elementName 'my-book'
					attribute 'isbn'
				}
			}
		}
		
     	String isbn
      	String name
    }
    

In the above specified configuration 



* *xml* is the serialization format. Currently, only *xml* is supported, but it is planned to support *json* format also
* *export* is the identifier for named converter configuration. Could be any name or *default* which identifies default converter configuration. 

Within the named configuration closure there are several configuration options possible

* *ignoreIdentifier* when true will suppress serialization of domain object identifier
* *identifier* a comma separated list of fields which uniquely identifies a domain object in case database id is not sufficient. 
* *elementName* configures a custom domain object element name which should be used instead of default one
* *attribute* a comma separated list of field names which will be serialized as attributes of domain object element 
* *deep* a comma separated list of field names. If a field representing one-to-many relation is marked as *deep*, all contained data of related objects will be serialized
* *virtual* configuration option allows us to define closures with custom serialization behavior

When configuration is defined as above the following snippet of code would perform actual serialization
	
	Author author=Author.findByName('Jonathan Franzen')
	XML.use('export')
	XML converter=new XML(author)
	String xml=converter.toString()
	
Producing the following hypothetical XML output
	
	<?xml version="1.0" encoding="UTF-8"?>
	<author dob="Fri Aug 17 00:00:00 CET 1959">
		<name>Jonathan Franzen</name>
		<books>
			<my-book isbn="1111">
				<name>The Twenty-Seventh City</name>
			<my-book>
			<my-book isbn="2222">
				<name>Freedom</name>
			<my-book>
		<%books>
		<popularBooks>
			<my-book isbn="2222">
				<name>Freedom</name>
			<my-book>
		</popularBooks>
	</author>