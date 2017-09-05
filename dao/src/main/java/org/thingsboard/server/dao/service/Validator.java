/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.BasePageLink;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.List;
import java.util.UUID;

public class Validator {

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
     * This method validate <code>String</code> string. If string is invalid than throw
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

    /**
     * This method validate <code>PageLink</code> page link. If pageLink is invalid than throw
     * <code>IncorrectParameterException</code> exception
     *
     * @param pageLink     the page link
     * @param errorMessage the error message for exception
     */
    public static void validatePageLink(BasePageLink pageLink, String errorMessage) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Page link must be specified.");
        } else if (pageLink.getLimit() < 1) {
            throw new IncorrectParameterException("Incorrect page link limit '"+pageLink.getLimit()+"'. Limit must be greater than zero.");
        } else if (pageLink.getIdOffset() != null && pageLink.getIdOffset().version() != 1) {
            throw new IncorrectParameterException("Incorrect page link ID offset '" + pageLink.getIdOffset() + "'. ID offset must be timebased uuid (version 1).");
        }
    }

}
