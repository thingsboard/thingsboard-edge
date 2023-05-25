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
package org.thingsboard.server.dao.service;

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class Validator {

    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$"); // Unicode letters, numbers, '_' and '-' allowed

    /**
     * This method validate <code>EntityId</code> entity id. If entity id is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param entityId          the entityId
     * @param errorMessage the error message for exception
     */
    public static void validateEntityId(EntityId entityId, String errorMessage) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>String</code> string. If string is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val          the val
     * @param errorMessage the error message for exception
     */
    public static void validateString(String val, String errorMessage) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        }
    }


    /**
     * This method validate <code>long</code> value. If value isn't possitive than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param val          the val
     * @param errorMessage the error message for exception
     */
    public static void validatePositiveNumber(long val, String errorMessage) {
        if (val <= 0) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate <code>UUID</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id           the id
     * @param errorMessage the error message for exception
     */
    public static void validateId(UUID id, String errorMessage) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }


    /**
     * This method validate <code>UUIDBased</code> id. If id is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param id           the id
     * @param errorMessage the error message for exception
     */
    public static void validateId(UUIDBased id, String errorMessage) {
        if (id == null || id.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * This method validate list of <code>UUIDBased</code> ids. If at least one of the ids is null than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param ids          the list of ids
     * @param errorMessage the error message for exception
     */
    public static void validateIds(List<? extends UUIDBased> ids, String errorMessage) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        } else {
            for (UUIDBased id : ids) {
                validateId(id, errorMessage);
            }
        }
    }

    public static void validateEntityIds(List<EntityId> ids, String errorMessage) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        } else {
            for (EntityId id : ids) {
                validateEntityId(id, errorMessage);
            }
        }
    }

    /**
     * This method validate <code>PageLink</code> page link. If pageLink is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param pageLink     the page link
     */
    public static void validatePageLink(PageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect page link page size '"+pageLink.getPageSize()+"'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect page link page '"+pageLink.getPage()+"'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null) {
            if (!isValidProperty(pageLink.getSortOrder().getProperty())) {
                throw new IncorrectParameterException("Invalid page link sort property");
            }
        }
    }

    public static void validateEntityDataPageLink(EntityDataPageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Entity Data Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect entity data page link page size '" + pageLink.getPageSize() + "'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect entity data page link page '" + pageLink.getPage() + "'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null && pageLink.getSortOrder().getKey() != null) {
            EntityKey sortKey = pageLink.getSortOrder().getKey();
            if ((sortKey.getType() == EntityKeyType.ENTITY_FIELD || sortKey.getType() == EntityKeyType.ALARM_FIELD)
                    && !isValidProperty(sortKey.getKey())) {
                throw new IncorrectParameterException("Invalid entity data page link sort property");
            }
        }
    }

    public static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

}
