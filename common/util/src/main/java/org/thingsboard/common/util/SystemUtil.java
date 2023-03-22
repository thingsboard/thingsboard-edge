/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.common.util;

import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class SystemUtil {

    private static final HardwareAbstractionLayer HARDWARE;

    static {
        SystemInfo si = new SystemInfo();
        HARDWARE = si.getHardware();
    }

    public static Long getMemoryUsage() {
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            return memoryMXBean.getHeapMemoryUsage().getUsed();
        } catch (Exception e) {
            log.debug("Failed to get memory usage!!!", e);
        }
        return null;
    }

    public static Long getTotalMemory() {
        try {
            return HARDWARE.getMemory().getTotal();
        } catch (Exception e) {
            log.debug("Failed to get total memory!!!", e);
        }
        return null;
    }

    public static Long getFreeMemory() {
        try {
            return HARDWARE.getMemory().getAvailable();
        } catch (Exception e) {
            log.debug("Failed to get free memory!!!", e);
        }
        return null;
    }

    public static Double getCpuUsage() {
        try {
            return prepare(HARDWARE.getProcessor().getSystemLoadAverage());
        } catch (Exception e) {
            log.debug("Failed to get cpu usage!!!", e);
        }
        return null;
    }

    public static Double getTotalCpuUsage() {
        try {
            return prepare(HARDWARE.getProcessor().getSystemCpuLoad() * 100);
        } catch (Exception e) {
            log.debug("Failed to get total cpu usage!!!", e);
        }
        return null;
    }

    public static Long getFreeDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return store.getUsableSpace();
        } catch (Exception e) {
            log.debug("Failed to get free disc space!!!", e);
        }
        return null;
    }

    public static Long getTotalDiscSpace() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            return store.getTotalSpace();
        } catch (Exception e) {
            log.debug("Failed to get total disc space!!!", e);
        }
        return null;
    }

    private static Double prepare(Double d) {
        return (int) (d * 100) / 100.0;
    }
}
