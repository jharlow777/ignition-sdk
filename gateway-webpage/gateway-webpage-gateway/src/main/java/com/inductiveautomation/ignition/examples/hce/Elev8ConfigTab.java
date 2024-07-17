package com.inductiveautomation.ignition.examples.hce;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.pages.BasicReactPanel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

import java.util.Arrays;

import org.apache.wicket.markup.html.WebMarkupContainer;

public class Elev8ConfigTab extends DefaultConfigTab {
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigTab.class.getSimpleName());

    Elev8ConfigTab(ConfigCategory category, String name, String titleKey, Class<? extends ConfigPanel> panelClass) {
        super(category, name, titleKey, panelClass);
        log.info("Elev8ConfigTab()::initializing");
    }

    @Override
    public ConfigPanel getPanel(IConfigPage configPage) {
        log.info("Elev8ConfigTab()::getPanel(configPage)");
        return new Elev8ConfigPanel(configPage);
    }

    @Override
    public WebMarkupContainer getPanel(String panelId) {
        log.info("Elev8ConfigTab()::getPanel(panelId):" + panelId);
        return new BasicReactPanel(panelId, "/res/hce/js/homeconnectstatus.js", "homeconnectstatus");
    }

    @Override
    public Iterable<String> getSearchTerms(){
        return Arrays.asList("home connect", "hce");
    }

}