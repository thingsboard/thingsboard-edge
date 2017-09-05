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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.CUSTOMER_COLUMN_FAMILY_NAME)
public final class CustomerEntity extends BaseSqlEntity<Customer> implements SearchTextEntity<Customer> {

    @Transient
    private static final long serialVersionUID = 8951342124082981556L;

    @Column(name = ModelConstants.CUSTOMER_TENANT_ID_PROPERTY)
    private String tenantId;
    
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

    public CustomerEntity() {
        super();
    }

    public CustomerEntity(Customer customer) {
        if (customer.getId() != null) {
            this.setId(customer.getId().getId());
        }
        this.tenantId = UUIDConverter.fromTimeUUID(customer.getTenantId().getId());
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
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Customer toData() {
        Customer customer = new Customer(new CustomerId(getId()));
        customer.setCreatedTime(UUIDs.unixTimestamp(getId()));
        customer.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
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
        return customer;
    }
}