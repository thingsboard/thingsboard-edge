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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;

import static org.thingsboard.server.controller.ControllerConstants.ALARM_COMMENT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_COMMENT_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ALARM_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.dao.service.Validator.validateId;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmCommentController extends BaseController {
    public static final String ALARM_ID = "alarmId";
    public static final String ALARM_COMMENT_ID = "commentId";

    private final TbAlarmCommentService tbAlarmCommentService;

    @ApiOperation(value = "Create or update Alarm Comment ",
            notes = "Creates or Updates the Alarm Comment. " +
                    "When creating comment, platform generates Alarm Comment Id as " + UUID_WIKI_LINK +
                    "The newly created Alarm Comment id will be present in the response. Specify existing Alarm Comment id to update the alarm. " +
                    "Referencing non-existing Alarm Comment Id will cause 'Not Found' error. " +
                    "\n\n To create new Alarm comment entity it is enough to specify 'comment' json element with 'text' node, for example: {\"comment\": { \"text\": \"my comment\"}}. " +
                    "\n\n If comment type is not specified the default value 'OTHER' will be saved. If 'alarmId' or 'userId' specified in body it will be ignored." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment", method = RequestMethod.POST)
    @ResponseBody
    public AlarmComment saveAlarmComment(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION)
                                         @PathVariable(ALARM_ID) String strAlarmId, @ApiParam(value = "A JSON value representing the comment.") @RequestBody AlarmComment alarmComment) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        alarmComment.setAlarmId(alarmId);
        return tbAlarmCommentService.saveAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm comment (deleteAlarmComment)",
            notes = "Deletes the Alarm comment. Referencing non-existing Alarm comment Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment/{commentId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteAlarmComment(@ApiParam(value = ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId, @ApiParam(value = ALARM_COMMENT_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_COMMENT_ID) String strCommentId) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);

        AlarmCommentId alarmCommentId = new AlarmCommentId(toUUID(strCommentId));
        AlarmComment alarmComment = checkAlarmCommentId(alarmCommentId, alarmId);
        tbAlarmCommentService.deleteAlarmComment(alarm, alarmComment, getCurrentUser());
    }

    @ApiOperation(value = "Get Alarm comments (getAlarmComments)",
            notes = "Returns a page of alarm comments for specified alarm. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/comment", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmCommentInfo> getAlarmComments(
            @ApiParam(value = ALARM_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ALARM_ID) String strAlarmId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = ALARM_COMMENT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        checkAlarmId(alarmId, Operation.READ);

        PageLink pageLink = createPageLink(pageSize, page, null, sortProperty, sortOrder);
        return checkNotNull(alarmCommentService.findAlarmComments(getTenantId(), alarmId, pageLink));
    }
}
