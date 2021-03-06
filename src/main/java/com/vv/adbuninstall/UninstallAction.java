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

package com.vv.adbuninstall;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.gradle.project.model.AndroidModuleModel;
import com.android.tools.idea.run.AndroidRunState;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.vv.adbuninstall.presentation.DeviceChooserDialog;
import org.jetbrains.android.facet.AndroidFacet;

import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Main Action which is triggered on ADB Uninstall clicked.
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class UninstallAction extends AnAction {

    /**
     * used for EventLog messages
     */
    public static final String ADB_UNINSTALL_ID = "ADB Uninstall";
    public static final String NOTIFICATION_TITLE = ADB_UNINSTALL_ID;
    public static final int NOTIFICATION_EXPIRE_DELAY = 3000;
    private Project project;

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final AnActionEvent event) {
        project = event.getProject();
        final DeviceChooserDialog deviceChooser = new DeviceChooserDialog(false);
        AndroidDebugBridge.addDeviceChangeListener(new DeviceChangeListener(deviceChooser));
        final AndroidDebugBridge adb = AndroidSdkUtils.getDebugBridge(project);
        deviceChooser.addDevices(adb.getDevices());
        deviceChooser.show();
        if (deviceChooser.isOK()) {
            final List<IDevice> selectedDevices = deviceChooser.getSelectedDevices();
            if (selectedDevices.size() > 0) {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, ADB_UNINSTALL_ID) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        uninstallFromDevices(selectedDevices, progressIndicator);
                    }
                });
            }
        }
    }

    /**
     * Performs device shell <code>"pm uninstall"</code> command on the given device.
     *
     * @param devices           list of {@link IDevice} objects
     * @param progressIndicator {@link ProgressIndicator} for the process command executed in.
     */
    private void uninstallFromDevices(@NotNull final List<IDevice> devices, @NotNull final ProgressIndicator progressIndicator) {
        final Module runningModule = getModule();
        if (runningModule == null) {
            return;
        }
        final String packageName = AndroidModuleModel.get(runningModule).getApplicationId();
        for (final IDevice device : devices) {
            try {
                progressIndicator.setText("Uninstalling " + packageName + " from " + DeviceUtils.getDeviceDisplayName(device));
                uninstallFromDevice(device, packageName);
            } catch (UninstallException ex) {
                showNotification(ex.getMessage(), NotificationType.ERROR);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Performs device shell <code>"pm uninstall"</code> command for the given device.
     *
     * @param device      {@link IDevice} object
     * @param packageName name of the package which should be uninstalled
     * @throws UninstallException in case of errors
     */
    private void uninstallFromDevice(@NotNull final IDevice device, @NotNull final String packageName) throws UninstallException {
        try {
            device.uninstallPackage(packageName);
            showNotification("App [" + packageName + "] successfully uninstalled from " + DeviceUtils.getDeviceDisplayName(device), NotificationType.INFORMATION);
        } catch (Exception ex) {
            throw new UninstallException(ex.getMessage(), ex);
        }

    }

    /**
     * Returns the active module from the selected Run Configuration.
     *
     * @return {@link Module}
     */
    @Nullable
    private Module getModule() {
        final RunManager runManager = RunManager.getInstance(project);
        final RunnerAndConfigurationSettings configurationSettings = runManager.getSelectedConfiguration();
        if (configurationSettings == null) {
            showNotification("Run Configuration is not defined", NotificationType.ERROR);
            return null;
        }
        final ModuleBasedConfiguration selectedConfiguration = (ModuleBasedConfiguration) configurationSettings.getConfiguration();
        final Module module = selectedConfiguration.getConfigurationModule().getModule();
        if (module == null) {
            showNotification("Module is not specified for selected Run Configuration", NotificationType.ERROR);
        }

        return module;
    }

    /**
     * Used for user notifications.
     *
     * @param message notification's message
     * @param type    one of the {@link NotificationType}
     */
    private void showNotification(@NotNull final String message, @NotNull final NotificationType type) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final Notification notification = new Notification(ADB_UNINSTALL_ID, NOTIFICATION_TITLE, message, type);
                Notifications.Bus.notify(notification);
                new Timer(NOTIFICATION_EXPIRE_DELAY, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        notification.expire();
                    }
                }).start();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    private class DeviceChangeListener implements AndroidDebugBridge.IDeviceChangeListener {
        private DeviceChooserDialog deviceChooser;

        private DeviceChangeListener(@NotNull final DeviceChooserDialog deviceChooser) {
            this.deviceChooser = deviceChooser;
        }

        @Override
        public void deviceConnected(@NotNull final IDevice iDevice) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    deviceChooser.addDevice(iDevice);
                }
            });
        }

        @Override
        public void deviceDisconnected(@NotNull final IDevice iDevice) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    deviceChooser.removeDevice(iDevice);
                }
            });
        }

        @Override
        public void deviceChanged(@NotNull final IDevice iDevice, int i) {
        }
    }
}

