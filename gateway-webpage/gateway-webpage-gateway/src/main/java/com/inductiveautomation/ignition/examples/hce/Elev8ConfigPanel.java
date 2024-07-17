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

public class Elev8ConfigPanel extends ConfigPanel{
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigPanel.class.getSimpleName());

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");

    protected void initComponents(){
        Form form = new Form("form");

        Button submit = new Button("submit-button-test");
        form.add(new Component[]{submit});
        form.add(new Component[]{this.createCustomEditPanel("custom-edit-panel")});
        this.add(new Component[]{form});
        this.add(new Component[]{this.createFooterComponent("footer")});
        log.info("Elev8ConfigPanel()::Created form: " + form.getId());
    }

    protected Component createCustomEditPanel(String id) {
        log.info("Elev8ConfigPanel()::createCustomEditPanel");
        return new BasicReactPanel("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus");
    }

    protected WebMarkupContainer createFooterComponent(String id) {
      return new InvisibleContainer(id);
    }

    public Elev8ConfigPanel(final IConfigPage configPage) {
        super("HomeConnect.nav.settings.title");
        log.info("Elev8ConfigPanel()::Initializing");
        this.initComponents();
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
