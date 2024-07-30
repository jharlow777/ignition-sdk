// package com.inductiveautomation.ignition.examples.hce.web;

// import com.inductiveautomation.ignition.common.util.LogUtil;
// import com.inductiveautomation.ignition.common.util.LoggerEx;
// import com.inductiveautomation.ignition.gateway.dataroutes.RequestContext;
// import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
// import com.inductiveautomation.ignition.gateway.dataroutes.WicketAccessControl;
// import com.inductiveautomation.ignition.gateway.model.GatewayContext;
// import com.inductiveautomation.ignition.gateway.project.ProjectManager;
// import com.inductiveautomation.ignition.gateway.datasource.SRConnection;
// import com.inductiveautomation.ignition.gateway.datasource.DatasourceManager;
// import com.inductiveautomation.ignition.common.Dataset;
// import com.inductiveautomation.ignition.gateway.datasource.Datasource;
// import com.inductiveautomation.ignition.gateway.dataroutes.HttpMethod;

// import org.json.JSONException;
// import org.json.JSONObject;
// import org.apache.commons.io.IOUtils;

// import java.sql.SQLException;

// import javax.servlet.http.HttpServletResponse;

// import static com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup.TYPE_JSON;

// import java.io.File;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.io.FileInputStream;
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.nio.file.Paths;
// import java.util.List;
// import java.util.Arrays;
// import java.util.stream.Collectors;
// import java.util.stream.Stream;
// import java.util.zip.ZipEntry;
// import java.util.zip.ZipInputStream;
// import java.util.zip.ZipOutputStream;

// public class HomeConnectStatusRoutes {

//     private final LoggerEx log = LogUtil.getLogger(getClass().getSimpleName());
//     private final RouteGroup routes;
//     private boolean routesMounted = false;

//     public HomeConnectStatusRoutes(GatewayContext context, RouteGroup group) {
//         this.routes = group;
//     }

//     public void mountRoutes() {
//         if (!routesMounted) {
//             routes.newRoute("/install")
//                 .handler((req, res) -> install(req, res, req.getParameter("params")))
//                 .type(TYPE_JSON)
//                 .restrict(WicketAccessControl.STATUS_SECTION)
//                 .mount();

//             routes.newRoute("/installFile")
//                 .handler((req, res) -> {
//                     try {
//                         InputStream fileStream = req.getRequest().getInputStream();
//                         return installFile(req, res, fileStream);
//                     } catch (IOException e) {
//                         log.error("Error reading file stream: " + e.getMessage(), e);
//                         return new JSONObject().put("file-install-status", "error").toString();
//                     }
//                 })
//                 .type(TYPE_JSON)
//                 .method(HttpMethod.POST)
//                 .restrict(WicketAccessControl.STATUS_SECTION)
//                 .mount();

//             routes.newRoute("/activeFeatures")
//                 .handler((req, res) -> retrieveAvailableFeatures(req, res))
//                 .type(TYPE_JSON)
//                 .restrict(WicketAccessControl.STATUS_SECTION)
//                 .mount();

//             routes.newRoute("/resetFeatures")
//                 .handler((req, res) -> resetInstallStatus(req))
//                 .type(TYPE_JSON)
//                 .restrict(WicketAccessControl.STATUS_SECTION)
//                 .mount();

//             routesMounted = true;
//         }
//     }

//     // FOR TESTING: Reset all modules' install status
//     public JSONObject resetInstallStatus(RequestContext requestContext) throws SQLException {
//         SRConnection conn = null;
//         try {
//             GatewayContext context = requestContext.getGatewayContext();
//             DatasourceManager datasourceManager = context.getDatasourceManager();
//             conn = datasourceManager.getConnection("MSSQL_MES");
//             conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 0");
//         } catch(SQLException e) {
//             log.info("HomeConnectStatusRoutes()::resetInstallStatus()::Failed to update due to SQLException: " + e.getMessage());
//         } finally {
//             conn.close();
//         }
//         return null;
//     }

