// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.eventsourcing;

public class GrpcConnectionEstablishedEvent {

    public static final GrpcConnectionEstablishedEvent INSTANCE = new GrpcConnectionEstablishedEvent();

    private GrpcConnectionEstablishedEvent() {}
}
