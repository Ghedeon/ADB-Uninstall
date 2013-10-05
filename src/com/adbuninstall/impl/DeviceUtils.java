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

package com.adbuninstall.impl;

import com.android.ddmlib.IDevice;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for device related operations.
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class DeviceUtils {

    private static final String MANUFACTURER = "ro.product.manufacturer";
    private static final String MODEL = "ro.product.model";


    /**
     * Returns the device's human-readable name.
     *
     * @param device {@link IDevice} object
     * @return device name compound from the most important device properties.
     */
    @NotNull
    public static String getDeviceDisplayName(@NotNull IDevice device) {
        StringBuilder sb = new StringBuilder();
        try {
            String property;
            if (device.isEmulator()) {
                sb.append("Emulator ").append(device.getAvdName()).append(" ");
            } else {
                property = device.getPropertyCacheOrSync(MANUFACTURER);
                if (property != null && !property.isEmpty() && !"unknown".equalsIgnoreCase(property)) {
                    sb.append(Character.toUpperCase(property.charAt(0))).append(property.substring(1)).append(" ");
                }
                property = device.getPropertyCacheOrSync(MODEL);
                if (property != null && !property.isEmpty()) {
                    sb.append(property).append(" ");
                }
            }
            property = device.getPropertyCacheOrSync(IDevice.PROP_BUILD_VERSION);
            if (property != null && !property.isEmpty()) {
                sb.append("Android ").append(property).append(" ");
            }
            property = device.getPropertyCacheOrSync(IDevice.PROP_BUILD_API_LEVEL);
            if (property != null && !property.isEmpty()) {
                sb.append("(API ").append(property).append(")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}