//     public JSONObject retrieveAvailableFeatures(RequestContext requestContext, HttpServletResponse httpServletResponse) throws SQLException, JSONException {
//         JSONObject json = new JSONObject();
//         SRConnection conn = null;
//         try {
//             GatewayContext context = requestContext.getGatewayContext();
//             DatasourceManager datasourceManager = context.getDatasourceManager();
//             conn = datasourceManager.getConnection("MSSQL_MES");

//             Dataset result = conn.runQuery("SELECT name FROM config.modules WHERE isActive = 1 AND isInstalled = 0");
//             List<Object> features = result.getColumnAsList(0);
//             json.put("availableFeatures", features);
//         } catch(SQLException e) {
//             log.error("HomeConnectStatusRoutes()::retrieveFeatures()::SQLException " + e.getMessage());
//         } finally {
//             conn.close();
//         }
//         return json;
//     }

//     public JSONObject install(RequestContext requestContext, HttpServletResponse httpServletResponse, String params) throws SQLException, JSONException {
//         GatewayContext context = requestContext.getGatewayContext();
//         JSONObject json = new JSONObject();
//         json.put("selected-features", params);
//         log.info("HomeConnectStatusRoutes()::selected-features " + params);

//         // Get path to ignition installation using SDK
//         String viewsDir = context
//         .getSystemManager()
//         .getDataDir()
//         .getAbsolutePath()
//         .replace('\\','/') 
//         + "/projects/mes_ui/com.inductiveautomation.perspective/views/";

//         List<String> features = Arrays.asList(params.split(","));
//         DatasourceManager datasourceManager = context.getDatasourceManager();
//         SRConnection conn = datasourceManager.getConnection("MSSQL_MES");
//         features.forEach(feature -> {
//             log.info("HomeConnectStatusRoutes()::installing " + feature);

//             // Extract zip
//             extractFilesFromResources(viewsDir + "App/", feature + ".zip");

//             // Enable navigation option
//             int index = 0;
//             switch(feature) {
//                 case "TrackAndTrace":
//                     index = 2;
//                     break;
//                 case "Quality":
//                     index = 7;
//                     break;
//                 case "DocumentManager":
//                     index = 8;
//                     break;
//                 default:
//                     log.error("HomeConnectStatusRoutes()::install()::Undefined nav menu item: " + feature);
//                     break;
//             }
//             enableNavComponent(viewsDir + "GlobalComponents/Navigation/NavComponent/view.json", index);

//             try {
//                 int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + feature + "'");
//                 if(result == 1) {
//                     log.info("HomeConnectStatusRoutes()::install()::Updated modules install status for " + feature);
//                 } else {
//                     log.error("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature);
//                 }
//             } catch(SQLException e) {
//                 log.info("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature + " due to SQLException: " + e.getMessage());
//             }
//         });
//         conn.close();

//         // Re-register project files
//         triggerResourceScan(context);

//         log.info("HomeConnectStatusRoutes()::install()::json response " + json);
//         return json;
//     }

//     public JSONObject installFile(RequestContext requestContext, HttpServletResponse httpServletResponse, InputStream fileStream) throws SQLException, IOException, JSONException {
//         log.info("Entering method HomeConnectStatusRoutes.installFile");
//         GatewayContext context = requestContext.getGatewayContext();
//         JSONObject json = new JSONObject();
    
//         // Get path to ignition installation using SDK
//         String viewsDir = context
//             .getSystemManager()
//             .getDataDir()
//             .getAbsolutePath()
//             .replace('\\', '/') 
//             + "/projects/mes_ui/com.inductiveautomation.perspective/views/";
    
//         File tempFile;
//         try {
//             tempFile = File.createTempFile("upload-", ".zip");
//             tempFile.deleteOnExit();
//             try (ZipInputStream zis = new ZipInputStream(fileStream);
//                  FileOutputStream fos = new FileOutputStream(viewsDir + "App/" + tempFile.getName());
//                  ZipOutputStream zos = new ZipOutputStream(fos)) {
    
