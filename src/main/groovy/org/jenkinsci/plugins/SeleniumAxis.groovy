/*
* The MIT License
*
* Copyright (c) 2010, InfraDNA, Inc.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/
package org.jenkinsci.plugins

import hudson.Extension
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import hudson.util.FormValidation
import org.kohsuke.stapler.QueryParameter
import jenkins.model.Jenkins
import org.jenkinsci.complex.axes.Axis
import org.jenkinsci.complex.axes.AxisDescriptor
import org.jenkinsci.complex.axes.Item
import org.jenkinsci.complex.axes.ItemList
import org.jenkinsci.complex.axes.ItemDescriptor
import hudson.util.Secret
import org.kohsuke.stapler.StaplerRequest
import hudson.model.Descriptor.FormException
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript

class SeleniumAxis extends Axis {
    Boolean slOverride
    String slName
    Secret slPassword

    @DataBoundConstructor
    SeleniumAxis(String name, Boolean slOverride, String slName, Secret slPassword,
                 List<? extends Item> seleniumCapabilities) {
        super(name, seleniumCapabilities)

        this.slOverride = slOverride
        this.slName = slName
        this.slPassword = slPassword
    }

    List<? extends SeleniumCapability> getSeleniumCapabilities() {
        this.complexAxisItems as List<? extends SeleniumCapability>
    }

    String getURL(String which) {
        if (which == 'SEL') {
            descriptor.server
        } else if (which == 'SL') {
            if (slOverride) {
                buildURL(slName, slPassword, descriptor.sauceLabsURL)
            } else {
                buildURL(descriptor.sauceLabsName, descriptor.sauceLabsPwd, descriptor.sauceLabsURL)
            }
        }
    }

    static String buildURL(String name, Secret pwd, String url) {
        String myurl = url - 'http://'
        "http://${name}:${pwd}@${myurl}"
    }

    @Override
    void addBuildVariable(String value, Map<String,String> map) {

       //so the value is PLATFORM-BROWSER-VERSION

       List<String> parts = value.split(/-/)

       map.put(name + '_PLATFORM', parts[1])
       map.put(name + '_BROWSER', parts[2])
       map.put(name + '_VERSION', parts[3])
       map.put(name + '_URL', getURL(parts[0]))
    }

    @Extension
    static class DescriptorImpl extends AxisDescriptor {

        final String displayName = 'Selenium Capability Axis'

        String server = 'http://localhost:4444'

        Boolean sauceLabs = false
        String sauceLabsName
        Secret sauceLabsPwd
        String sauceLabsAPIURL = 'http://saucelabs.com/rest/v1/info/platforms/webdriver'
        String sauceLabsURL = 'http://ondemand.saucelabs.com:80'

        //don't serialize this
        @SuppressWarnings('UnnecessaryTransientModifier')
        transient Map<String, List<? extends SeleniumCapability>> sauceLabsCapabilities

        List<ItemDescriptor> axisItemTypes() {
            def ait = Jenkins.instance.<Item,ItemDescriptor>getDescriptorList(Item)

            List<ItemDescriptor> ret = []

            for (int i = 0; i < ait.size(); i++) {
                String name = ait.get(i).getClass().name

                //don't want the RO version to appear in the add list as it is added as part of the Dynamic item
                if (!name.contains('RO$')) {
                    ret.add(ait.get(i))
                }
            }
            ret
        }

        List<? extends SeleniumCapability> getSauceLabsCapabilities(String which) {
            if (sauceLabs) {
                try {
                    if (sauceLabsCapabilities == null) {
                        SauceLabsCapabilityReader reader = new SauceLabsCapabilityReader()
                        reader.loadCapabilities(sauceLabsAPIURL)

                        Selenium sel = new Selenium(reader, SauceLabsCapabilityRO)
                        sauceLabsCapabilities = [:]

                        sauceLabsCapabilities['latest'] = sel.seleniumLatest
                        sauceLabsCapabilities['all'] = sel.seleniumCapabilities
                        sauceLabsCapabilities['web'] = sel.seleniumSelected
                    }
                    return sauceLabsCapabilities[which]

                } catch (ex) {
                    ItemList.emptyList()
                }
            } else {
                ItemList.emptyList()
            }
        }

        List<? extends SeleniumCapability> getSeleniumCapabilities() {
            try {
                //def sel = new Selenium(Selenium.load(server), SeleniumCapabilityRO)
                SeleniumHubCapabilityReader reader = new SeleniumHubCapabilityReader()
                reader.loadCapabilities(server)

                Selenium sel = new Selenium(reader, SeleniumCapabilityRO)
                sel.seleniumLatest

            } catch (ex) {
                ItemList.emptyList()
            }
        }
        List<? extends SeleniumCapability> getRandomSauceLabsCapabilities(String which, Integer count, SecureGroovyScript secureFilter) {

            ItemList<? extends SeleniumCapability> selected = new ItemList<? extends SeleniumCapability>()
            def cap = getSauceLabsCapabilities(which)
            def myCap = cap.clone()

            Collections.shuffle(myCap)

            if (myCap.size() == 0) {
                return ItemList.emptyList()
            }

            while (count > 0 && myCap.size() > 0) {
                def current = myCap.pop()
                //boolean differentEnough = true

                Binding binding = new Binding()
                binding.setVariable('current', current)
                binding.setVariable('selected', selected)

                println(secureFilter.script)

                def differentEnough = secureFilter.evaluate(getClass().classLoader, binding)

                println(differentEnough.toString())

                if (differentEnough) {
                    selected << current
                    count--
                }

            }
            selected
        }
        @Override
        boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            super.configure( req, formData)
            sauceLabsCapabilities = null
            true
        }

        FormValidation doCheckServer(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error('You must provide an URL.')
            }

            try {
                new URL(value)
            } catch (final MalformedURLException e) {
                return FormValidation.error('This is not a valid URL.')
            }
            FormValidation.ok()
        }

        FormValidation doCheckSauceLabsURL(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error('You must provide an URL.')
            }

            try {
                new URL(value)
            } catch (final MalformedURLException e) {
                return FormValidation.error('This is not a valid URL.')
            }
            FormValidation.ok()
        }

        FormValidation doCheckSauceLabsAPIURL(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error('You must provide an URL.')
            }

            try {
                new URL(value)
            } catch (final MalformedURLException e) {
                return FormValidation.error('This is not a valid URL.')
            }
            FormValidation.ok()
        }
    }
}
