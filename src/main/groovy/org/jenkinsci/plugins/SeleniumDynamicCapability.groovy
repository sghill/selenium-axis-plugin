package org.jenkinsci.plugins

import hudson.Extension
import org.kohsuke.stapler.DataBoundConstructor
import hudson.model.Descriptor
import hudson.DescriptorExtensionList
import jenkins.model.Jenkins

class SeleniumDynamicCapability extends  ComplexAxisItemContainer {

    SeleniumDynamicCapability() {
        super(new ArrayList<SeleniumCapability>())
    }

    @DataBoundConstructor
    SeleniumDynamicCapability(List<SeleniumCapability> seleniumCapabilities) {
        super( seleniumCapabilities)
    }

    @Extension public static class DescriptorImpl extends ComplexAxisItemContainerDescriptor {

        //so we need this to get at the name of the selenium server in the global config
        protected static Descriptor<SeleniumAxis.DescriptorImpl> getTopLevelDescriptor(){
            //def xxx = Jenkins.getInstance().getPlugin('selenium-axis')

            //def yyy=Jenkins.getInstance().getPluginManager().getPlugins()

            def xxx = Jenkins.getInstance().getDescriptor(SeleniumAxis.class)
            xxx.load()

            return xxx
        }

        @Override
        public static List<SeleniumCapability> loadDefaultItems(){
            SeleniumAxis.DescriptorImpl tld = getTopLevelDescriptor()

            def sel = new Selenium(tld.getServer())

            //def ret = new ArrayList<SeleniumDynamicCapability>()
            //ret.add(new SeleniumDynamicCapability(sel.getSeleniumCapabilities()))
            //return ret
            return sel.getSeleniumCapabilities()
        }

        @Override public String getDisplayName() {
            return "Selenium Dynamic Capability";
        }

    }
}