//                 ZipEntry entry;
//                 // Read each entry from the input ZIP stream
//                 while ((entry = zis.getNextEntry()) != null) {
//                     log.info("Zip entry: " + entry.getName());
//                     // Create a new entry in the output ZIP stream
//                     zos.putNextEntry(new ZipEntry(entry.getName()));
                    
//                     // Buffer for reading/writing data
//                     byte[] buffer = new byte[1024];
//                     int len;
//                     // Copy the contents of the current entry to the new ZIP file
//                     while ((len = zis.read(buffer)) > 0) {
//                         zos.write(buffer, 0, len);
//                     }
                    
//                     // Close the current entry in the output ZIP stream
//                     zos.closeEntry();
                    
//                     // Close the current entry in the input ZIP stream
//                     zis.closeEntry();
//                 }
                
//                 log.info("ZIP file recreated successfully.");
//             }
//             // Add additional file processing logic if needed
    
//         } catch (IOException e) {
//             log.error("HomeConnectStatusRoutes()::installFile()::Error handling uploaded file: " + e.getMessage(), e);
//             json.put("file-install-status", "error");
//         }
    
//         json.put("file-install-status", "success");
//         log.info("HomeConnectStatusRoutes()::installFile()::json response " + json);
//         return json;
//     }

//     // public JSONObject installFile(RequestContext requestContext, HttpServletResponse httpServletResponse, InputStream fileStream) throws SQLException, IOException, JSONException {
//     //     log.info("Entering method HomeConnectStatusRoutes.installFile");
//     //     GatewayContext context = requestContext.getGatewayContext();
//     //     JSONObject json = new JSONObject();

//     //     // Get path to ignition installation using SDK
//     //     String viewsDir = context
//     //         .getSystemManager()
//     //         .getDataDir()
//     //         .getAbsolutePath()
//     //         .replace('\\','/') 
//     //         + "/projects/mes_ui/com.inductiveautomation.perspective/views/";

//     //     File tempFile;
//     //     try {
//     //         tempFile = File.createTempFile("upload-", ".zip");
//     //         tempFile.deleteOnExit();
//     //         try (ZipInputStream zis = new ZipInputStream(fileStream);
//     //             FileOutputStream fos = new FileOutputStream(viewsDir + "App/" + tempFile.getName());
//     //             ZipOutputStream zos = new ZipOutputStream(fos)) {

//     //             ZipEntry entry;
//     //             // Read each entry from the input ZIP stream
//     //             while ((entry = zis.getNextEntry()) != null) {
//     //                 log.info("Zip entry: " + entry.getName());
//     //                 // Create a new entry in the output ZIP stream
//     //                 zos.putNextEntry(new ZipEntry(entry.getName()));
                    
//     //                 // Buffer for reading/writing data
//     //                 byte[] buffer = new byte[1024];
//     //                 int len;
//     //                 // Copy the contents of the current entry to the new ZIP file
//     //                 while ((len = zis.read(buffer)) > 0) {
//     //                     zos.write(buffer, 0, len);
//     //                 }
                    
//     //                 // Close the current entry in the output ZIP stream
//     //                 zos.closeEntry();
                    
//     //                 // Close the current entry in the input ZIP stream
//     //                 zis.closeEntry();
//     //             }
                
//     //             log.info("ZIP file recreated successfully.");

//     //         } catch (IOException e) {
//     //             log.error("Error recreating ZIP file: " + e.getMessage());
//     //         }
//     //     } catch (IOException e) {
//     //         log.error("HomeConnectStatusRoutes()::installFile()::Error handling uploaded file: " + e.getMessage(), e);
//     //         json.put("file-install-status", "error");
//     //     }

