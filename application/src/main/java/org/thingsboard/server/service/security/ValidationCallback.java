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
package org.thingsboard.server.service.security;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.server.exception.AccessDeniedException;
import org.thingsboard.server.exception.EntityNotFoundException;
import org.thingsboard.server.exception.InternalErrorException;
import org.thingsboard.server.exception.UnauthorizedException;

/**
 * Created by ashvayka on 31.03.18.
 */
public class ValidationCallback<T> implements FutureCallback<ValidationResult> {

    private final T response;
    private final FutureCallback<T> action;

    public ValidationCallback(T response, FutureCallback<T> action) {
        this.response = response;
        this.action = action;
    }

    @Override
    public void onSuccess(ValidationResult result) {
        if (result.getResultCode() == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            onFailure(getException(result));
        }
    }

    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }

    public static Exception getException(ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        Exception e;
        switch (resultCode) {
            case ENTITY_NOT_FOUND:
                e = new EntityNotFoundException(result.getMessage());
                break;
            case UNAUTHORIZED:
                e = new UnauthorizedException(result.getMessage());
                break;
            case ACCESS_DENIED:
                e = new AccessDeniedException(result.getMessage());
                break;
            case INTERNAL_ERROR:
                e = new InternalErrorException(result.getMessage());
                break;
            default:
                e = new UnauthorizedException("Permission denied.");
                break;
        }
        return e;
    }

}
