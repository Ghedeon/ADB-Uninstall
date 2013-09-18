/*
 * Copyright (C) Ghedeon
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

package com.uninstall.impl;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.uninstall.impl.model.Device;
import com.uninstall.impl.presentation.DeviceChooserDialog;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main Action which is triggered on ADB Uninstall clicked.
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class UninstallAction extends AnAction {

    /**
     * used for EventLog messages
     */
    public static final String ADB_UNINSTALLER_ID = "ADB Uninstall";

    private static final String MANUFACTURER       = "ro.product.manufacturer";
    private static final String MODEL              = "ro.product.model";
    private static final String RELEASE_VERSION    = "ro.build.version.release";
    private static final String API_VERSION        = "ro.build.version.sdk";
    public static final  String PLATFORM_TOOLS_DIR = "platform-tools";
    private AnActionEvent event;
    private String        toolPath;

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(AnActionEvent event) {
        this.event = event;
        String sdkPath = ProjectRootManager.getInstance(event.getProject()).getProjectSdk().getHomePath();
        toolPath = sdkPath + File.separator + PLATFORM_TOOLS_DIR;
        List<Device> devices = getAvailableDevices();
        DeviceChooserDialog deviceChooser = new DeviceChooserDialog(false);
        deviceChooser.setDeviceList(devices);
        deviceChooser.show();
        int exitCode = deviceChooser.getExitCode();
        if (exitCode == DialogWrapper.OK_EXIT_CODE) {
            List<Device> selectedDevices = deviceChooser.getSelectedDevices();
            for (Device device : selectedDevices) {
                try {
                    uninstallFromDevice(device);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Performs <code>"adb uninstall"</code> command for given device
     *
     * @param device {@link Device} object
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private void uninstallFromDevice(Device device) throws XMLStreamException, FileNotFoundException {
        String packageName = parseAndroidXmlForPackageName();
        //TODO: should switch to GeneralCommandLine here
        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt
                    .exec(toolPath + File.separator + "adb -s " + device.getSerialNumber() + " uninstall "
                            + packageName);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                Notifications.Bus.notify(new Notification(ADB_UNINSTALLER_ID,
                        "Uninstalling " + packageName + " from " + device.getName(), line,
                        NotificationType.INFORMATION));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses AndroidManifest.xml for package name.
     *
     * @return project's package name
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private String parseAndroidXmlForPackageName() throws XMLStreamException, FileNotFoundException {
        String projectFilePath = getEventProject(event).getBasePath();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory
                .createXMLStreamReader(new FileReader(projectFilePath + File.separator + "AndroidManifest.xml"));
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    if ("manifest".equals(reader.getLocalName())) {
                        return reader.getAttributeValue(null, "package");
                    }
            }
        }
        return null;
    }

    /**
     * Performs <code>"adb devices"</code> command
     *
     * @return List of available devices
     */
    private List<Device> getAvailableDevices() {
        List<Device> devices = new ArrayList<Device>();
        //TODO: should switch to GeneralCommandLine here
        Runtime rt = Runtime.getRuntime();
        boolean isDeviceEntry = false;
        try {
            Process pr = rt.exec(toolPath + File.separator + "adb devices");
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
            int exitVal = pr.waitFor();
            System.out.println("Exited with error code " + exitVal);
        }
        catch (Exception ex) {
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
        //TODO: should switch to GeneralCommandLine here
        Runtime rt = Runtime.getRuntime();
        try {
            Process pr = rt.exec(toolPath + File.separator + "adb -s " + device.getSerialNumber()
                    + " shell cat /system/build.prop");
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
        }
        catch (Exception ex) {
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

}
