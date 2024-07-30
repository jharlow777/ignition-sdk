package com.inductiveautomation.ignition.examples.hce.web;

import com.inductiveautomation.ignition.common.util.LogUtil;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.dataroutes.WicketAccessControl;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import com.inductiveautomation.ignition.gateway.datasource.SRConnection;
import com.inductiveautomation.ignition.gateway.datasource.DatasourceManager;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.gateway.datasource.Datasource;
import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;

import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class HomeConnectStatusRoutes {

    private final LoggerEx log = LogUtil.getLogger(getClass().getSimpleName());
    private final RouteGroup routes;
    private boolean routesMounted = false;

    public HomeConnectStatusRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
    }

    public void mountRoutes() {
        if (!routesMounted) {

            routes.newRoute("/install")
                .handler((req, res) -> {
                    return install(req, res, req.getParameter("params"));
                })
                .type(TYPE_JSON)
                .restrict(WicketAccessControl.STATUS_SECTION)
                .mount();

            routes.newRoute("/installFile")
                .handler((req, res) -> {
                    try {
                        InputStream fileStream = req.getRequest().getInputStream();
                        return installFile(req, res, fileStream);
                    } catch (IOException e) {
                        log.error("Error reading file stream: " + e.getMessage(), e);
                        return new JSONObject().put("file-install-status", "error").toString();
                    }
                })
                .type(TYPE_JSON)
                .method(HttpMethod.POST)
                .restrict(WicketAccessControl.STATUS_SECTION)
                .mount();

            routes.newRoute("/activeFeatures")
                .handler((req, res) -> {
                    return retrieveAvailableFeatures(req, res);
                })
                .type(TYPE_JSON)
                .restrict(WicketAccessControl.STATUS_SECTION)
                .mount();
            
            routes.newRoute("/resetFeatures")
                .handler((req, res) -> {
                    return resetInstallStatus(req);
                })
                .type(TYPE_JSON)
                .restrict(WicketAccessControl.STATUS_SECTION)
                .mount();
            
            routesMounted = true;
        }
    }

    // FOR TESTING: Reset all modules' install status
    public JSONObject resetInstallStatus(RequestContext requestContext) throws SQLException {
        SRConnection conn = null;
        try {
            GatewayContext context = requestContext.getGatewayContext();
            DatasourceManager datasourceManager = context.getDatasourceManager();
            conn = datasourceManager.getConnection("MSSQL_MES");
            conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 0");
        } catch(SQLException e) {
            log.info("HomeConnectStatusRoutes()::resetInstallStatus()::Failed to update due to SQLException: " + e.getMessage());
        } finally {
            conn.close();
        }
        return null;
    }

    public JSONObject retrieveAvailableFeatures(RequestContext requestContext, HttpServletResponse httpServletResponse) throws SQLException, JSONException {
        JSONObject json = new JSONObject();
        SRConnection conn = null;
        try {
            GatewayContext context = requestContext.getGatewayContext();
            DatasourceManager datasourceManager = context.getDatasourceManager();
            conn = datasourceManager.getConnection("MSSQL_MES");

            Dataset result = conn.runQuery("SELECT name FROM config.modules WHERE isActive = 1 AND isInstalled = 0");
            List<Object> features = result.getColumnAsList(0);
            json.put("availableFeatures", features);
        } catch(SQLException e) {
            log.error("HomeConnectStatusRoutes()::retrieveFeatures()::SQLException " + e.getMessage());
        } finally {
            conn.close();
        }
        return json;
    }

    public JSONObject install(RequestContext requestContext, HttpServletResponse httpServletResponse, String params) throws SQLException, JSONException {
        GatewayContext context = requestContext.getGatewayContext();
        JSONObject json = new JSONObject();
        json.put("selected-features", params);
        log.info("HomeConnectStatusRoutes()::selected-features " + params);

        // Get path to ignition installation using SDK
        String viewsDir = context
        .getSystemManager()
        .getDataDir()
        .getAbsolutePath()
        .replace('\\','/') 
        + "/projects/mes_ui/com.inductiveautomation.perspective/views/";

        List<String> features = Arrays.asList(params.split(","));
        DatasourceManager datasourceManager = context.getDatasourceManager();
        SRConnection conn = datasourceManager.getConnection("MSSQL_MES");
        features.forEach(feature -> {
            log.info("HomeConnectStatusRoutes()::installing " + feature);

            // Extract zip
            try {
                json.put("install-complete", 
                    extractFilesFromInputStream(viewsDir + "App/", getClass().getClassLoader().getResourceAsStream(feature + ".zip")));
            } catch(JSONException e) {
                log.error("HomeConnectStatusRoutes()::install()::Failed to update json response object for extractFilesFromInputStream due to error: " + e.getMessage());
            }

            // Enable navigation option
            int index = 0;
            switch(feature) {
                case "TrackAndTrace":
                    index = 2;
                    break;
                case "Quality":
                    index = 7;
                    break;
                case "DocumentManager":
                    index = 8;
                    break;
                default:
                    log.error("HomeConnectStatusRoutes()::install()::Undefined nav menu item: " + feature);
                    break;
            }

            try{
                json.put("nav-component-enabled", 
                    enableNavComponent(viewsDir + "GlobalComponents/Navigation/NavComponent/view.json", index));
            } catch(JSONException e) {
                log.error("HomeConnectStatusRoutes()::install()::Failed to update json response object for enableNavComponent due to error: " + e.getMessage());
            }

            try {
                int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + feature + "'");
                if(result == 1) {
                    log.info("HomeConnectStatusRoutes()::install()::Updated modules install status for " + feature);
                } else {
                    log.error("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature);
                }
            } catch(SQLException e) {
                log.info("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature + " due to SQLException: " + e.getMessage());
            }
        });
        conn.close();

        // Re-register project files
        json.put("resource-scan-complete", 
            triggerResourceScan(context));

        log.info("HomeConnectStatusRoutes()::install()::json response " + json);
        return json;
    }

    public JSONObject installFile(RequestContext requestContext, HttpServletResponse httpServletResponse, InputStream fileStream) throws SQLException, IOException, JSONException {
        GatewayContext context = requestContext.getGatewayContext();
        JSONObject json = new JSONObject();

        // Get path to ignition installation using SDK
        String viewsDir = context
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/App/";

        json.put("file-install-complete", 
            extractFilesFromInputStream(viewsDir, fileStream));

        // TODO: Determine feature name from file
        // enableNavComponent(file, 0);
        // DatasourceManager datasourceManager = context.getDatasourceManager();
        // SRConnection conn = datasourceManager.getConnection("MSSQL_MES");
        // try {
        //     int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + feature + "'");
        //     if(result == 1) {
        //         log.info("HomeConnectStatusRoutes()::install()::Updated modules install status for " + feature);
        //     } else {
        //         log.error("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature);
        //     }
        // } catch(SQLException e) {
        //     log.info("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature + " due to SQLException: " + e.getMessage());
        // } finally {
        //     conn.close();
        // }
        json.put("resource-scan-complete", 
            triggerResourceScan(context));

        log.info("HomeConnectStatusRoutes()::installFile()::json response " + json);
        return json;
    }

    // Extract files from zip to destDir overwriting existing files
    private boolean extractFilesFromInputStream(String destDir, InputStream fileStream) {
        boolean success = false;
        try (ZipInputStream zis = new ZipInputStream(fileStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];

            if(zipEntry == null) {
                log.info("HomeConnectStatusRoutes()::extractFilesFromUpload()::Source file invalid, zipEntry null");
            }
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
            success = true;
        } catch(IOException e) {
            log.error("HomeConnectStatusRoutes()::extractFilesFromUpload()::Failed to extract files due to error: " + e.getMessage());
        }
        return success;
    }

    // Trigger Project Resource Scan to immediately update
    private boolean triggerResourceScan(GatewayContext context) {
        boolean success = false;
        try {
            ProjectManager projectManager = context.getProjectManager();
            projectManager.requestScan();
            log.info("HomeConnectStatusRoutes()::Successfully triggered resource scan");
            success = true;
        } catch (Exception e) {
            log.error("HomeConnectStatusRoutes()::Failed to trigger resource scan: " + e.getMessage(), e);
        }
        return success;
    }

    // Enable nav menu item at index
    private boolean enableNavComponent(String navJsonPath, int index) {
        boolean success = false;
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
                    .getJSONObject(index)
                    .put("visible", true);

                // Write back to file
                Path path = Paths.get(navJsonPath);
                byte[] strToBytes = JSONObject.valueToString(json).getBytes();
                Files.write(path, strToBytes);
                log.info("HomeConnectStatusRoutes()::Successfully enabled nav menu item");
                success = true;
            } catch(Exception e) {
                log.error("HomeConnectStatusRoutes()::Failed to parse NavComponent's view.json with error: " + e.getMessage());
            }
        } else {
            log.error("HomeConnectStatusRoutes()::Failed to find nav menu file");
        }
        return success;
    }
}
