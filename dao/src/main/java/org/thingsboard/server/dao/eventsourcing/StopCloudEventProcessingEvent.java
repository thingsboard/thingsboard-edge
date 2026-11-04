// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

public class StopCloudEventProcessingEvent {

    public static final StopCloudEventProcessingEvent INSTANCE = new StopCloudEventProcessingEvent();

    private StopCloudEventProcessingEvent() {}
}
