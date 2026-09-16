// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud;

public interface CloudEventMigrationService {

    boolean isMigrated();

    boolean isTsMigrated();

    void migrateUnprocessedEventToKafka();

}
