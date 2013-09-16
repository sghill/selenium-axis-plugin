package org.jenkinsci.plugins.SeleniumCapabilityRO
/*
This Groovy script is used to produce the global configuration option.

Jenkins uses a set of tag libraries to provide uniformity in forms.
To determine where this tag is defined, first check the namespace URI,
and then look under $JENKINS/views/. For example, section() is defined
in $JENKINS/views/lib/form/section.jelly.

It's also often useful to just check other similar scripts to see what
tags they use. Views are always organized according to its owner class,
so it should be straightforward to find them.
*/
namespace(lib.FormTagLib).with {
    readOnlyTextbox( value:instance.toString())
    input(name:"_.browserName", value:instance.browserName, type:"hidden")
    input(name:"_.platformName", value:instance.platformName, type:"hidden")
    input(name:"_.browserVersion", value:instance.browserVersion, type:"hidden")
}

