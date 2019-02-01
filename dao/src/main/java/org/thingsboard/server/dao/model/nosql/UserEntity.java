/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.AuthorityCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_AUTHORITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.USER_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_FIRST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_LAST_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.USER_TENANT_ID_PROPERTY;

@Table(name = USER_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class UserEntity implements SearchTextEntity<User> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = USER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @PartitionKey(value = 2)
    @Column(name = USER_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @PartitionKey(value = 3)
    @Column(name = USER_AUTHORITY_PROPERTY, codec = AuthorityCodec.class)
    private Authority authority;

    @Column(name = USER_EMAIL_PROPERTY)
    private String email;
    
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    
    @Column(name = USER_FIRST_NAME_PROPERTY)
    private String firstName;
    
    @Column(name = USER_LAST_NAME_PROPERTY)
    private String lastName;

    @Column(name = USER_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public UserEntity() {
        super();
    }

    public UserEntity(User user) {
        if (user.getId() != null) {
            this.id = user.getId().getId();
        }
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
        this.additionalInfo = user.getAdditionalInfo();
    }
    
	public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Authority getAuthority() {
		return authority;
	}

	public void setAuthority(Authority authority) {
		this.authority = authority;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public JsonNode getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(JsonNode additionalInfo) {
		this.additionalInfo = additionalInfo;
	}
	
    @Override
    public String getSearchTextSource() {
        return getEmail();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public String getSearchText() {
        return searchText;
    }

    @Override
    public User toData() {
		User user = new User(new UserId(id));
		user.setCreatedTime(UUIDs.unixTimestamp(id));
		user.setAuthority(authority);
		if (tenantId != null) {
			user.setTenantId(new TenantId(tenantId));
		}
		if (customerId != null) {
			user.setCustomerId(new CustomerId(customerId));
		}
		user.setEmail(email);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setAdditionalInfo(additionalInfo);
        return user;
    }

}