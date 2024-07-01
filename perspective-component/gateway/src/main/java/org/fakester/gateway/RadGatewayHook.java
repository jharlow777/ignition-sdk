package org.fakester.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.project.Project;
import com.inductiveautomation.ignition.common.project.resource.ProjectResource;
import com.inductiveautomation.ignition.common.project.resource.ResourcePath;
import com.inductiveautomation.ignition.common.project.resource.ResourceType;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.perspective.common.api.ComponentRegistry;
import com.inductiveautomation.perspective.gateway.api.ComponentModelDelegateRegistry;
import com.inductiveautomation.perspective.gateway.api.PerspectiveContext;
import org.fakester.common.RadComponents;
import org.fakester.common.component.display.Image;
import org.fakester.common.component.display.Messenger;
import org.fakester.common.component.display.TagCounter;
import org.fakester.gateway.delegate.MessageComponentModelDelegate;
import org.fakester.gateway.endpoint.DataEndpoints;


public class RadGatewayHook extends AbstractGatewayModuleHook {

    private static final LoggerEx log = LoggerEx.newBuilder().build("rad.gateway.RadGatewayHook");

    private GatewayContext gatewayContext;
    private PerspectiveContext perspectiveContext;
    private ComponentRegistry componentRegistry;
    private ComponentModelDelegateRegistry modelDelegateRegistry;

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        log.info("Setting up RadComponents module.");
    }

    @Override
    public void startup(LicenseState activationState) {
        log.info("Starting up RadGatewayHook!");

        this.perspectiveContext = PerspectiveContext.get(this.gatewayContext);
        this.componentRegistry = this.perspectiveContext.getComponentRegistry();
        this.modelDelegateRegistry = this.perspectiveContext.getComponentModelDelegateRegistry();

        // Project project = this.gatewayContext.getProjectManager().getProject("mes_ui").orElse(null);
        // String projectName = project.getName();
        // log.info("RadGatewayHook()::Project Name: " + projectName);

        // ResourcePath trackAndTracePath = new ResourcePath(
        //     new ResourceType("com.inductiveautomation.perspective", "views"), 
        //     "App/TrackAndTrace"
        // );

        // log.info("RadGatewayHook()::Resource Path: " + trackAndTracePath);

        // List<ProjectResource> trackAndTraceResources = project
        //     .browse(trackAndTracePath)
        //     .orElseGet(Collections::emptyList);
        
        // log.info("RadGatewayHook()::TRACK AND TRACE RESOURCES: " + trackAndTraceResources.size());
        
        // trackAndTraceResources.forEach(res -> {
        //     log.info("RadGatewayHook()::RES: " + res.getResourceName());
        // });

        // Get path to ignition installation using SDK
        String destDir = gatewayContext
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/App/";
        log.info("RadGatewayHook()::Data dir " + destDir);
        // Extract zip
        extractFiles(destDir,  "TrackAndTrace.zip");
        // re-register project files using SDK 
        

        if (this.componentRegistry != null) {
            log.info("Registering Rad components.");
            this.componentRegistry.registerComponent(Image.DESCRIPTOR);
            this.componentRegistry.registerComponent(TagCounter.DESCRIPTOR);
            this.componentRegistry.registerComponent(Messenger.DESCRIPTOR);
        } else {
            log.error("Reference to component registry not found, Rad Components will fail to function!");
        }

        if (this.modelDelegateRegistry != null) {
            log.info("Registering model delegates.");
            this.modelDelegateRegistry.register(Messenger.COMPONENT_ID, MessageComponentModelDelegate::new);
        } else {
            log.error("ModelDelegateRegistry was not found!");
        }

    }

    @Override
    public void shutdown() {
        log.info("Shutting down RadComponent module and removing registered components.");
        if (this.componentRegistry != null) {
            this.componentRegistry.removeComponent(Image.COMPONENT_ID);
            this.componentRegistry.removeComponent(TagCounter.COMPONENT_ID);
            this.componentRegistry.removeComponent(Messenger.COMPONENT_ID);
        } else {
            log.warn("Component registry was null, could not unregister Rad Components.");
        }
        if (this.modelDelegateRegistry != null ) {
            this.modelDelegateRegistry.remove(Messenger.COMPONENT_ID);
        }

    }

    @Override
    public Optional<String> getMountedResourceFolder() {
        return Optional.of("mounted");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routeGroup) {
        // where you may choose to implement web server endpoints accessible via `host:port/system/data/
        DataEndpoints.mountRoutes(routeGroup);
    }

    // Lets us use the route http://<gateway>/res/radcomponents/*
    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of(RadComponents.URL_ALIAS);
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }

    // Extract files from zip to destDir overwriting existing files
    private void extractFiles(String destDir, String zip) {
        String zipPath = destDir + zip;
        byte[] buffer = new byte[1024];
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                log.info("RadGatewayHook()::Zip entry filename: " + fileName);
                File newFile = new File(destDir + File.separator + fileName);
                if(zipEntry.isDirectory()) {
                    // Create directory
                    new File(newFile.getAbsolutePath()).mkdirs();
                } else {
                    // Write file
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }

                // Close this entry
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
            // Close last entry
            zis.closeEntry();
            zis.close();
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
