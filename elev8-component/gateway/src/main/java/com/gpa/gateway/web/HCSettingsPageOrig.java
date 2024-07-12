package com.gpa.gateway.web;

import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.ConfirmationPanel;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

import com.gpa.gateway.GatewayHook;
import com.gpa.gateway.records.HCSettingsRecord;

public class HCSettingsPageOrig extends RecordEditForm {
    // PropertyModel<String> messageModel = new PropertyModel<>(this, "message");

    // public HCSettingsPage() {
    //     Form<?> form = new Form("form");
    //     form.add(new TextField<>("msgInput", messageModel));
    //     add(form);
    // }
    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");

    public HCSettingsPageOrig(final IConfigPage configPage) {
        super(configPage, null, new LenientResourceModel("HomeConnect.nav.settings.panelTitle"),
            ((IgnitionWebApp) Application.get()).getContext().getPersistenceInterface().find(HCSettingsRecord.META, 0L)
        );
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }

}