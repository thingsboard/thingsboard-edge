// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.sender;

public interface CloudEventUplinkInterrupter {

    void interruptPreviousSendUplinkMsgsTask();
}
