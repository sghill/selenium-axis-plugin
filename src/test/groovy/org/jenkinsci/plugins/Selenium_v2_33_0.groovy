package org.jenkinsci.plugins

import org.jsoup.Jsoup
import spock.lang.Specification

class Selenium_v2_33_0 extends Specification {

    def 'tests'() {
        SeleniumHubCapabilityReader.metaClass.rawRead = {
            String s -> Jsoup.parse(this.class.getResourceAsStream(s), 'UTF-8', '')
        }

        when:
        def reader = new SeleniumHubCapabilityReader()
        reader.loadCapabilities('/grid-2.33.0.html')
        def sel = new Selenium(reader, SeleniumCapability)

        then:
        assert sel.seleniumCapabilities.size() == 4
        //assert sel.seleniumVer.matches('Grid Console v.2.33.0')
        assert sel.seleniumCapabilities.toString() ==
                'SEL-Any-phantomjs-Any\nSEL-LINUX-chrome-25\nSEL-LINUX-firefox-Any\nSEL-LINUX-opera-12\n'
    }
}
