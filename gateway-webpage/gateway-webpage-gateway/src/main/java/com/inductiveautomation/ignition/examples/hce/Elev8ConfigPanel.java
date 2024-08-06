package com.inductiveautomation.ignition.examples.hce;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.react.ReactComponent;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

public class Elev8ConfigPanel extends ConfigPanel{
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigPanel.class.getSimpleName());

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");


    public Elev8ConfigPanel(String id) {
        super(id);
        initComponents();
    }

    public Elev8ConfigPanel(String id, String titleKey) { 
        super(id, titleKey);
        initComponents();
    }

    public Elev8ConfigPanel(String titleKey, IConfigPage configPage, ConfigPanel returnPanel) {
        super(titleKey, configPage, returnPanel);
        initComponents();
    }

    public Elev8ConfigPanel(String titleKey, String defaultTitle, IConfigPage configPage, ConfigPanel returnPanel) {
        super(titleKey, defaultTitle, configPage, returnPanel);
        initComponents();
    }

    private void initComponents() {
        WebMarkupContainer reactContainer = new WebMarkupContainer("reactContainer");
        reactContainer.setOutputMarkupId(true);
        // JavaScriptResourceReference reactJs = new JavaScriptResourceReference(Elev8ConfigPanel.class, "../../../../../mounted/js/homeconnectstatus.js");
        // reactContainer.add(new Component[]{new ReactComponent("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus")});

        add(reactContainer);

    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // Add your JavaScript file to the head
        log.info("Rendering react");
        JavaScriptResourceReference reactJs = new JavaScriptResourceReference(Elev8ConfigPanel.class, "../../../../../mounted/js/homeconnectstatus.js");
        response.render(JavaScriptHeaderItem.forReference(reactJs));
    }

    public Elev8ConfigPanel(IConfigPage configPage) {
        super("HomeConnect.nav.settings.title", "Install", configPage, null);
        log.info("Elev8ConfigPanel()::Initializing with basic configPage constructor");
        this.initComponents();
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }
}
