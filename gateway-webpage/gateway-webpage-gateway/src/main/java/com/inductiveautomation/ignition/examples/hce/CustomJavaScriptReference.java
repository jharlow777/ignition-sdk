package com.inductiveautomation.ignition.examples.hce;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class CustomJavaScriptReference extends JavaScriptResourceReference {
    public CustomJavaScriptReference() {
        super(CustomJavaScriptReference.class, "/res/hce/js/homeconnectstatus.js");
    }
}