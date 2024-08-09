package com.inductiveautomation.ignition.examples.hce;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.components.AbstractNamedTab;

public abstract class Elev8ConfigTab extends AbstractNamedTab implements IConfigTab {
    private static final LoggerEx log = LogUtil.getLogger(Elev8ConfigTab.class.getSimpleName());

    Elev8ConfigTab(String category, String name, String titleKey) {
        super(name, category, titleKey);
        log.info("Elev8ConfigTab()::initializing");
    }

}