package com.gpa.designer;

import org.fakester.common.component.display.Image;
import org.fakester.common.component.display.Messenger;
import org.fakester.common.component.display.TagCounter;

import com.gpa.designer.component.TagCountDesignDelegate;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.perspective.designer.DesignerComponentRegistry;
import com.inductiveautomation.perspective.designer.api.ComponentDesignDelegateRegistry;
import com.inductiveautomation.perspective.designer.api.PerspectiveDesignerInterface;


/**
 * The 'hook' class for the designer scope of the module.  Registered in the ignitionModule configuration of the
 * root build.gradle file.
 */
public class DesignerHook extends AbstractDesignerModuleHook {
    private static final LoggerEx logger = LoggerEx.newBuilder().build("DesignerHook");

    private DesignerContext context;
    private DesignerComponentRegistry registry;
    private ComponentDesignDelegateRegistry delegateRegistry;

    static {
        BundleUtil.get().addBundle("radcomponents", DesignerHook.class.getClassLoader(), "radcomponents");
    }

    public DesignerHook() {
        logger.info("Registering Components in Designer!");
    }

    @Override
    public void startup(DesignerContext context, LicenseState activationState) {
        this.context = context;
        init();
    }

    private void init() {
        PerspectiveDesignerInterface pdi = PerspectiveDesignerInterface.get(context);

        registry = pdi.getDesignerComponentRegistry();
        delegateRegistry = pdi.getComponentDesignDelegateRegistry();

        // register components to get them on the palette
        registry.registerComponent(Image.DESCRIPTOR);
        registry.registerComponent(TagCounter.DESCRIPTOR);
        registry.registerComponent(Messenger.DESCRIPTOR);

        // register design delegates to get the special config UI when a component type is selected in the designer
        delegateRegistry.register(TagCounter.COMPONENT_ID, new TagCountDesignDelegate());
    }

    @Override
    public void shutdown() {
        removeComponents();
    }

    private void removeComponents() {
        registry.removeComponent(Image.COMPONENT_ID);
        registry.removeComponent(TagCounter.COMPONENT_ID);
        registry.removeComponent(Messenger.COMPONENT_ID);

        delegateRegistry.remove(TagCounter.COMPONENT_ID);
    }
}
