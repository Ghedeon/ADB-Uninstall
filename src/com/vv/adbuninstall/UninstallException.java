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

package com.vv.adbuninstall;

/**
 * Thrown in case of uninstalling errors,
 *
 * @author Ghedeon <asfalit@gmail.com>
 */
public class UninstallException extends Exception {

    public UninstallException() {
    }

    public UninstallException(String message) {
        super(message);
    }

    public UninstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public UninstallException(Throwable cause) {
        super(cause);
    }
}
