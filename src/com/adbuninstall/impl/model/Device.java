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

package com.adbuninstall.impl.model;

/**
 * Model class for device representation.
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class Device {

    private String manufacturer;
    private String model;
    private String serialNumber;
    private String apiVersion;
    private String releaseVersion;
    private String state;

    /**
     * @return human-readable device description
     */
    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (manufacturer != null && !manufacturer.isEmpty() && !"unknown".equalsIgnoreCase(manufacturer)) {
            sb.append(Character.toUpperCase(manufacturer.charAt(0))).append(manufacturer.substring(1)).append(" ");
        }
        if (model != null && !model.isEmpty()) {
            sb.append(model).append(" ");
        }
        if (releaseVersion != null && !releaseVersion.isEmpty()) {
            sb.append("Android ").append(releaseVersion).append(" ");
        }
        if (apiVersion != null && !apiVersion.isEmpty()) {
            sb.append("(API ").append(apiVersion).append(")");
        }
        return sb.toString();
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        if ("device".equals(state)) {
            this.state = "online";
            return;
        }
        this.state = state;
    }
}
