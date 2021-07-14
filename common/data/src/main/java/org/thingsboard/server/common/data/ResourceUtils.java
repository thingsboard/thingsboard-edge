/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.common.data;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Slf4j
public class ResourceUtils {

    public static InputStream getInputStream(Object classLoaderSource, String filePath) {
        return getInputStream(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static InputStream getInputStream(ClassLoader classLoader, String filePath) {
        try {
            InputStream keyStoreInputStream;
            File keyStoreFile = new File(filePath);
            if (keyStoreFile.exists()) {
                log.info("Reading key store from file {}", filePath);
                keyStoreInputStream = new FileInputStream(keyStoreFile);
            } else {
                InputStream classPathStream = classLoader.getResourceAsStream(filePath);
                if (classPathStream != null) {
                    log.info("Reading key store from class path {}", filePath);
                    keyStoreInputStream = classPathStream;
                } else {
                    URI uri = Resources.getResource(filePath).toURI();
                    log.info("Reading key store from URI {}", filePath);
                    keyStoreInputStream = new FileInputStream(new File(uri));
                }
            }
            return keyStoreInputStream;
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }

    public static String getUri(Object classLoaderSource, String filePath) {
        return getUri(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static String getUri(ClassLoader classLoader, String filePath) {
        try {
            File keyStoreFile = new File(filePath);
            if (keyStoreFile.exists()) {
                log.info("Reading key store from file {}", filePath);
                return keyStoreFile.getAbsolutePath();
            } else {
                URL url = classLoader.getResource(filePath);
                return url.toURI().toString();
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }
}
