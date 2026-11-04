// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.cloud;

public interface CloudSynchronizationManager {

    ThreadLocal<Boolean> getSync();

    boolean isSync();
}
