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
import java.nio.file.Paths;
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

    public HomeConnectStatusRoutes(GatewayContext context, RouteGroup group) {
        this.routes = group;
    }

    public void mountRoutes() {
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
            extractFilesFromResources(viewsDir + "App/", feature + ".zip");

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
            enableNavComponent(viewsDir + "GlobalComponents/Navigation/NavComponent/view.json", index);

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
        triggerResourceScan(context);

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
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/";

        // extractFiles(file);
        // try (ZipInputStream zis = new ZipInputStream(fileStream)) {
        //     if (zis == null) {
        //         log.error("HomeConnectStatusRoutes()::extractFiles()::fileStream failed to convert to zipinputstream");
        //     } else {
        //         log.info("Zis valid");
        //     }
        //     ZipEntry zipEntry = zis.getNextEntry();

        //     // Iterate over each entry in the ZIP file
        //     while (zipEntry != null) {
        //         File newFile = new File(viewsDir + "App/", zipEntry.getName());
        //         log.info("newFile from zipEntry: " + newFile.getAbsolutePath());

        //         // Ensure parent directories exist
        //         if (zipEntry.isDirectory()) {
        //             log.info("Zip entry is directory: " + zipEntry.getName());
        //             if (!newFile.isDirectory() && !newFile.mkdirs()) {
        //                 throw new IOException("Failed to create directory " + newFile);
        //             }
        //         } else {
        //             log.info("Zip entry is file: " + zipEntry.getName());
        //             // Create parent directories if they don't exist
        //             File parentDir = newFile.getParentFile();
        //             if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
        //                 throw new IOException("Failed to create directory " + parentDir);
        //             }

        //             // Write file contents
        //             try (FileOutputStream fos = new FileOutputStream(newFile)) {
        //                 byte[] buffer = new byte[1024];
        //                 int len;
        //                 while ((len = zis.read(buffer)) > 0) {
        //                     fos.write(buffer, 0, len);
        //                 }
        //             }
        //             log.info("File contents successfully written");
        //         }
        //         zis.closeEntry();
        //         zipEntry = zis.getNextEntry();
        //     }
        // } catch (Exception e) {
        //     log.error("HomeConnectStatusRoutes()::installFile()::Error handling uploaded file: " + e.getMessage(), e);
        //     json.put("file-install-status", "error");
        //     return json;
        // }
        File tempFile;
        try {
            tempFile = File.createTempFile("upload-", ".txt");
            tempFile.deleteOnExit();

            try(FileOutputStream out = new FileOutputStream(viewsDir + "App/" + tempFile.getName())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
            }
            log.info("HomeConnectStatusRoutes()::installFile()::installing file " + tempFile.getAbsolutePath());

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
            // triggerResourceScan(context);

        } catch (IOException e) {
            log.error("HomeConnectStatusRoutes()::installFile()::Error handling uploaded file: " + e.getMessage(), e);
            json.put("file-install-status", "error");
        }

        json.put("file-install-status", "success");
        log.info("HomeConnectStatusRoutes()::installFile()::json response " + json);
        return json;
    }

    // Extract files from zip to destDir overwriting existing files
    // NOTE: requires zipFileName to be Module name
    private void extractFiles(String destDir, File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            if (zis == null) {
                log.error("HomeConnectStatusRoutes()::extractFiles()::Zip file not found: " + zipFile.getName());
            } else {
                log.info("Zip file " + zipFile.getAbsolutePath() + " found");
            }
            // try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry zipEntry = zis.getNextEntry();
                byte[] buffer = new byte[1024];
                if(zipEntry == null) {
                    log.info("ZipEntry started as null");
                }
                while (zipEntry != null) {
                    log.info("Unzipping file: " + zipEntry.getName() + " at " + destDir);
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
            // } catch(Exception e) {
            //     log.error("HomeConnectStatusRoutes()::extractFiles()::Failed to open ZipInputStream due to error: " + e.getMessage());
            // }
        } catch (IOException e) {
            log.error("HomeConnectStatusRoutes()::extractFiles()::Error extracting zip file due to: " + e.getMessage(), e);

        };
    }

    private void extractFilesFromResources(String destDir, String zipFileName) {
        try (InputStream zipStream = getClass().getClassLoader().getResourceAsStream(zipFileName)) {
            if (zipStream == null) {
                log.error("Zip file not found: " + zipFileName);
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

        };
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

    // Enable nav menu item at index
    private void enableNavComponent(String navJsonPath, int index) {
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
            } catch(Exception e) {
                log.error("HomeConnectStatusRoutes()::Failed to parse NavComponent's view.json with error: " + e.getMessage());
            }
        } else {
            log.error("HomeConnectStatusRoutes()::Failed to find nav menu file");
        }
    }
}

