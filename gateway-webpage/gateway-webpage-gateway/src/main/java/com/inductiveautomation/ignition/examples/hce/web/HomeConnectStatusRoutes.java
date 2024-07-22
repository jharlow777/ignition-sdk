package com.inductiveautomation.ignition.examples.hce.web;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.WicketAccessControl;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;

import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HomeConnectStatusRoutes {

    private final LoggerEx log = LogUtil.getLogger(getClass().getSimpleName());
    private final RouteGroup routes;

    public HomeConnectStatusRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
    }

    public void mountRoutes() {

        routes.newRoute("/install")
            .handler((req, res) -> install(req, res, "Track And Trace"))
            .type(TYPE_JSON)
            .restrict(WicketAccessControl.STATUS_SECTION)
            .mount();

    }

    public JSONObject install(RequestContext requestContext, HttpServletResponse httpServletResponse, String features) throws JSONException {
        GatewayContext context = requestContext.getGatewayContext();
        JSONObject json = new JSONObject();
        json.put("selected-features", features);
        log.info("HomeConnectStatusRoutes()::installing " + features);

        // Get path to ignition installation using SDK
        String viewsDir = context
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/";
        log.info("HomeConnectStatusRoutes()::Views dir " + viewsDir);
        String mountedDir = "/mounted/project-resources/";

        log.info("CWD: " +  System.getProperty("user.dir"));
        // Extract zip
        extractFiles(viewsDir + "App/", "TrackAndTrace.zip");
        // extractFiles(mountedDir, "TrackAndTrace.zip");

        // Enable navigation option
        String navJsonPath = viewsDir + "GlobalComponents/Navigation/NavComponent/view.json";
        enableNavComponent(navJsonPath);

        // Re-register project files using SDK 
        triggerResourceScan(context);

        log.info("HomeConnectStatusRoutes()::install()::json response " + json);
        return json;
    }

     // Extract files from zip to destDir overwriting existing files
     private void extractFiles(String destDir, String zipFileName) {
         try (InputStream zipStream = getClass().getClassLoader().getResourceAsStream(zipFileName)) {
             if (zipStream == null) {
                 log.error("Zip file not found: " + zipFileName);
                 return;
             }
             try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                 ZipEntry zipEntry = zis.getNextEntry();
                 byte[] buffer = new byte[1024];

                 while (zipEntry != null) {
                     File newFile = new File(destDir, zipEntry.getName());

                     if (zipEntry.isDirectory()) {
                         if (!newFile.isDirectory() && !newFile.mkdirs()) {
                             throw new IOException("Failed to create directory " + newFile);
                         }
                     } else {
                         File parent = newFile.getParentFile();
                         if (!parent.isDirectory() && !parent.mkdirs()) {
                             throw new IOException("Failed to create directory " + parent);
                         }

                         try (FileOutputStream fos = new FileOutputStream(newFile)) {
                             int len;
                             while ((len = zis.read(buffer)) > 0) {
                                 fos.write(buffer, 0, len);
                             }
                         }
                     }
                     zis.closeEntry();
                     zipEntry = zis.getNextEntry();
                 }
             }
         } catch (IOException e) {
             log.error("Error extracting zip file: " + e.getMessage(), e);
         }
     }

    // Trigger Project Resource Scan to immediately update
    private void triggerResourceScan(GatewayContext context) {
        try {
            ProjectManager projectManager = context.getProjectManager();
            projectManager.requestScan();
            log.info("HomeConnectStatusRoutes()::Successfully triggered resource scan");
        } catch (Exception e) {
            log.error("HomeConnectStatusRoutes()::Failed to trigger resource scan: " + e.getMessage(), e);
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
                log.info("HomeConnectStatusRoutes()::Updated NavComponent JSONObject: " + json);  

                // Write back to file
                Path path = Paths.get(navJsonPath);
                byte[] strToBytes = JSONObject.valueToString(json).getBytes();
                Files.write(path, strToBytes);
                log.info("HomeConnectStatusRoutes()::Successfully enabled nav menu item for TrackAndTrace");
            } catch(Exception e) {
                log.error("HomeConnectStatusRoutes()::Failed to parse NavComponent's view.json with error: " + e.getMessage());
            }
        }
    }
}

