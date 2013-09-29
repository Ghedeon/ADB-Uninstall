/*
 * Copyright (C) 2013 Vitali Vasilioglo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adbuninstall.impl;

import com.adbuninstall.impl.model.Device;
import com.adbuninstall.impl.presentation.DeviceChooserDialog;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Action which is triggered on ADB Uninstall clicked.
 *
 * @author Vitali Vasilioglo <vitali.vasilioglo@gmail.com>
 */
public class UninstallAction extends AnAction {

    /**
     * used for EventLog messages
     */
    public static final String ADB_UNINSTALLER_ID = "ADB Uninstall";
    public static final String NOTIFICATION_TITLE = ADB_UNINSTALLER_ID;
    public static final String PLATFORM_TOOLS_DIR = "platform-tools";
    private static final String MANUFACTURER = "ro.product.manufacturer";
    private static final String MODEL = "ro.product.model";
    private static final String RELEASE_VERSION = "ro.build.version.release";
    private static final String API_VERSION = "ro.build.version.sdk";
    private AnActionEvent event;
    private GeneralCommandLine adbCmd = new GeneralCommandLine();

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(AnActionEvent event) {
        this.event = event;
        String sdkPath;
        Sdk projectSdk = ProjectRootManager.getInstance(event.getProject()).getProjectSdk();
        if (projectSdk != null) {
            sdkPath = projectSdk.getHomePath();
        } else {
            showNotification("Project SDK is not defined", NotificationType.ERROR);
            return;
        }
        String toolPath = sdkPath + "/" + PLATFORM_TOOLS_DIR;
        adbCmd.setExePath(toolPath + "/adb");
        List devices = getAvailableDevices();
        DeviceChooserDialog deviceChooser = new DeviceChooserDialog(false);
        deviceChooser.setDeviceList(devices);
        deviceChooser.show();
        int exitCode = deviceChooser.getExitCode();
        if (exitCode == DialogWrapper.OK_EXIT_CODE) {
            try {
                List<Device> selectedDevices = deviceChooser.getSelectedDevices();
                if (selectedDevices.size() > 0) {
                    String packageName = parseAndroidXmlForPackageName();
                    for (Device device : selectedDevices) {
                        uninstallFromDevice(device, packageName);
                    }
                }
            } catch (Exception ex) {
                showNotification(ex.getMessage(), NotificationType.ERROR);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Performs <code>"adb uninstall"</code> command for given device
     *
     * @param device      {@link com.adbuninstall.impl.model.Device} object
     * @param packageName name of the package should be uninstalled
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private void uninstallFromDevice(Device device, String packageName) throws UninstallException {
        try {
            ParametersList params = adbCmd.getParametersList();
            params.clearAll();
            params.add("-s");
            params.add(device.getSerialNumber());
            params.add("uninstall");
            params.add(packageName);
            Process pr = adbCmd.createProcess();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                String message = "Uninstalling " + packageName + " from " + device.getName() + ": " + line;
                showNotification(message, NotificationType.INFORMATION);
            }
        } catch (Exception ex) {
            throw new UninstallException(ex.getMessage(), ex);
        }

    }

    /**
     * Parses AndroidManifest.xml for package name.
     *
     * @return project's package name
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private String parseAndroidXmlForPackageName() throws ParseException {
        RunManager runManager = RunManager.getInstance(event.getProject());
        String currentModuleFilePath;
        RunnerAndConfigurationSettings configurationSettings = runManager.getSelectedConfiguration();
        if (configurationSettings == null) {
            Messages.showErrorDialog(event.getProject(), "Run Configuration is not defined", NOTIFICATION_TITLE);
            throw new ParseException("Run Configuration is not defined");
        }
        ModuleBasedConfiguration selectedConfiguration = (ModuleBasedConfiguration) configurationSettings.getConfiguration();
        if (selectedConfiguration != null) {
            Module module = selectedConfiguration.getConfigurationModule().getModule();
            if (module != null) {
                currentModuleFilePath = module.getModuleFilePath();
            } else {
                Messages.showErrorDialog(event.getProject(), "Module is not specified for selected Run Configuration", NOTIFICATION_TITLE);
                throw new ParseException("Module is not specified for selected Run Configuration");
            }
        } else {
            Messages.showErrorDialog(event.getProject(), "Run Configuration is not defined", NOTIFICATION_TITLE);
            throw new ParseException("Run Configuration not found");
        }
        try {
            int index = currentModuleFilePath.lastIndexOf("/");
            String currentModulePath = ".";
            if (index != -1) {
                currentModulePath = currentModuleFilePath.substring(0, index);
            }
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader;
            try {
                // gradle project
                reader = factory.createXMLStreamReader(new FileReader(currentModulePath + "/src/main/AndroidManifest.xml"));
            } catch (FileNotFoundException ex) {
                // old style project
                reader = factory.createXMLStreamReader(new FileReader(currentModulePath + "/AndroidManifest.xml"));
            }

            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        if ("manifest".equals(reader.getLocalName())) {
                            return reader.getAttributeValue(null, "package");
                        }
                }
            }
        } catch (Exception ex) {
            throw new ParseException(ex.getMessage());
        }

        throw new ParseException("Package not found");
    }

    /**
     * Performs <code>"adb devices"</code> command
     *
     * @return List of available devices
     */
    private List<Device> getAvailableDevices() {
        List<Device> devices = new ArrayList<Device>();
        boolean isDeviceEntry = false;
        try {
            ParametersList params = adbCmd.getParametersList();
            params.clearAll();
            params.add("devices");
            Process pr = adbCmd.createProcess();
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                if (line.toLowerCase().contains("list of devices")) {
                    isDeviceEntry = true;
                    continue;
                }
                if (isDeviceEntry) {
                    System.out.println(line);
                    if (!line.isEmpty()) {
                        Device device = parseDevice(line);
                        fillDeviceInfo(device);
                        devices.add(device);
                    }
                }
            }
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationType.ERROR);
            ex.printStackTrace();
        }
        return devices;
    }

    /**
     * Obtains additional device's info
     *
     * @param device {@link Device}
     */
    private void fillDeviceInfo(Device device) {
        try {
            ParametersList params = adbCmd.getParametersList();
            params.clearAll();
            params.add("-s");
            params.add(device.getSerialNumber());
            params.add("shell");
            params.add("cat");
            params.add("/system/build.prop");
            Process pr = adbCmd.createProcess();

            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            Map<String, String> properties = new HashMap<String, String>();
            while ((line = input.readLine()) != null) {
                if (line.contains("=")) {
                    String[] split = line.split("=");
                    if (split.length == 2) {
                        properties.put(split[0], split[1]);
                    }
                }
            }
            device.setManufacturer(properties.get(MANUFACTURER));
            device.setModel(properties.get(MODEL));
            device.setReleaseVersion(properties.get(RELEASE_VERSION));
            device.setApiVersion(properties.get(API_VERSION));
        } catch (Exception ex) {
            showNotification(ex.getMessage(), NotificationType.ERROR);
            ex.printStackTrace();
        }
    }

    /**
     * Parses <code>"adb devices"</code> command output for device serial number and status
     *
     * @param line given line of output
     * @return {@Device} object
     */
    private Device parseDevice(String line) {
        Device device = new Device();
        String[] split = line.split("\t");
        device.setSerialNumber(split[0]);
        device.setState(split[1]);
        return device;
    }

    private void showNotification(@NotNull String message, @NotNull NotificationType type) {
        Notifications.Bus.notify(new Notification(ADB_UNINSTALLER_ID,
                NOTIFICATION_TITLE, message,
                type));
    }
}
