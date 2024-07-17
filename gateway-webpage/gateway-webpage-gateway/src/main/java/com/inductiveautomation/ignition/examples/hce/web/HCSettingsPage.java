package com.inductiveautomation.ignition.examples.hce.web;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.examples.hce.Elev8ConfigPanel;
import com.inductiveautomation.ignition.examples.hce.GatewayHook;
import com.inductiveautomation.ignition.examples.hce.records.HCSettingsRecord;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditMode;
import com.inductiveautomation.ignition.gateway.web.components.react.ReactComponent;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.BasicReactPanel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import com.inductiveautomation.ignition.gateway.localdb.persistence.Category;
import com.inductiveautomation.ignition.gateway.localdb.persistence.FormMeta;
import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord;
import com.inductiveautomation.ignition.gateway.web.models.CompoundRecordModel;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import simpleorm.dataset.SFieldMeta;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;

public class HCSettingsPage extends RecordEditForm {
    private static final LoggerEx log = LogUtil.getLogger(HCSettingsPage.class.getSimpleName());

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(GatewayHook.CONFIG_CATEGORY.getName(), "homeconnect");

    public HCSettingsPage(final IConfigPage configPage) {
        super(configPage, 
            null, 
            new LenientResourceModel("HomeConnect.nav.settings.panelTitle"),
            ((IgnitionWebApp) Application.get()).getContext().getPersistenceInterface().find(HCSettingsRecord.META, 0L)
        );
        log.info("HCSettingsPage()::initializing");
    }

    // @Override
    // protected void initComponents(List<SRecordInstance> records){
    //     // Form form = new Form("form");

    //     // Button submit = new Button("submit-button-test");
    //     // form.add(new Component[]{submit});
    //     // form.add(new Component[]{this.createCustomEditPanel("custom-edit-panel")});
    //     // this.add(new Component[]{form});
    //     // this.add(new Component[]{this.createFooterComponent("footer")});

    //     RecordEditMode mode = this.getMode(records);
    //     Form form = new Form("form");
    //     List<SRecordMeta<?>> recordTypes = new ArrayList(records.size());
    //     Iterator var5 = records.iterator();

    //     while(var5.hasNext()) {
    //         SRecordInstance rec = (SRecordInstance)var5.next();
    //         SRecordMeta<?> recMeta = rec.getMeta();
    //         if (recordTypes.contains(recMeta)) {
    //             throw new WicketRuntimeException("Cannot edit multiple records of the same type at once.");
    //         }

    //         recordTypes.add(recMeta);
    //     }

    //     Map<Category, List<FormMeta>> categories = new TreeMap();
    //     Map<SFieldMeta, SRecordInstance> recordLookup = new HashMap();

    //     for(int i = 0; i < recordTypes.size(); ++i) {
    //         SRecordMeta<? extends SRecordInstance> meta = (SRecordMeta)recordTypes.get(i);
    //         SRecordInstance record = (SRecordInstance)records.get(i);
    //         List<SFieldMeta> fields = meta.getFieldMetas();
    //         Iterator var11 = fields.iterator();

    //         while(var11.hasNext()) {
    //             SFieldMeta field = (SFieldMeta)var11.next();
    //             FormMeta formMeta = (FormMeta)field.getUserProperty(PersistentRecord.FORM_META_KEY);
    //             if (formMeta != null && formMeta.isVisible()) {
    //             recordLookup.put(field, record);
    //             Category category = formMeta.getCategory();
    //             if (category == null) {
    //                 category = new Category("RecordEditForm.Category.Other", Integer.MAX_VALUE, false);
    //             }

    //             List<FormMeta> formMetas = (List)categories.get(category);
    //             if (formMetas == null) {
    //                 formMetas = new ArrayList();
    //                 categories.put(category, formMetas);
    //             }

    //             ((List)formMetas).add(formMeta);
    //             }
    //         }
    //     }

    //     RepeatingView tableRepeater = new RepeatingView("category-table");
    //     this.addStandardCategories(tableRepeater, mode, categories, recordLookup);
    //     form.add(new Component[]{tableRepeater});
    //     AjaxCheckBox cb = new 2(this, "adv", new PropertyModel(this, "showAdvanced"));
    //     cb.setOutputMarkupId(true);
    //     this.ajaxComponents.add(cb);
    //     cb.setVisible(this.showAdvancedCheckbox(categories.keySet()));
    //     form.add(new Component[]{cb});
    //     tableRepeater = new RepeatingView("category-table-adv");
    //     this.addAdvancedCategories(tableRepeater, mode, categories, recordLookup);
    //     form.add(new Component[]{tableRepeater});
    //     Button submit = new Button("submit-button", new StringResourceModel("RecordEditForm.SubmitButton.${mode}", this, new Model(this), new Object[0]));
    //     form.add(new Component[]{submit});
    //     form.add(new Component[]{this.createCustomEditPanel("custom-edit-panel")});
    //     this.add(new Component[]{form});
    //     this.add(new Component[]{this.createFooterComponent("footer")});

    //     log.info("HCSettingsPage()::Created form: " + form.getId());

    // }

    // @Override
    // public Component createCustomEditPanel(String id) {
    //     log.info("HCSettingsPage()::createCustomEditPanel");
    //     return new BasicReactPanel("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus");
    // }

    @Override
    public WebMarkupContainer createFooterComponent(String id) {
        log.info("HCSettingsPage()::createCustomFooterComponent");
        return new BasicReactPanel("react", "/res/hce/js/homeconnectstatus.js", "homeconnectstatus");
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }

}
