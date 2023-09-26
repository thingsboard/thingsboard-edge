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
package org.thingsboard.server.service.security.auth;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class DefaultTokenOutdatingService implements TokenOutdatingService {

    private final TbTransactionalCache<String, Long> cache;
    private final JwtTokenFactory tokenFactory;

    public DefaultTokenOutdatingService(@Qualifier("UsersSessionInvalidation") TbTransactionalCache<String, Long> cache, JwtTokenFactory tokenFactory) {
        this.cache = cache;
        this.tokenFactory = tokenFactory;
    }

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        if (StringUtils.hasText(event.getId())) {
            cache.put(event.getId(), event.getTs());
        }
    }

    @Override
    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();
        String sessionId = claims.get("sessionId", String.class);
        if (isTokenOutdated(issueTime, userId.toString())){
             return true;
        } else {
             return sessionId != null && isTokenOutdated(issueTime, sessionId);
        }
    }

    private Boolean isTokenOutdated(long issueTime, String sessionId) {
        return Optional.ofNullable(cache.get(sessionId)).map(outdatageTime -> isTokenOutdated(issueTime, outdatageTime.get())).orElse(false);
    }

    private boolean isTokenOutdated(long issueTime, Long outdatageTime) {
        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
    }
}
