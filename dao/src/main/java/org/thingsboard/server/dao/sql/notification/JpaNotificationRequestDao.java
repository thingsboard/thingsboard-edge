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
package org.thingsboard.server.dao.sql.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;
import org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.getId;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationRequestDao extends JpaAbstractDao<NotificationRequestEntity, NotificationRequest> implements NotificationRequestDao {

    private final NotificationRequestRepository notificationRequestRepository;

    @Override
    public PageData<NotificationRequest> findByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRequestRepository.findByTenantIdAndOriginatorEntityType(tenantId.getId(),
                originatorType, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<NotificationRequestInfo> findInfosByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return DaoUtil.pageToPageData(notificationRequestRepository.findInfosByTenantIdAndOriginatorEntityTypeAndSearchText(tenantId.getId(),
                originatorType, Strings.nullToEmpty(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink))).mapData(NotificationRequestInfoEntity::toData);
    }

    @Override
    public List<NotificationRequestId> findIdsByRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId) {
        return notificationRequestRepository.findAllIdsByStatusAndRuleId(requestStatus, ruleId.getId()).stream()
                .map(NotificationRequestId::new).collect(Collectors.toList());
    }

    @Override
    public List<NotificationRequest> findByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId) {
        return DaoUtil.convertDataList(notificationRequestRepository.findAllByRuleIdAndOriginatorEntityTypeAndOriginatorEntityId(ruleId.getId(), originatorEntityId.getEntityType(), originatorEntityId.getId()));
    }

    @Override
    public PageData<NotificationRequest> findAllByStatus(NotificationRequestStatus status, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRequestRepository.findAllByStatus(status, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void updateById(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats) {
        notificationRequestRepository.updateStatusAndStatsById(requestId.getId(), requestStatus, JacksonUtil.valueToTree(stats));
    }

    @Override
    public boolean existsByStatusAndTargetId(TenantId tenantId, NotificationRequestStatus status, NotificationTargetId targetId) {
        return notificationRequestRepository.existsByStatusAndTargetsContaining(status, targetId.getId().toString());
    }

    @Override
    public boolean existsByStatusAndTemplateId(TenantId tenantId, NotificationRequestStatus status, NotificationTemplateId templateId) {
        return notificationRequestRepository.existsByStatusAndTemplateId(status, templateId.getId());
    }

    @Override
    public int removeAllByCreatedTimeBefore(long ts) {
        return notificationRequestRepository.deleteAllByCreatedTimeBefore(ts);
    }

    @Override
    public NotificationRequestInfo findInfoById(TenantId tenantId, NotificationRequestId id) {
        NotificationRequestInfoEntity info = notificationRequestRepository.findInfoById(id.getId());
        return info != null ? info.toData() : null;
    }

    @Override
    protected Class<NotificationRequestEntity> getEntityClass() {
        return NotificationRequestEntity.class;
    }

    @Override
    protected JpaRepository<NotificationRequestEntity, UUID> getRepository() {
        return notificationRequestRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_REQUEST;
    }

}
