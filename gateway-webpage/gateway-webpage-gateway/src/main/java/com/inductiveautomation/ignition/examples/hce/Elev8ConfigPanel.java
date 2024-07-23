package com.inductiveautomation.ignition.examples.hce;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.InvisibleContainer;
import com.inductiveautomation.ignition.gateway.web.pages.BasicReactPanel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import com.inductiveautomation.ignition.gateway.web.components.react.ReactComponent;

public class Elev8ConfigPanel extends ConfigPanel{
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigPanel.class.getSimpleName());

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");

    protected void initComponents(){
        log.info("Elev8ConfigPanel()::initComponents()::initializing Elev8ConfigPanel");
    }

    public Elev8ConfigPanel(final IConfigPage configPage) {
        super("HomeConnect.nav.settings.title");
        log.info("Elev8ConfigPanel()::Initializing with configPage");
        this.initComponents();
    }

    public Elev8ConfigPanel(String titleKey, String defaultTitle, IConfigPage configPage, ConfigPanel returnPanel, String id, String jsUrl, String jsLibraryName) {
        super(titleKey, defaultTitle, configPage, returnPanel);
        log.info("Elev8ConfigPanel()::Initializing with ReactComponent");
        this.add(new Component[]{new ReactComponent("react", jsUrl, jsLibraryName)});
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }
    
//     public Elev8ConfigPanel(String titleKey, String jsUrl, String jsLibraryName) {
//       super(titleKey);
//       this.initComponents();
//       this.add(new Component[]{new ReactComponent("react", jsUrl, jsLibraryName)});
//    }
}
