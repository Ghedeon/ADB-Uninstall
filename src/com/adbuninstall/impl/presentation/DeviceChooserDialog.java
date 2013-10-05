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

package com.adbuninstall.impl.presentation;

import com.adbuninstall.impl.DeviceUtils;
import com.android.ddmlib.IDevice;
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
 * @author Vitali Vasilioglo <vitali.vasilioglo@gmail.com>
 */
public class DeviceChooserDialog extends DialogWrapper implements ItemListener {

    private JPanel contentPane;
    private JTable jTable;
    private JCheckBox allCheckbox;
    private DevicesTableModel tableModel;

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
        allCheckbox.setEnabled(false);
        jTable.setRowHeight(22);
        tableModel = new DevicesTableModel();
        jTable.setModel(tableModel);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        allCheckbox.addItemListener(this);
        return contentPane;
    }

    public List<IDevice> getSelectedDevices() {
        if (allCheckbox.isSelected()) {
            return tableModel.devices;
        }
        List<IDevice> selectedDevices = new ArrayList<IDevice>();
        for (int row : jTable.getSelectedRows()) {
            selectedDevices.add(tableModel.devices.get(row));
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

    public void addDevice(@NotNull IDevice device) {
        tableModel.addDevice(device);
        updateSelectionIfNeeded();
    }

    public void removeDevice(@NotNull IDevice device) {
        tableModel.removeDevice(device);
        if (tableModel.devices.size() == 0) { // last device removed
            allCheckbox.setEnabled(false);
        }
    }

    public void addDevices(IDevice[] devices) {
        if (devices.length != 0) {
            tableModel.addDevices(devices);
            updateSelectionIfNeeded();
        }
    }

    private void updateSelectionIfNeeded() {
        if (jTable.getSelectedRow() == -1) {
            allCheckbox.setEnabled(true);
            jTable.setRowSelectionInterval(0, 0);
        }
    }

    private class DevicesTableModel extends AbstractTableModel {

        private final String[] cols = {"Device", "Serial Number", "State"};
        private List<IDevice> devices = new ArrayList<IDevice>();

        public void addDevice(@NotNull IDevice device) {
            devices.add(device);
            fireTableDataChanged();
        }

        public void removeDevice(@NotNull IDevice device) {
            devices.remove(device);
            fireTableDataChanged();
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
                    return DeviceUtils.getDeviceDisplayName(devices.get(rowIndex));
                case 1:
                    return devices.get(rowIndex).getSerialNumber();
                case 2:
                    return devices.get(rowIndex).getState();
            }
            //shouldn't happen
            return null;
        }

        public void addDevices(IDevice[] devices) {
            boolean needRefresh = false;
            for (IDevice device : devices) {
                if (!this.devices.contains(device)) {
                    this.devices.add(device);
                    needRefresh = true;
                }
            }
            if (needRefresh) {
                fireTableDataChanged();
            }
        }
    }

}
