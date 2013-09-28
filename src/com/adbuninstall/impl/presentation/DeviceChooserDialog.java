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

package com.adbuninstall.impl.presentation;

import com.adbuninstall.impl.model.Device;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Device Chooser dialog which allows to select on of the attached devices or all of them.
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class DeviceChooserDialog extends DialogWrapper implements ItemListener {

    private JPanel contentPane;
    private JTable jTable;
    private JCheckBox allCheckbox;
    private List<Device> devices = new ArrayList<Device>();

    public DeviceChooserDialog(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
        _init();
    }

    public DeviceChooserDialog(@Nullable Project project) {
        super(project);
        _init();
    }

    public DeviceChooserDialog(boolean canBeParent) {
        super(canBeParent);
        _init();
    }

    public DeviceChooserDialog(@NotNull Component parent, boolean canBeParent) {
        super(parent, canBeParent);
        _init();
    }

    private void _init() {
        init();
        setModal(true);
        setTitle("Choose device");
        jTable.setRowHeight(22);
        allCheckbox.setEnabled(false);
    }

    public void setDeviceList(List<Device> devices) {
        this.devices.clear();
        if (devices != null) {
            this.devices.addAll(devices);
        }
        jTable.setModel(new DevicesTableModel(devices));
        if (devices.size() > 0) {
            jTable.setRowSelectionInterval(0, 0);
            allCheckbox.setEnabled(true);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        allCheckbox.addItemListener(this);
        return contentPane;
    }

    public List<Device> getSelectedDevices() {
        if (allCheckbox.isSelected()) {
            return devices;
        }
        List<Device> selectedDevices = new ArrayList<Device>();
        for (int row : jTable.getSelectedRows()) {
            selectedDevices.add(devices.get(row));
        }
        return selectedDevices;
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            jTable.setRowSelectionInterval(0, jTable.getRowCount() - 1);
        } else {
            jTable.setRowSelectionInterval(0, 0);
        }
    }

    private class DevicesTableModel extends AbstractTableModel {

        private final String[] cols = {"Device", "Serial Number", "State"};
        private List<Device> devices;

        private DevicesTableModel(List<Device> devices) {
            this.devices = devices;
        }

        @Override
        public int getRowCount() {
            return devices.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return devices.get(rowIndex).getName();
                case 1:
                    return devices.get(rowIndex).getSerialNumber();
                case 2:
                    return devices.get(rowIndex).getState();
            }
            //shouldn't happen
            return null;
        }
    }
}
