package org.fakester.designer;

import java.util.List;
import java.util.Collections;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.perspective.designer.DesignerComponentRegistry;
import com.inductiveautomation.perspective.designer.api.ComponentDesignDelegateRegistry;
import com.inductiveautomation.perspective.designer.api.PerspectiveDesignerInterface;
import org.fakester.common.component.display.Image;
import org.fakester.common.component.display.Messenger;
import org.fakester.common.component.display.TagCounter;
import org.fakester.designer.component.TagCountDesignDelegate;
import com.inductiveautomation.ignition.common.project.Project;
import com.inductiveautomation.ignition.common.project.resource.ProjectResource;
import com.inductiveautomation.ignition.common.project.resource.ResourcePath;
import com.inductiveautomation.ignition.common.project.resource.ResourceType;

/**
 * The 'hook' class for the designer scope of the module.  Registered in the ignitionModule configuration of the
 * root build.gradle file.
 */
public class RadDesignerHook extends AbstractDesignerModuleHook {
    private static final LoggerEx logger = LoggerEx.newBuilder().build("rad.designer.RadComponents");

    private DesignerContext context;
    private DesignerComponentRegistry registry;
    private ComponentDesignDelegateRegistry delegateRegistry;

    static {
        BundleUtil.get().addBundle("radcomponents", RadDesignerHook.class.getClassLoader(), "radcomponents");
    }

    public RadDesignerHook() {
        logger.info("Registering Rad Components in Designer!");
    }

    @Override
    public void startup(DesignerContext context, LicenseState activationState) {
        this.context = context;
        init();
    }

    private void init() {
        logger.debug("Initializing registry entrants...");

        PerspectiveDesignerInterface pdi = PerspectiveDesignerInterface.get(context);

        registry = pdi.getDesignerComponentRegistry();
        delegateRegistry = pdi.getComponentDesignDelegateRegistry();

        Project project = this.context.getProject();
        String projectName = project.getName();
        logger.info("Project Name: " + projectName);

        ResourcePath trackAndTracePath = new ResourcePath(
            new ResourceType("com.inductiveautomation.perspective", "views"), 
            "App/TrackAndTrace"
        );

        List<ProjectResource> trackAndTraceResources = project
            .browse(trackAndTracePath)
            .orElseGet(Collections::emptyList);
        logger.debug("TRACK AND TRACE RESOURCES: " + trackAndTraceResources.size());
        trackAndTraceResources.forEach(res -> {
            logger.debug("RES: " + res.getResourceName());
        });

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
