/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.scheduler.SchedulerEventInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.UUID;


@Component
public class JpaSchedulerEventInfoDao extends JpaAbstractSearchTextDao<SchedulerEventInfoEntity, SchedulerEventInfo> implements SchedulerEventInfoDao {

    @Autowired
    SchedulerEventInfoRepository schedulerEventInfoRepository;

    @Override
    protected Class<SchedulerEventInfoEntity> getEntityClass() {
        return SchedulerEventInfoEntity.class;
    }

    @Override
    protected CrudRepository<SchedulerEventInfoEntity, UUID> getCrudRepository() {
        return schedulerEventInfoRepository;
    }

    @Override
    public SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(UUID tenantId, UUID schedulerEventId) {
        return DaoUtil.getData(schedulerEventInfoRepository.findSchedulerEventWithCustomerInfoById(
                schedulerEventId
        ));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findSchedulerEventInfoEntitiesByTenantId(
                        tenantId));
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsWithCustomerInfoByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findSchedulerEventsWithCustomerInfoByTenantId(
                        tenantId));
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndType(UUID tenantId, String type) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndType(
                        tenantId,
                        type));
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId));
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type));
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds) {
        return service.submit(() -> DaoUtil.convertDataList(schedulerEventInfoRepository.findSchedulerEventsByTenantIdAndIdIn(tenantId, schedulerEventIds)));
    }
}
