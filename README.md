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
	
	