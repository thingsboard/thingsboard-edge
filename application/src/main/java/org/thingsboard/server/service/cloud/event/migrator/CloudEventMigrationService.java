// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cloud.event.migrator;

public interface CloudEventMigrationService {

    boolean isMigrationRequired();
    void migrateUnprocessedEventToKafka();

}
