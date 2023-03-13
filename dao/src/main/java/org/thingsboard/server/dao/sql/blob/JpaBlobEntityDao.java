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
package org.thingsboard.server.dao.sql.blob;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.blob.BlobEntityDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.BlobEntityEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaBlobEntityDao extends JpaAbstractSearchTextDao<BlobEntityEntity, BlobEntity> implements BlobEntityDao {

    private final BlobEntityRepository blobEntityRepository;
    private final SqlPartitioningRepository partitioningRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${sql.blob_entities.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.blob_entities.enabled:false}")
    private boolean ttlEnabled;
    @Value("${sql.ttl.blob_entities.ttl:0}")
    private int ttlInSec;

    private static final String TABLE_NAME = ModelConstants.BLOB_ENTITY_COLUMN_FAMILY_NAME;

    @Override
    protected Class<BlobEntityEntity> getEntityClass() {
        return BlobEntityEntity.class;
    }

    @Override
    protected JpaRepository<BlobEntityEntity, UUID> getRepository() {
        return blobEntityRepository;
    }

    @Override
    public BlobEntity save(TenantId tenantId, BlobEntity blobEntity) {
        if (blobEntity.getId() == null) {
            UUID uuid = Uuids.timeBased();
            blobEntity.setId(new BlobEntityId(uuid));
            blobEntity.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, blobEntity.getCreatedTime(), getPartitionSizeInMs());
        return super.save(tenantId, blobEntity);
    }

    @Override
    public void cleanUpBlobEntities(long expTime) {
        partitioningRepository.dropPartitionsBefore(TABLE_NAME, expTime, getPartitionSizeInMs());
    }

    @Override
    public void migrateBlobEntities() {
        long startTime = ttlEnabled && (long) ttlInSec > 0 ?
                System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec) : 1480982400000L;

        long currentTime = System.currentTimeMillis();
        var partitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTime - startTime) / partitionStepInMs;

        if (numberOfPartitions > 1000) {
            String error = "Please adjust your " + TABLE_NAME + " partitioning configuration. Configuration with partition size " +
                    "of " + partitionSizeInHours + " hours and corresponding TTL will use " + numberOfPartitions + " " +
                    "(> 1000) partitions which is not recommended!";
            log.error(error);
            throw new RuntimeException(error);
        }

        while (startTime < currentTime) {
            var endTime = startTime + partitionStepInMs;
            log.info("Migrating blob entities for time period: {} - {}", startTime, endTime);
            jdbcTemplate.update("CALL migrate_blob_entities(?, ?, ?)", startTime, endTime, partitionStepInMs);
            startTime = endTime;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS old_blob_entity");
        log.info("Dropped old_blob_entity table");
        log.info("Blob entities migration finished");
    }

    private long getPartitionSizeInMs() {
        return TimeUnit.HOURS.toMillis(partitionSizeInHours);
    }

}
