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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

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
        final String METHOD_NAME = "mountRoutes()::";
        if (!routesMounted) {

            routes.newRoute("/install")
                .handler((req, res) -> {
                    return install(req, res, req.getParameter("params"));
                })
                .type(TYPE_JSON)
                .restrict(WicketAccessControl.STATUS_SECTION)
                .mount();

            routes.newRoute("/installFile/:filename")
                .handler((req, res) -> {
                    try {
                        String filename = req.getParameter("filename");
                        InputStream fileStream = req.getRequest().getInputStream();
                        return installFile(req, res, fileStream, filename);
                    } catch (IOException e) {
                        log.error(METHOD_NAME + "Error reading file stream: " + e.getMessage(), e);
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
        final String METHOD_NAME = "resetInstallStatus()::";
        SRConnection conn = null;
        try {
            GatewayContext context = requestContext.getGatewayContext();
            DatasourceManager datasourceManager = context.getDatasourceManager();
            conn = datasourceManager.getConnection("MSSQL_MES");
            conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 0");
        } catch(SQLException e) {
            log.info(METHOD_NAME + "Failed to update due to SQLException: " + e.getMessage());
        } finally {
            conn.close();
        }
        return null;
    }

    public JSONObject retrieveAvailableFeatures(RequestContext requestContext, HttpServletResponse httpServletResponse) throws SQLException, JSONException {
        final String METHOD_NAME = "retrieveAvailableFeatures()::";
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
            log.error(METHOD_NAME + "SQLException " + e.getMessage());
        } finally {
            conn.close();
        }
        return json;
    }

    public JSONObject install(RequestContext requestContext, HttpServletResponse httpServletResponse, String params) throws SQLException, JSONException {
        final String METHOD_NAME = "install()::";
        GatewayContext context = requestContext.getGatewayContext();
        JSONObject json = new JSONObject();
        json.put("selected-features", params);
        log.info(METHOD_NAME + "selected-features " + params);

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
            log.info(METHOD_NAME + "installing " + feature);

            // Update to latest version if git is available
            // TODO: determine target repo
            // gitPull(feature);

            // Extract zip
            try {
                json.put("install-complete", 
                    extractFilesFromInputStream(viewsDir + "App/", getClass().getClassLoader().getResourceAsStream("mounted/project-resources/" + feature + ".zip")));
            } catch(JSONException e) {
                log.error(METHOD_NAME + "Failed to update json response object for extractFilesFromInputStream due to error: " + e.getMessage());
            }

            // Enable navigation option
            try {
                json.put("nav-component-enabled", 
                    enableNavComponent(viewsDir + "GlobalComponents/Navigation/NavComponent/view.json", feature));
            } catch(JSONException e) {
                log.error(METHOD_NAME + "Failed to update json response object for enableNavComponent due to error: " + e.getMessage());
            }

            try {
                json.put("db-status-update", false);
            } catch(JSONException je) {
                log.error(METHOD_NAME + "Failed to update json response object for dbStatusUpdate due to error: " + je.getMessage());
            }

            try {
                int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + feature + "'");
                if(result == 1) {
                    log.info(METHOD_NAME + "Updated modules install status for " + feature);
                    try {
                        json.put("db-status-update", true);
                    } catch(JSONException e) {
                        log.error(METHOD_NAME + "Failed to update json response object for dbStatusUpdate due to error: " + e.getMessage());
                    }
                } else {
                    log.error(METHOD_NAME + "Failed to update modules install status for " + feature);
                }
            } catch(SQLException e) {
                log.info(METHOD_NAME + "Failed to update modules install status for " + feature + " due to SQLException: " + e.getMessage());
            }
        });
        conn.close();

        // Re-register project files
        json.put("resource-scan-complete", 
            triggerResourceScan(context));

        log.info(METHOD_NAME + "json response " + json);
        return json;
    }

    public JSONObject installFile(RequestContext requestContext, HttpServletResponse httpServletResponse, InputStream fileStream, String fileName) throws SQLException, IOException, JSONException {
        final String METHOD_NAME = "installFile()::";
        GatewayContext context = requestContext.getGatewayContext();
        JSONObject json = new JSONObject();

        // Strip extension from fileName
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            fileName = fileName.substring(0, lastDotIndex);
        }

        // Get path to ignition installation using SDK
        String viewsDir = context
            .getSystemManager()
            .getDataDir()
            .getAbsolutePath()
            .replace('\\','/') 
            + "/projects/mes_ui/com.inductiveautomation.perspective/views/";

        json.put("file-install-complete", 
            extractFilesFromInputStream(viewsDir + "App/", fileStream));

        try {
            json.put("nav-component-enabled", 
                enableNavComponent(viewsDir + "GlobalComponents/Navigation/NavComponent/view.json", fileName));
        } catch (JSONException e) {
            log.error(METHOD_NAME + "Failed to update json response object for enableNavComponent due to error: " + e.getMessage());
        }
        DatasourceManager datasourceManager = context.getDatasourceManager();
        SRConnection conn = datasourceManager.getConnection("MSSQL_MES");
        json.put("db-status-update", false);
        try {
            int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + fileName + "'");
            if(result == 1) {
                log.info(METHOD_NAME + "Updated modules install status for " + fileName);
                json.put("db-status-update", true);
            } else {
                log.error(METHOD_NAME + "Failed to update modules install status for " + fileName);
            }
        } catch(SQLException e) {
            log.info(METHOD_NAME + "Failed to update modules install status for " + fileName + " due to SQLException: " + e.getMessage());
        } finally {
            conn.close();
        }
        json.put("resource-scan-complete", 
            triggerResourceScan(context));

        log.info(METHOD_NAME + "json response " + json);
        return json;
    }

    // Extract files from zip to destDir overwriting existing files
    private boolean extractFilesFromInputStream(String destDir, InputStream fileStream) {
        final String METHOD_NAME = "extractFilesFromInputStream()::";
        if(fileStream == null) {
            log.error(METHOD_NAME + "InputStream fileStream is null");
        }
        boolean success = false;
        try (ZipInputStream zis = new ZipInputStream(fileStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[1024];

            if(zipEntry == null) {
                log.info(METHOD_NAME + "Source file invalid, zipEntry null");
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
            log.info(METHOD_NAME + "Successfully extracted and installed source files");
        } catch(IOException e) {
            log.error(METHOD_NAME + "Failed to extract files due to error: " + e.getMessage());
        }
        return success;
    }

    // Trigger Project Resource Scan to immediately update
    private boolean triggerResourceScan(GatewayContext context) {
        final String METHOD_NAME = "triggerResourceScan()::";
        boolean success = false;
        try {
            ProjectManager projectManager = context.getProjectManager();
            projectManager.requestScan();
            log.info(METHOD_NAME + "Successfully triggered resource scan");
            success = true;
        } catch (Exception e) {
            log.error(METHOD_NAME + "Failed to trigger resource scan: " + e.getMessage(), e);
        }
        return success;
    }

    // Enable nav menu item for feature
    private boolean enableNavComponent(String navJsonPath, String feature) {
        final String METHOD_NAME = "enableNavComponent()::";
        boolean success = false;

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
                log.error(METHOD_NAME + "Undefined nav menu item: " + feature);
                return success;
        }

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
                log.info(METHOD_NAME + "Successfully enabled nav menu item");
                success = true;
            } catch(Exception e) {
                log.error(METHOD_NAME + "Failed to parse NavComponent's view.json with error: " + e.getMessage());
            }
        } else {
            log.error(METHOD_NAME + "Failed to find nav menu file");
        }
        return success;
    }

    private boolean gitPull(String feature) {
        final String METHOD_NAME = "gitPull()::";
        boolean success = false;
        String repoUrl = "https://github.com/your/repo.git";
        String branchName = "main";
        String filePathInRepo = "path/to/your/file.txt";
        String localRepoDir = "path/to/local/repo";
        String resourcesDir = "path/to/resources/dir";

        try {
            // Clone the repository if it doesn't exist locally
            File repoDir = new File(localRepoDir);
            if (!repoDir.exists()) {
                log.info(METHOD_NAME + "Cloning repository");
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .call();
            } else {
                log.info(METHOD_NAME + "Repository already cloned. Pulling latest changes");
                try (Git git = Git.open(repoDir)) {
                    git.pull().call();
                }
            }

            // Get the latest version of the file from the repository
            File fileInRepo = new File(repoDir, filePathInRepo);
            File targetFile = new File(resourcesDir, "file.txt");

            if (!fileInRepo.exists()) {
                throw new IOException("File does not exist in the repository.");
            }

            // Copy the file from the repository to the resources directory
            Files.copy(fileInRepo.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info(METHOD_NAME + "Git File updated successfully.");
            success = true;
        } catch (IOException | GitAPIException e) {
            log.error(METHOD_NAME + "Failed to update file from Git due to error: " + e.getMessage());
        }
        return success;
    }
}
