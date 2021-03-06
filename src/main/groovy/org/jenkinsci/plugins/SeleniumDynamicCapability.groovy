package org.jenkinsci.plugins

import hudson.Extension
import org.kohsuke.stapler.DataBoundConstructor
import hudson.model.Descriptor
import jenkins.model.Jenkins
import org.jenkinsci.complex.axes.AxisDescriptor
import org.jenkinsci.complex.axes.Item
import org.jenkinsci.complex.axes.Container
import org.jenkinsci.complex.axes.ContainerDescriptor

class SeleniumDynamicCapability extends  Container {

    SeleniumDynamicCapability() {
        super( [] )
    }

    List<SeleniumCapabilityRO> getSeleniumCapabilities() {
        complexAxisItems
    }

    void setSeleniumCapabilities(List<SeleniumCapabilityRO> sc) {
        setComplexAxisItems(sc)
    }

    @DataBoundConstructor
    SeleniumDynamicCapability(List<SeleniumCapabilityRO> seleniumCapabilities) {
        super( seleniumCapabilities)
    }

    String toString() {
        'DetectedSelenium'
    }

    @Override
    List<String> rebuild(List<String> list) {
        def sc = descriptor.loadDefaultItems()

        if (sc.size() == 0) {
            throw (new SeleniumException('No capabilities detected'))
        }
        setSeleniumCapabilities(sc)

        sc.each { list.add(it.toString()) }
        list
    }

    @Override
    List<String> getValues(List<String> list) {
        seleniumCapabilities.each { list.add(it.toString()) }

        if (list.size() == 0) {
            list.add('Rebuilt at build time')
        }
        list
    }

    @Extension static class DescriptorImpl extends ContainerDescriptor {

        //so we need this to get at the name of the selenium server in the global config
        static Descriptor<? extends AxisDescriptor> getTopLevelDescriptor() {
            SeleniumAxis.DescriptorImpl sad = Jenkins.instance.getDescriptor(SeleniumAxis)
            sad.load()

            sad
        }

        @Override
        List<? extends Item> loadDefaultItems(List<? extends Item> cai) {
            def sdc = new SeleniumDynamicCapability(loadDefaultItems())

            cai.add(sdc)

            cai
        }

        @Override
        List<? extends Item> loadDefaultItems() {
            topLevelDescriptor.seleniumCapabilities
        }

        String displayName = 'Detected Selenium Capability'
    }
}
