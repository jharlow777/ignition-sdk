package com.gpa.gateway.web;

import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.ConfirmationPanel;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;

import com.gpa.gateway.GatewayHook;
import com.gpa.gateway.records.HCSettingsRecord;

public class HCSettingsPage extends RecordEditForm {

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");

    public HCSettingsPage(final IConfigPage configPage) {
        super(configPage, null, new LenientResourceModel("HomeConnect.nav.settings.panelTitle"),
            ((IgnitionWebApp) Application.get()).getContext().getPersistenceInterface().find(HCSettingsRecord.META, 0L)
        );
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }

}