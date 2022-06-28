/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.event.UserAuthDataChangedEvent;
import org.thingsboard.server.common.data.security.model.JwtToken;
import org.thingsboard.server.config.JwtSettings;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
@RequiredArgsConstructor
public class TokenOutdatingService {
    private final CacheManager cacheManager;
    private final JwtTokenFactory tokenFactory;
    private final JwtSettings jwtSettings;
    private Cache usersUpdateTimeCache;

    @PostConstruct
    protected void initCache() {
        usersUpdateTimeCache = cacheManager.getCache(CacheConstants.USERS_UPDATE_TIME_CACHE);
    }

    @EventListener(classes = UserAuthDataChangedEvent.class)
    public void onUserAuthDataChanged(UserAuthDataChangedEvent event) {
        usersUpdateTimeCache.put(toKey(event.getUserId()), event.getTs());
    }

    public boolean isOutdated(JwtToken token, UserId userId) {
        Claims claims = tokenFactory.parseTokenClaims(token).getBody();
        long issueTime = claims.getIssuedAt().getTime();

        return Optional.ofNullable(usersUpdateTimeCache.get(toKey(userId), Long.class))
                .map(outdatageTime -> {
                    if (System.currentTimeMillis() - outdatageTime <= SECONDS.toMillis(jwtSettings.getRefreshTokenExpTime())) {
                        return MILLISECONDS.toSeconds(issueTime) < MILLISECONDS.toSeconds(outdatageTime);
                    } else {
                        /*
                         * Means that since the outdating has passed more than
                         * the lifetime of refresh token (the longest lived)
                         * and there is no need to store outdatage time anymore
                         * as all the tokens issued before the outdatage time
                         * are now expired by themselves
                         * */
                        usersUpdateTimeCache.evict(toKey(userId));
                        return false;
                    }
                })
                .orElse(false);
    }

    private String toKey(UserId userId) {
        return userId.getId().toString();
    }
}
