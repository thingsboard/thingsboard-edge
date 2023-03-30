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
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractCustomerEntity<T extends Customer> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    public static final Map<String,String> customerColumnMap = new HashMap<>();
    static {
        customerColumnMap.put("name", "title");
    }

    @Column(name = ModelConstants.CUSTOMER_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.CUSTOMER_PARENT_CUSTOMER_ID_PROPERTY)
    private UUID parentCustomerId;

    @Column(name = ModelConstants.CUSTOMER_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.COUNTRY_PROPERTY)
    private String country;

    @Column(name = ModelConstants.STATE_PROPERTY)
    private String state;

    @Column(name = ModelConstants.CITY_PROPERTY)
    private String city;

    @Column(name = ModelConstants.ADDRESS_PROPERTY)
    private String address;

    @Column(name = ModelConstants.ADDRESS2_PROPERTY)
    private String address2;

    @Column(name = ModelConstants.ZIP_PROPERTY)
    private String zip;

    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    @Column(name = ModelConstants.EMAIL_PROPERTY)
    private String email;

    @Type(type = "json")
    @Column(name = ModelConstants.CUSTOMER_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public AbstractCustomerEntity() {
        super();
    }

    public AbstractCustomerEntity(Customer customer) {
        if (customer.getId() != null) {
            this.setUuid(customer.getId().getId());
        }
        this.setCreatedTime(customer.getCreatedTime());
        this.tenantId = customer.getTenantId().getId();
        if (customer.getParentCustomerId() != null) {
            this.parentCustomerId = customer.getParentCustomerId().getId();
        }
        this.title = customer.getTitle();
        this.country = customer.getCountry();
        this.state = customer.getState();
        this.city = customer.getCity();
        this.address = customer.getAddress();
        this.address2 = customer.getAddress2();
        this.zip = customer.getZip();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
        this.additionalInfo = customer.getAdditionalInfo();
        if (customer.getExternalId() != null) {
            this.externalId = customer.getExternalId().getId();
        }
    }

    public AbstractCustomerEntity(CustomerEntity customerEntity) {
        this.setId(customerEntity.getId());
        this.setCreatedTime(customerEntity.getCreatedTime());
        this.tenantId = customerEntity.getTenantId();
        this.parentCustomerId = customerEntity.getParentCustomerId();
        this.title = customerEntity.getTitle();
        this.country = customerEntity.getCountry();
        this.state = customerEntity.getState();
        this.city = customerEntity.getCity();
        this.address = customerEntity.getAddress();
        this.address2 = customerEntity.getAddress2();
        this.zip = customerEntity.getZip();
        this.phone = customerEntity.getPhone();
        this.email = customerEntity.getEmail();
        this.additionalInfo = customerEntity.getAdditionalInfo();
        this.externalId = customerEntity.getExternalId();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    protected Customer toCustomer() {
        Customer customer = new Customer(new CustomerId(this.getUuid()));
        customer.setCreatedTime(createdTime);
        customer.setTenantId(TenantId.fromUUID(tenantId));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTitle(title);
        customer.setCountry(country);
        customer.setState(state);
        customer.setCity(city);
        customer.setAddress(address);
        customer.setAddress2(address2);
        customer.setZip(zip);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            customer.setExternalId(new CustomerId(externalId));
        }
        return customer;
    }
}
