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
package org.thingsboard.server.service.security.auth.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.auth.RefreshAuthenticationToken;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

import java.util.UUID;

@Component
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final JwtTokenFactory tokenFactory;
    private final UserService userService;
    private final CustomerService customerService;

    @Autowired
    public RefreshTokenAuthenticationProvider(final UserService userService, final CustomerService customerService, final JwtTokenFactory tokenFactory) {
        this.userService = userService;
        this.customerService = customerService;
        this.tokenFactory = tokenFactory;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser unsafeUser = tokenFactory.parseRefreshToken(rawAccessToken);
        UserPrincipal principal = unsafeUser.getUserPrincipal();
        SecurityUser securityUser;
        if (principal.getType() == UserPrincipal.Type.USER_NAME) {
            securityUser = authenticateByUserId(unsafeUser.getId());
        } else {
            securityUser = authenticateByPublicId(principal.getValue());
        }
        return new RefreshAuthenticationToken(securityUser);
    }

    private SecurityUser authenticateByUserId(UserId userId) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found by refresh token");
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getId());
        if (userCredentials == null) {
            throw new UsernameNotFoundException("User credentials not found");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        if (user.getAuthority() == null) throw new InsufficientAuthenticationException("User has no authority assigned");

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());

        SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);

        return securityUser;
    }

    private SecurityUser authenticateByPublicId(String publicId) {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token is not valid");
        }
        Customer publicCustomer = customerService.findCustomerById(customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found by refresh token");
        }

        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Refresh token is not valid");
        }

        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        UserPrincipal userPrincipal = new UserPrincipal(UserPrincipal.Type.PUBLIC_ID, publicId);

        SecurityUser securityUser = new SecurityUser(user, true, userPrincipal);

        return securityUser;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (RefreshAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
