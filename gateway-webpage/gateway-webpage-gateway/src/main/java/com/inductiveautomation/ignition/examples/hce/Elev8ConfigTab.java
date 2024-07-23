package com.inductiveautomation.ignition.examples.hce;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.pages.BasicReactPanel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.components.AbstractNamedTab;

import java.util.Arrays;

import org.apache.wicket.model.IModel;
import org.apache.wicket.markup.html.WebMarkupContainer;

public abstract class Elev8ConfigTab extends AbstractNamedTab implements IConfigTab {
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigTab.class.getSimpleName());

    Elev8ConfigTab(String category, String name, String titleKey) {
        super(name, category, titleKey);
        log.info("Elev8ConfigTab()::initializing");
    }

}