/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.security.auth.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;

import java.util.UUID;

@Component
public class RestAuthenticationProvider implements AuthenticationProvider {

    private final BCryptPasswordEncoder encoder;
    private final UserService userService;
    private final CustomerService customerService;

    @Autowired
    public RestAuthenticationProvider(final UserService userService, final CustomerService customerService, final BCryptPasswordEncoder encoder) {
        this.userService = userService;
        this.customerService = customerService;
        this.encoder = encoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal)) {
            throw new BadCredentialsException("Authentication Failed. Bad user principal.");
        }

        UserPrincipal userPrincipal =  (UserPrincipal) principal;
        if (userPrincipal.getType() == UserPrincipal.Type.USER_NAME) {
            String username = userPrincipal.getValue();
            String password = (String) authentication.getCredentials();
            return authenticateByUsernameAndPassword(userPrincipal, username, password);
        } else {
            String publicId = userPrincipal.getValue();
            return authenticateByPublicId(userPrincipal, publicId);
        }
    }

    private Authentication authenticateByUsernameAndPassword(UserPrincipal userPrincipal, String username, String password) {
        User user = userService.findUserByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getId());
        if (userCredentials == null) {
            throw new UsernameNotFoundException("User credentials not found");
        }

        if (!userCredentials.isEnabled()) {
            throw new DisabledException("User is not active");
        }

        if (!encoder.matches(password, userCredentials.getPassword())) {
            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
        }

        if (user.getAuthority() == null) throw new InsufficientAuthenticationException("User has no authority assigned");

        SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), userPrincipal);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    private Authentication authenticateByPublicId(UserPrincipal userPrincipal, String publicId) {
        CustomerId customerId;
        try {
            customerId = new CustomerId(UUID.fromString(publicId));
        } catch (Exception e) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        Customer publicCustomer = customerService.findCustomerById(customerId);
        if (publicCustomer == null) {
            throw new UsernameNotFoundException("Public entity not found: " + publicId);
        }
        if (!publicCustomer.isPublic()) {
            throw new BadCredentialsException("Authentication Failed. Public Id is not valid.");
        }
        User user = new User(new UserId(EntityId.NULL_UUID));
        user.setTenantId(publicCustomer.getTenantId());
        user.setCustomerId(publicCustomer.getId());
        user.setEmail(publicId);
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName("Public");
        user.setLastName("Public");

        SecurityUser securityUser = new SecurityUser(user, true, userPrincipal);

        return new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