//         // try {
//         //     tempFile = File.createTempFile("upload-", ".zip");
//         //     tempFile.deleteOnExit();

//         //     try(FileOutputStream out = new FileOutputStream(viewsDir + "App/" + tempFile.getName())) {
//         //         ZipOutputStream zout = new ZipOutputStream(out);

//         //         byte[] buffer = new byte[1024];
//         //         int bytesRead;
//         //         while ((bytesRead = fileStream.read(buffer)) != -1) {
//         //             zout.write(buffer, 0, bytesRead);
//         //         }

//         //         zout.flush();
//         //         zout.close();
//         //         out.flush();
//         //         out.close();
//         //     }
//         //     log.info("HomeConnectStatusRoutes()::installFile()::installing file " + tempFile.getAbsolutePath());

//             // Unzip at destination
//             // extractFiles(viewsDir, tempFile.getName());

//             // TODO: Determine feature name from file
//             // enableNavComponent(file, 0);
//             // DatasourceManager datasourceManager = context.getDatasourceManager();
//             // SRConnection conn = datasourceManager.getConnection("MSSQL_MES");
//             // try {
//             //     int result = conn.runUpdateQuery("UPDATE config.modules SET isInstalled = 1 WHERE name = '" + feature + "'");
//             //     if(result == 1) {
//             //         log.info("HomeConnectStatusRoutes()::install()::Updated modules install status for " + feature);
//             //     } else {
//             //         log.error("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature);
//             //     }
//             // } catch(SQLException e) {
//             //     log.info("HomeConnectStatusRoutes()::install()::Failed to update modules install status for " + feature + " due to SQLException: " + e.getMessage());
//             // } finally {
//             //     conn.close();
//             // }
//             // triggerResourceScan(context);

//         // } catch (IOException e) {
//         //     log.error("HomeConnectStatusRoutes()::installFile()::Error handling uploaded file: " + e.getMessage(), e);
//         //     json.put("file-install-status", "error");
//         // }

//     //     json.put("file-install-status", "success");
//     //     log.info("HomeConnectStatusRoutes()::installFile()::json response " + json);
//     //     return json;
//     // }

//     // Extract files from zip to destDir overwriting existing files
//     // NOTE: requires zipFileName to be Module name
//     private void extractFiles(String destDir, String zipFileName) {
//         File zipFile = new File(destDir + zipFileName);
//         try (InputStream zipStream = new FileInputStream(zipFile)) {
//             if (zipStream == null) {
//                 log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Zip file not found: " + zipFileName);
//             }
//             try (ZipInputStream zis = new ZipInputStream(zipStream)) {
//                 if (zis == null) {
//                     log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Zip file not found: " + zipFile.getName());
//                 } else {
//                     log.info("HomeConnectStatusRoutes()::extractFilesFromResources()::Zip file " + zipFile.getAbsolutePath() + " found");
//                 }
//                 ZipEntry zipEntry = zis.getNextEntry();
//                 if(zipEntry == null) {
//                     log.info("HomeConnectStatusRoutes()::extractFilesFromResources()::ZipEntry started as null");
//                 }
//                 byte[] buffer = new byte[1024];

//                 while (zipEntry != null) {
//                     File newFile = new File(destDir, zipEntry.getName());

//                     if (zipEntry.isDirectory()) {
//                         if (!newFile.isDirectory() && !newFile.mkdirs()) {
//                             throw new IOException("HomeConnectStatusRoutes()::extractFilesFromResources()::Failed to create directory " + newFile);
//                         }
//                     } else {
//                         File parent = newFile.getParentFile();
//                         if (!parent.isDirectory() && !parent.mkdirs()) {
//                             throw new IOException("HomeConnectStatusRoutes()::extractFilesFromResources()::Failed to create directory " + parent);
//                         }

