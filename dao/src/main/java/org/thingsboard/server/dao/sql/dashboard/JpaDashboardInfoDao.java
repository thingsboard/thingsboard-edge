/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.sql.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Slf4j
@Component
public class JpaDashboardInfoDao extends JpaAbstractSearchTextDao<DashboardInfoEntity, DashboardInfo> implements DashboardInfoDao {

    @Autowired
    private DashboardInfoRepository dashboardInfoRepository;

    @Override
    protected Class getEntityClass() {
        return DashboardInfoEntity.class;
    }

    @Override
    protected CrudRepository getCrudRepository() {
        return dashboardInfoRepository;
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public ListenableFuture<List<DashboardInfo>> findDashboardsByIdsAsync(UUID tenantId, List<UUID> dashboardIds) {
        return service.submit(() -> DaoUtil.convertDataList(dashboardInfoRepository.findByIdIn(dashboardIds)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByEntityGroupId(
                        groupId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

    @Override
    public PageData<DashboardInfo> findDashboardsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByEntityGroupIds(
                        groupIds,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink, DashboardInfoEntity.dashboardColumnMap)));
    }

}
