package com.gpa.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.gpa.gateway.endpoint.DataEndpoints;
import org.fakester.common.RadComponents;

public class GatewayHook extends AbstractGatewayModuleHook {

    private static final LoggerEx log = LoggerEx.newBuilder().build("rad.gateway.GatewayHook");

    private GatewayContext gatewayContext;

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
    }

    @Override
    public void startup(LicenseState activationState) {
        log.info("Starting up GatewayHook!");

        // Get path to ignition installation using SDK
        String destDir = gatewayContext
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/App/";
        log.info("GatewayHook()::Data dir " + destDir);

        // Check for git availability to pull latest zip version if possible
        // Extract zip
        extractFiles(destDir,  "ModuleResources/TrackAndTrace.zip");
        // Re-register project files using SDK 
        
    }

    @Override
    public void shutdown() {

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
}
