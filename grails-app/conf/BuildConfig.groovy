grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6

grails.project.dependency.resolution = {

    inherits("global")
    log "warn" 
    repositories {
		 grailsCentral()
        mavenCentral()
        mavenLocal()
 
    }
    plugins {
		build (':release:2.2.1') {
            export = false
        }
		test(':spock:0.7') {
			export = false
			exclude 'spock-grails-support'
		}
    }
    dependencies {

       test('org.spockframework:spock-grails-support:0.7-groovy-2.0') {
			export = false
		}

    }
}
