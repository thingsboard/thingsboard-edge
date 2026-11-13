// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.runner;

public interface CloudEventUplinkProcessingRunner {

    void init();
    void shutdown();
}
