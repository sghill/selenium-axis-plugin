package org.jenkinsci.plugins

import hudson.matrix.MatrixProject
import hudson.matrix.AxisList
import hudson.util.Secret
import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript
import org.jenkinsci.plugins.scriptsecurity.scripts.ApprovalContext
import org.jenkinsci.plugins.scriptsecurity.scripts.ClasspathEntry
import org.jsoup.Jsoup
import spock.lang.Specification
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule


class SeleniumAxisSpec extends Specification {

    @Rule
    JenkinsRule rule = new JenkinsRule()

    MatrixProject configure(seleniumFile, sauceLabsFile, advanced = false) {
        SeleniumAxis.DescriptorImpl seleniumAxisDescriptor = Jenkins.instance.getDescriptorOrDie(SeleniumAxis)

        SeleniumHubCapabilityReader.metaClass.rawRead = {
            String s -> Jsoup.parse(this.class.getResourceAsStream(s), 'UTF-8', '')
        }
        SauceLabsCapabilityReader.metaClass.rawRead = {
            String s -> this.class.getResource(s).text
        }

        seleniumAxisDescriptor.server = seleniumFile
        seleniumAxisDescriptor.sauceLabs = true
        seleniumAxisDescriptor.sauceLabsName = 'test'
        seleniumAxisDescriptor.sauceLabsPwd = new Secret('pass')
        seleniumAxisDescriptor.sauceLabsAPIURL = sauceLabsFile

        def matrixProject = rule.createProject(MatrixProject, "m")

        SeleniumAxis axis = new SeleniumAxis('TEST', false, '', new Secret(''),
                seleniumAxisDescriptor.loadDefaultItems() )

        if(advanced) {
            axis.complexAxisItems[0].secureFilter = new SecureGroovyScript('true', true, Collections.<ClasspathEntry> emptyList())
            axis.complexAxisItems[0].secureFilter.configuring(ApprovalContext.create())
            axis.complexAxisItems[0].advanced = true
            axis.complexAxisItems[0].criteria = 'latest'
        }

        def axl = new AxisList()

        axl << axis
        matrixProject.setAxes(axl)

        matrixProject
    }

    MatrixProject configureManual() {

        def matrixProject = rule.createProject(MatrixProject, "m")

        def sc = new SeleniumCapability()
        def sc2 = new SeleniumCapability('Browser', 'Platform', 'Version', 'SEL')
        def scont = []
        scont.add(sc)
        scont.add(sc2)

        def axis = new SeleniumAxis('TEST', false, '', new Secret(''), scont)

        def axl = new AxisList()

        axl.add(axis)
        matrixProject.setAxes(axl)

        matrixProject
    }

    def 'Dynamic-advanced'() {
        given:
        def matrixProject = configure('/grid-2.35.0.html', '/saucelabs_3.json', true)

        when:
        def build = matrixProject.scheduleBuild2(0).get()
        def runs = build.runs

        then:
        build.logFile.text.contains('SUCCESS')
        runs.every { it.logFile.text.contains('SUCCESS') }
        runs.size() == 7
    }


    def 'Dynamic'() {
        given:
        def matrixProject = configure('/grid-2.35.0.html', '/saucelabs_3.json')

        when:
        def build = matrixProject.scheduleBuild2(0).get()
        def runs = build.runs

        then:
        build.logFile.text.contains('SUCCESS')
        runs.every { it.logFile.text.contains('SUCCESS') }
        runs.size() == 7
    }

    def 'Dynamic Grid Gone'() {
        given:
        def matrixProject = configure('/grid-2.35.0.html', '/saucelabs_3.json')
        SeleniumAxis.DescriptorImpl seleniumAxisDescriptor = Jenkins.instance.getDescriptorOrDie(SeleniumAxis)

        when:
        seleniumAxisDescriptor.setServer('null://')
        def build = matrixProject.scheduleBuild2(0).get()
        def runs = build.runs

        then:
        build.logFile.text.contains('SeleniumException')
        runs.size() == 0
    }

    def 'Dynamic Grid Arrived'() {
        given:

        SeleniumAxis.DescriptorImpl seleniumAxisDescriptor = Jenkins.instance.getDescriptorOrDie(SeleniumAxis)

        def matrixProject = rule.createProject(MatrixProject, "m")

        AxisList axl = new AxisList()
        seleniumAxisDescriptor.setServer('/empty-grid-2.35.0.html')

        def axis = new SeleniumAxis('TEST', false, '', new Secret(''), seleniumAxisDescriptor.loadDefaultItems())

        axl.add(axis)
        matrixProject.setAxes(axl)
        seleniumAxisDescriptor.setServer('/grid-2.25.0.html')

        def build = matrixProject.scheduleBuild2(0).get()

        expect: build.logFile.text.contains('SUCCESS')

        when:
        def runs = build.runs
        then:
        runs.each { assert it.logFile.text.contains('SUCCESS') }
    }

    def 'Empty Axis'() {
        given:
        def matrixProject = rule.createProject(MatrixProject, "m")

        AxisList axl = new AxisList()
        def axis = new SeleniumAxis('TEST', false, '', new Secret(''), null)

        axl.add(axis)
        matrixProject.setAxes(axl)
        def build = matrixProject.scheduleBuild2(0).get()

        expect:
        build.logFile.text.contains('SUCCESS')
        build.runs.size() == 0
    }

    def 'Manual'() {
        given:
        def matrixProject = configureManual()

        when:
        def build = matrixProject.scheduleBuild2(0).get()
        def runs = build.runs

        then:
        build.logFile.text.contains('SUCCESS')
        runs.every { it.logFile.text.contains('SUCCESS') }
        runs.size() == 2
    }
}