//                         try (FileOutputStream fos = new FileOutputStream(newFile)) {
//                             int len;
//                             while ((len = zis.read(buffer)) > 0) {
//                                 fos.write(buffer, 0, len);
//                             }
//                         }
//                     }
//                     zis.closeEntry();
//                     zipEntry = zis.getNextEntry();
//                 }
//             }
//         } catch (IOException e) {
//             log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Error extracting zip file: " + e.getMessage(), e);

//         };
//     }

//     private void extractFilesFromResources(String destDir, String zipFileName) {
//         try (InputStream zipStream = getClass().getClassLoader().getResourceAsStream(zipFileName)) {
//             if (zipStream == null) {
//                 log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Zip file not found: " + zipFileName);
//             }
//             try (ZipInputStream zis = new ZipInputStream(zipStream)) {
//                 if (zis == null) {
//                     log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Zip file not found: " + zipFileName);
//                 }
//                 ZipEntry zipEntry = zis.getNextEntry();
//                 if(zipEntry == null) {
//                     log.info("HomeConnectStatusRoutes()::extractFilesFromResources()::ZipEntry started as null");
//                 }
//                 byte[] buffer = new byte[1024];

//                 while (zipEntry != null) {
//                     File newFile = new File(destDir, zipEntry.getName());

//                     if (zipEntry.isDirectory()) {
//                         if (!newFile.isDirectory() && !newFile.mkdirs()) {
//                             throw new IOException("HomeConnectStatusRoutes()::extractFilesFromResources()::Failed to create directory " + newFile);
//                         }
//                     } else {
//                         File parent = newFile.getParentFile();
//                         if (!parent.isDirectory() && !parent.mkdirs()) {
//                             throw new IOException("HomeConnectStatusRoutes()::extractFilesFromResources()::Failed to create directory " + parent);
//                         }

//                         try (FileOutputStream fos = new FileOutputStream(newFile)) {
//                             int len;
//                             while ((len = zis.read(buffer)) > 0) {
//                                 fos.write(buffer, 0, len);
//                             }
//                         }
//                     }
//                     zis.closeEntry();
//                     zipEntry = zis.getNextEntry();
//                 }
//             }
//         } catch (IOException e) {
//             log.error("HomeConnectStatusRoutes()::extractFilesFromResources()::Error extracting zip file: " + e.getMessage(), e);

//         };
//     }

//     // Trigger Project Resource Scan to immediately update
//     private void triggerResourceScan(GatewayContext context) {
//         try {
//             ProjectManager projectManager = context.getProjectManager();
//             projectManager.requestScan();
//             log.info("HomeConnectStatusRoutes()::Successfully triggered resource scan");
//         } catch (Exception e) {
//             log.error("HomeConnectStatusRoutes()::Failed to trigger resource scan: " + e.getMessage(), e);
//         }
//     }

//     // Enable nav menu item at index
//     private void enableNavComponent(String navJsonPath, int index) {
//         File f = new File(navJsonPath);
//         if (f.exists()){
//             try {
//                 // Update value
//                 FileInputStream is = new FileInputStream(navJsonPath);
//                 String jsonTxt = IOUtils.toString(is, "UTF-8");
//                 JSONObject json = new JSONObject(jsonTxt);    
//                 json
//                     .getJSONObject("root")
//                     .getJSONArray("children")
//                     .getJSONObject(0) // Only one child
//                     .getJSONObject("props")
//                     .getJSONArray("items")
//                     .getJSONObject(index)
//                     .put("visible", true);

//                 // Write back to file
//                 Path path = Paths.get(navJsonPath);
//                 byte[] strToBytes = JSONObject.valueToString(json).getBytes();
//                 Files.write(path, strToBytes);
//                 log.info("HomeConnectStatusRoutes()::Successfully enabled nav menu item");
//             } catch(Exception e) {
//                 log.error("HomeConnectStatusRoutes()::Failed to parse NavComponent's view.json with error: " + e.getMessage());
//             }
//         } else {
//             log.error("HomeConnectStatusRoutes()::Failed to find nav menu file");
//         }
//     }
// }

