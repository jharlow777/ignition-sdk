package com.gpa.gateway.web;

import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.components.ConfirmationPanel;
import com.inductiveautomation.ignition.gateway.web.components.DescriptiveRadioChoice;
import com.inductiveautomation.ignition.gateway.web.components.RecordChoicePanel;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.models.RecordChoiceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.gpa.gateway.GatewayHook;
import com.gpa.gateway.records.HCSettingsRecord;

public class HCSettingsPage extends ConfigPanel {
    // PropertyModel<String> messageModel = new PropertyModel<>(this, "message");

    // public HCSettingsPage() {
    //     Form<?> form = new Form("form");
    //     form.add(new TextField<>("msgInput", messageModel));
    //     add(form);
    // }
    private static final LoggerEx log = LoggerEx.newBuilder().build("gpa.gateway.web.HCSettingsPageReact");

   protected Pair<String, String> menuLocation;

   public HCSettingsPage(Pair<String, String> menuLocation, String titleKey, String descriptionKey) {
      super(titleKey);
      this.menuLocation = menuLocation;
      Form form = new Form(titleKey);
      IModel<String> selected = new Model<String>();

      RadioGroup group = new RadioGroup("group", selected);
      form.add(group);

      group.add(new Radio("aapl", new Model<String>("AAPL")));
      group.add(new Radio("goog", new Model<String>("GOOG")));
      group.add(new Radio("msft", new Model<String>("MSFT")));
      this.add(new Component[]{form});
   }

   public Pair<String, String> getMenuLocation() {
      return this.menuLocation;
   }

   protected void onSubmit(Radio selectedChoice) {
        log.info("Selected " + selectedChoice.getValue());
   }

}