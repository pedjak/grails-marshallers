Custom Grails XML and JSON Marshallers
======================================
[![Build Status](https://travis-ci.org/pedjak/grails-marshallers.png?branch=master)](https://travis-ci.org/pedjak/grails-marshallers)

The development of this plugin has been inspired by [this blog post](http://jwicz.wordpress.com/2011/07/11/grails-custom-xml-marshaller/)
and a need to use custom marshallers - mostly by REST services whose responses must fulfill already defined formats.

The plugin is trying to address the following two issues
Change log
----------
### 0.7
 * Added a new include option (allow only specific properties - issue #12)
 * Added marshalling context as an additional argument to virtual property closure. See the test for the example of usage
 * Minor fixes and documentation updates
 


[Creating and Registering Custom Marshallers](#creating-and-registering-custom-marshallers)

Grails Converter plugin shipping within the Grails installation provides already
all pieces needed to register and use a custom XML or JSON marshaller, but the way how 
to achieve it is (1) rather undocumented and (2) requires explicit registrations 
somewhere in application code (usually in BootStrap file). 
This plugin adds to application XmlMarshaller and JsonMarshaller artefact types allowing
transparent registration of custom XML and/or JSON marshallers defined by the appropriate
artefacts.

[Configuring Domain Class Marshalling](#configuring-domain-class-marshalling)

 As far as marshalling of domain instances is concerned, converters plugin provides and 
registers two built in marshaller implementation per format. These are DomainClassJSON(XML)Marshaller 
and DeepDomainClassJSON(XML)Marshaller. The problem is the fact that there is no mechanism to somehow customize 
output of these marshallers. This plugin adds a _marshaller_ configuration option to each domain class allowing to customze output
produces by DomainClass marshaller.

Feel free to check unit tests for detailed examples of usage

Installation
------------
* For apps using Grails version <= 2.2.x -> compile ":marshallers:0.4"
* For apps using Grails version >= 2.3.x -> compile ":marshallers:0.6"

 
Creating and Registering Custom Marshallers
-------------------------------------------

Let's suppose that we have a domain class:
```groovy
    class A {
      String foo
      String bar
    }
```
and we need to serialize the fields as XML attributes.

here is the marshaller that does this for us:
```groovy
	import org.grails.web.converters.marshaller.ObjectMarshaller
	class AXmlMarshaller implements ObjectMarshaller<XML> {
		boolean supports(Object object) {
			A.isAssignableFrom(object.class) 
		}

		void marshalObject(obj, xml) {
			xml.attribute("foo", obj.foo)
			xml.attribute("bar", obj.bar)  
		}
	} 
```
and it will be registered at the application startup.
Note that this is a standard Grails artifact - thus dependency injection works as expected.

### Named Configurations


A nice feature of the actual converter architecture is support for named configurations 
that can be hierarchical as well. Imagine that you need to serialize in some cases instances of
A with "foo" content only. Let us register the appropriate marshaller 
in the configuration entitled 'restricted':
```groovy
	import org.grails.web.converters.marshaller.ObjectMarshaller
	class AXmlMarshaller implements ObjectMarshaller<XML> {
	
		static configuration = "restricted"
		
		boolean supports(Object object) {
			A.isAssignableFrom(object.class) 
		}

		void marshalObject(obj, xml) {
			xml.attribute("foo", obj.foo)
		}
	} 
```
The static property *configuration* can contain one particular converter configuration 
name or a list of names where the marshaller should be registered to. Further, we need
specify the configuration hierarchy in application Config:
```groovy
	grails.plugins.marshallers.xml = {
		restricted {
			evenMoreRestricted {
			}
		}
	} 
```
We always have one default system configuration that is the parent of the named configurations on the 
first level. If a given configuration does not define a marshaller for a given class, it will be searched
in its parent configuration until the system configuration is reached.

Now, when we
need to serialize instances of A restrictively, we would say:
```groovy
	import import grails.converters.XML
	
	def get_restricted = {
		def a = A.get(params.id)
		XML.use('restricted') {
			render a as XML
		}
	}
```
### Register Marshallers Within Application Config

If your marshallers are simple and/or you do not want to have them as artifacts in
the system, you can register them in-place while defining named configurations:
```groovy
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
```
Two more registration statements are available:
* register(Class class, Closure elementNameClosure, Closure contentClosure) - needed in a case
when XML element name should be custom, e.g.
```groovy
 	register A { obj -> "custom" } { obj, xml ->
		xml.attribute("foo", obj.foo)
		xml.attribute("bar", obj.bar)
	}
```	
* register(Class class) - when registering marshaller whose logic is in *class* and the class naming 
does not follow marshaller artifact convention, e.g.:

	register CustomAXMLSerializer

Configuring Domain Class Marshalling
------------------------------------

Along to developing and registering a custom marshaller, the way how a
domain class instance is marshalled can be specified within the domain
class itself - specifying the marshalling configuration(s). Maybe the best way to ilustrate the machanism is with a small
example

Let's assume that you have the following domain classes which has to be serialized
```groovy	
	class Author {
     	String name
      	Date dob
      	
      	static hasMany = [books: Book]
    }
	
	class Book {
     	String isbn
      	String name
    }
```
and we have the following requirements

* *dob* and *isbn* fields has to be serialized as attributes
* *books* belonging to an *author* has to be serialized as children of *author* xml element
* *identifier* , *class* and *version* information should be suppressed

Marshaling configuration for such a case is specified as a static closure of each class
```  groovy	
class Author {
    	static marshalling={
		shouldOutputIdentifier false
		shouldOutputVersion false
		shouldOutputClass false
		attribute 'dob'
		deep 'books'
				
	}
     
      	String name
      	Date dob
      	List books
}


class Book {
	static marshalling={
		shouldOutputIdentifier false
		shouldOutputVersion false
		shouldOutputClass false
		elementName 'my-book'
		attribute 'isbn'
	}

     	String isbn
	String name
}
    
```
When configuration is defined as above the following snippet of code would perform actual serialization
```groovy	
	Author author=Author.findByName('Jonathan Franzen')
	render author as XML
```
Producing the following hypothetical XML output
```xml	
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
		</books>
	
	</author>
```
while
```groovy	
	Author author=Author.findByName('Jonathan Franzen')
	render author as JSON
```
would produce the following JSON output
```json
	{
		"books":[
			{"isbn":"2222","name":"Freedom"},
			{"isbn":"1111","name":"The Twenty-Seventh City"}
		],
		"dob":"1959-08-17T00:00:00Z",
		"name":"Jonathan Franzen"
	}
```


Within the marshalling configuration closure there are several configuration options possible. 

* *shouldOutputIdentifier* when true will suppress serialization of domain object identifier (json,xml)
* *shouldOutputClass* whether class information should be serialized or not (json,xml)
* *shouldOutputVersion* whether version information should be serialized or not (json,xml)
* *identifier* is a comma separated list of fields which uniquely identifies a domain object in case database id is not sufficient. (xml)
* *elementName* configures a custom domain object element name which should be used instead of default one (xml)
* *attribute* is a comma separated list of field names which will be serialized as attributes of domain object element (xml)
* *deep* is a comma separated list of field names. If a field representing one-to-many relation is marked as *deep*, all contained data of related objects will be serialized (json,xml)
* *serializer* is a configuration option which allows us to define closures with custom serialization behavior. This configuration options allows us to customize serialization output for existing property (json,xml)
* *virtual* unlike *serializer* which will create completely new property
* *ignore* is a comma separated list of properties which should be ignored during serialization process (json,xml)
* *include* is an exclusive comma separated list of properties which will be included, meaning properties not listed will not be included (except for those specified using the should* options, or in case of XML, those listed as *attribute* or *identifier*). The *include* and *ignore* options are mutually exclusive - if both are defined, include takes priority (json,xml)


###Named and marshaller specific configuration

Example above specifies configuration will be applied both to xml and json marshallers. If we would like to specify marshaller specific configuration it can be done as follows

```  groovy	
class Author {
    	static marshalling={
		xml {
			//some xml specific configuration
		}
		json{
			//some json specific configuration
		}
			
	}
     
      	String name
      	Date dob
      	List books
}

   
```
Furthermore, it is possible to specify named configurations

```  groovy	
class Author {
    	static marshalling={
		xml {
			namedConfiguration{
				//some xml specific configuration
			}	
		}
		json{
			someOtherNamedConfig{
				//some json specific configuration
			}
			
		}
			
	}
     
      	String name
      	Date dob
      	List books
}

   
```
which will be than used as follows

```groovy	
	Author author=Author.findByName('Jonathan Franzen')
	XML.use('someNamedConfiguration'){
		render author as XML
	}
	
```

###Example serializer and virtual use

``` groovy
class Author {
    static marshalling={
		specialReport{
			serializer{  // customize the name output to all caps for our 'special report'
				 name { value, json -> json.value("${value.name.toUpperCase()}") }
				 //it is also possible to pass the marshalling context map as a parameter of virtual property
                 //value would be put to map as GenericDomainClassJSONMarshaller.marshallingContext.myCtxKey="context value"
                 ctxName { value, json,ctx -> json.value(ctx.myCtxKey) }
			}
			virtual{     // add a virtual property, in this case a date/time stamp
                time { value, json -> json.value("${new Date()}") }
            }
		}	
	}
     
    String name
    Date dob
    List books
}

```
