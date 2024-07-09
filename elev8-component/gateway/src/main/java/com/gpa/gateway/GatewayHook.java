package com.gpa.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.project.Project;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import com.inductiveautomation.perspective.gateway.api.PerspectiveContext;
import com.google.zxing.Writer;
import com.gpa.gateway.endpoint.DataEndpoints;

import org.apache.commons.io.IOUtils;
import org.fakester.common.RadComponents;
import org.hsqldb.lib.DataOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.python.core.io.BufferedWriter;

public class GatewayHook extends AbstractGatewayModuleHook {

    private static final LoggerEx log = LoggerEx.newBuilder().build("gpa.gateway.GatewayHook");

    private GatewayContext gatewayContext;

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
    }

    @Override
    public void startup(LicenseState activationState) {
        log.info("Starting up GatewayHook");

        // Get path to ignition installation using SDK
        String viewsDir = gatewayContext
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/";
        String mountPath = "mounted/";
        log.info("GatewayHook()::Views dir " + viewsDir);

        // Extract zip
        // extractFiles(destDir,  "App/ModuleResources/TrackAndTrace.zip");
        extractFiles(mountPath, "TrackAndTrace.zip");

        // Enable navigation option
        String navJsonPath = viewsDir + "GlobalComponents/Navigation/NavComponent/view.json";
        enableNavComponent(navJsonPath);

        // Re-register project files using SDK 
        triggerResourceScan();
    }

    @Override
    public void shutdown() {

    }

    // getMountPathAlias() allows us to use a shorter mount path. Use caution, because we don't want a conflict with
    // other modules by other authors.
    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of("gpa");
    }

    // Use this whenever you have mounted resources
    @Override
    public Optional<String> getMountedResourceFolder() {
        return Optional.of("mounted");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routeGroup) {
        // where you may choose to implement web server endpoints accessible via `host:port/system/data/
        DataEndpoints.mountRoutes(routeGroup);
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
                log.info("GatewayHook()::Zip entry filename: " + fileName);
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

    // Trigger Project Resource Scan to immediately update
    private void triggerResourceScan() {
        // DesignerContext designerContext = DesignerContext.get(this.gatewayContext);
        // (designerContext.frame as IgnitionDesigner).updateProject();
        try {
            ProjectManager projectManager = gatewayContext.getProjectManager();
            projectManager.requestScan();
            log.info("GatewayHook()::Successfully triggered resource scan");
        } catch (Exception e) {
            log.error("GatewayHook()::Failed to trigger resource scan: " + e.getMessage(), e);
        }
    }

    private void enableNavComponent(String navJsonPath) {
        File f = new File(navJsonPath);
        if (f.exists()){
            try {
                // Update value
                FileInputStream is = new FileInputStream(navJsonPath);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONObject json = new JSONObject(jsonTxt);    
                json
                    .getJSONObject("root")
                    .getJSONArray("children")
                    .getJSONObject(0) // Only one child
                    .getJSONObject("props")
                    .getJSONArray("items")
                    .getJSONObject(2) // TrackAndTrace
                    .put("visible", true);
                log.info("GatewayHook()::Updated NavComponent JSONObject: " + json);  

                // Write back to file
                Path path = Paths.get(navJsonPath);
                byte[] strToBytes = JSONObject.valueToString(json).getBytes();
                Files.write(path, strToBytes);
                log.info("GatewayHook()::Successfully enabled nav menu item for TrackAndTrace");
            } catch(Exception e) {
                log.error("GatewayHook()::Failed to parse NavComponent's view.json with error: " + e.getMessage());
            }
        }
    }
}
