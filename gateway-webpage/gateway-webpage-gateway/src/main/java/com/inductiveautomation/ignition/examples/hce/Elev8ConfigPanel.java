package com.inductiveautomation.ignition.examples.hce;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;

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

    public Elev8ConfigPanel(String titleKey, String defaultTitle, IConfigPage configPage, ConfigPanel returnPanel) {
        super(titleKey, defaultTitle, configPage, returnPanel);
        initComponents();
    }

    private void initComponents() {
        Form form = new Form("form");
        form.add(new Component[]{new ReactComponent("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus")});
        // this.add(new ReactComponent("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus"));
        // form.add(new BasicReactPanel("panel-id", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus"));
        this.add(new Component[]{form});

        // WebMarkupContainer reactContainer = new WebMarkupContainer("reactContainer");
        // reactContainer.setOutputMarkupId(true);
        // // reactContainer.add(new Component[]{new ReactComponent("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus")});
        // reactContainer.add(new BasicReactPanel("1", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus"));
        // add(reactContainer);
        // String text = reactContainer.getMarkup().toString(true);
        // log.info("Elev8ConfigPanel()::initComponents()::added BasicReactPanel: " + text);
    }

    public Elev8ConfigPanel(IConfigPage configPage) {
        super("HomeConnect.nav.settings.title", "install", configPage, null);
        log.info("Elev8ConfigPanel()::Initializing with basic configPage constructor");
        this.initComponents();
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }
}
