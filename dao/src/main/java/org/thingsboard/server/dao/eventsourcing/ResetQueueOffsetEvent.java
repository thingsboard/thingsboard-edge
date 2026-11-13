// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

public class ResetQueueOffsetEvent {

    public static final ResetQueueOffsetEvent INSTANCE = new ResetQueueOffsetEvent();

    private ResetQueueOffsetEvent() {}
}
