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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractUserEntity<T extends User> extends BaseSqlEntity<T> implements BaseEntity<T> {

    public static final Map<String,String> userColumnMap = new HashMap<>();
    static {
        userColumnMap.put("name", "email");
    }

    @Column(name = ModelConstants.USER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.USER_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.USER_AUTHORITY_PROPERTY)
    private Authority authority;

    @Column(name = ModelConstants.USER_EMAIL_PROPERTY, unique = true)
    private String email;

    @Column(name = ModelConstants.USER_FIRST_NAME_PROPERTY)
    private String firstName;

    @Column(name = ModelConstants.USER_LAST_NAME_PROPERTY)
    private String lastName;

    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    @Type(type = "json")
    @Column(name = ModelConstants.USER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractUserEntity() {
        super();
    }

    public AbstractUserEntity(User user) {
        if (user.getId() != null) {
            this.setUuid(user.getId().getId());
        }
        this.setCreatedTime(user.getCreatedTime());
        this.authority = user.getAuthority();
        if (user.getTenantId() != null) {
            this.tenantId = user.getTenantId().getId();
        }
        if (user.getCustomerId() != null) {
            this.customerId = user.getCustomerId().getId();
        }
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phone = user.getPhone();
        this.additionalInfo = user.getAdditionalInfo();
    }

    public AbstractUserEntity(UserEntity userEntity) {
        this.setId(userEntity.getId());
        this.setCreatedTime(userEntity.getCreatedTime());
        this.tenantId = userEntity.getTenantId();
        this.customerId = userEntity.getCustomerId();
        this.authority = userEntity.getAuthority();
        this.email = userEntity.getEmail();
        this.firstName = userEntity.getFirstName();
        this.lastName = userEntity.getLastName();
        this.phone = userEntity.getPhone();
        this.additionalInfo = userEntity.getAdditionalInfo();
    }

    protected User toUser() {
        User user = new User(new UserId(this.getUuid()));
        user.setCreatedTime(createdTime);
        user.setAuthority(authority);
        if (tenantId != null) {
            user.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            user.setCustomerId(new CustomerId(customerId));
        }
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAdditionalInfo(additionalInfo);
        return user;
    }

}
