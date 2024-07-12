package com.gpa.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Arrays;
import java.util.Collections;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.project.Project;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.localdb.persistence.IRecordListener;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.project.ProjectManager;
import com.inductiveautomation.ignition.gateway.web.components.AbstractNamedTab;
import com.inductiveautomation.ignition.gateway.web.components.ConfigPanel;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.INamedTab;
import com.inductiveautomation.ignition.gateway.web.pages.BasicReactPanel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import com.inductiveautomation.ignition.gateway.web.pages.status.StatusCategories;
import com.inductiveautomation.perspective.gateway.api.PerspectiveContext;
import com.inductiveautomation.ignition.gateway.web.models.KeyValue;
import com.google.zxing.Writer;
import com.gpa.gateway.endpoint.DataEndpoints;
import com.gpa.gateway.records.HCSettingsRecord;
import com.gpa.gateway.web.Elev8InstallPage;
import com.gpa.gateway.web.HCSettingsPage;
import com.gpa.gateway.web.HCSettingsPageOrig;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.fakester.common.RadComponents;
import org.hsqldb.lib.DataOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.python.core.io.BufferedWriter;

public class GatewayHook extends AbstractGatewayModuleHook {

    private static final LoggerEx log = LoggerEx.newBuilder().build("gpa.gateway.GatewayHook");

    private GatewayContext gatewayContext;

     /**
     * This sets up the config panel
     */
    public static final ConfigCategory CONFIG_CATEGORY =
        new ConfigCategory("HomeConnect", "HomeConnect.nav.header", 700);

    @Override
    public List<ConfigCategory> getConfigCategories() {
        return Collections.singletonList(CONFIG_CATEGORY);
    }

    /**
     * An IConfigTab contains all the info necessary to create a link to your config page on the gateway nav menu.
     * In order to make sure the breadcrumb and navigation works properly, the 'name' field should line up
     * with the right-hand value returned from {@link ConfigPanel#getMenuLocation}. In this case name("homeconnect")
     * lines up with HCSettingsPage#getMenuLocation().getRight()
     */
    public static final IConfigTab HCE_CONFIG_ENTRY = DefaultConfigTab.builder()
            .category(CONFIG_CATEGORY)
            .name("homeconnect")
            .i18n("HomeConnect.nav.settings.title")
            .page(HCSettingsPage.class)
            .terms("home connect settings")
            .build();

    // private static final IConfigTab HCE_CONFIG_ENTRY = new DefaultConfigTab(
    //     CONFIG_CATEGORY,
    //     "homeconnect",
    //     "HomeConnect.nav.settings.title",
    //     HCSettingsPage.class
    //     ) {

    //     @Override
    //     public ConfigPanel getPanel(IConfigPage configPage) {
    //         // We've set  GatewayHook.getMountPathAlias() to return hce, so we need to use that alias here.
    //         return new ConfigPanel(panelId, "/res/hce/js/homeconnectstatus.js", "homeconnectstatus");
    //     }
    // };

    @Override
    public List<? extends IConfigTab> getConfigPanels() {
        return Collections.singletonList(
            HCE_CONFIG_ENTRY
        );
    }

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;

        log.debug("Beginning setup of Elev8 Install Module");

        // Register GatewayHook.properties by registering the GatewayHook.class with BundleUtils
        BundleUtil.get().addBundle("Elev8Install", getClass(), "Elev8Install");

        //Verify tables for persistent records if necessary
        verifySchema(context);

        // create records if needed
        maybeCreateHCSettings(context);

        // get the settings record and do something with it...
        HCSettingsRecord theOneRecord = context.getLocalPersistenceInterface().find(HCSettingsRecord.META, 0L);
        log.info("Hub name: " + theOneRecord.getHCHubName());
        log.info("IP address: " + theOneRecord.getHCIPAddress());

        // listen for updates to the settings record...
        HCSettingsRecord.META.addRecordListener(new IRecordListener<HCSettingsRecord>() {
            @Override
            public void recordUpdated(HCSettingsRecord hcSettingsRecord) {
                log.info("recordUpdated()");
            }

            @Override
            public void recordAdded(HCSettingsRecord hcSettingsRecord) {
                log.info("recordAdded()");
            }

            @Override
            public void recordDeleted(KeyValue keyValue) {
                log.info("recordDeleted()");
            }
        });

        log.debug("Setup Complete.");
    }

    private void verifySchema(GatewayContext context) {
        try {
            context.getSchemaUpdater().updatePersistentRecords(HCSettingsRecord.META);
        } catch (SQLException e) {
            log.error("Error verifying persistent record schemas for HomeConnect records.", e);
        }
    }

    public void maybeCreateHCSettings(GatewayContext context) {
        log.trace("Attempting to create HomeConnect Settings Record");
        try {
            HCSettingsRecord settingsRecord = context.getLocalPersistenceInterface().createNew(HCSettingsRecord.META);
            settingsRecord.setId(0L);
            settingsRecord.setHCIPAddress("192.168.1.99");
            settingsRecord.setHCHubName("HomeConnect Hub");
            settingsRecord.setHCPowerOutput(23);
            settingsRecord.setHCDeviceCount(15);
            settingsRecord.setBroadcastSSID(false);

            /*
			 * This doesn't override existing settings, only replaces it with these if we didn't
			 * exist already.
			 */
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
        } catch (Exception e) {
            log.error("Failed to establish HCSettings Record exists", e);
        }

        log.trace("HomeConnect Settings Record Established");
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
        //String mountPath = "res/elev8-component/gpa/";
        log.info("GatewayHook()::Views dir " + viewsDir);

        // Extract zip
        extractFiles(viewsDir + "App/",  "ModuleResources/TrackAndTrace.zip");
        //extractFiles(mountPath, "TrackAndTrace.zip");

        // Enable navigation option
        String navJsonPath = viewsDir + "GlobalComponents/Navigation/NavComponent/view.json";
        enableNavComponent(navJsonPath);

        // Re-register project files using SDK 
        triggerResourceScan();
    }

    @Override
    public void shutdown() {
        BundleUtil.get().removeBundle("Elev8Install");
    }

    // getMountPathAlias() allows us to use a shorter mount path. Use caution, because we don't want a conflict with
    // other modules by other authors.
    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of("hce");
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
