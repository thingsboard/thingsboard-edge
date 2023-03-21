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
package org.thingsboard.server.dao.alarm;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService {

    @Autowired
    private AlarmCommentDao alarmCommentDao;

    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;

    @Override
    public AlarmComment createOrUpdateAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        if (alarmComment.getId() == null) {
            return createAlarmComment(tenantId, alarmComment);
        } else {
            return updateAlarmComment(tenantId, alarmComment);
        }
    }

    @Override
    public AlarmComment saveAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("Deleting Alarm Comment: {}", alarmComment);
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        return alarmCommentDao.save(tenantId, alarmComment);
    }

    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId alarmId, PageLink pageLink) {
        log.trace("Executing findAlarmComments by alarmId [{}]", alarmId);
        return alarmCommentDao.findAlarmComments(tenantId, alarmId, pageLink);
    }

    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findById(tenantId, alarmCommentId.getId());
    }

    private AlarmComment createAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType(AlarmCommentType.OTHER);
        }
        if (alarmComment.getId() == null) {
            UUID uuid = Uuids.timeBased();
            alarmComment.setId(new AlarmCommentId(uuid));
            alarmComment.setCreatedTime(System.currentTimeMillis());
        }
        return alarmCommentDao.createAlarmComment(tenantId, alarmComment);
    }

    private AlarmComment updateAlarmComment(TenantId tenantId, AlarmComment newAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        AlarmComment existing = alarmCommentDao.findAlarmCommentById(tenantId, newAlarmComment.getId().getId());
        if (existing != null) {
            if (newAlarmComment.getComment() != null) {
                JsonNode comment = newAlarmComment.getComment();
                ((ObjectNode) comment).put("edited", "true");
                ((ObjectNode) comment).put("editedOn", System.currentTimeMillis());
                existing.setComment(comment);
            }
            return alarmCommentDao.save(tenantId, existing);
        }
        return null;
    }

}
