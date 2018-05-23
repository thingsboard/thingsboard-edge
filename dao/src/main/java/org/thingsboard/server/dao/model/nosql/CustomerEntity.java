/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ADDRESS2_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ADDRESS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.COUNTRY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_TITLE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EMAIL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.PHONE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.STATE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ZIP_PROPERTY;

@Table(name = CUSTOMER_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class CustomerEntity implements SearchTextEntity<Customer> {

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;
    
    @PartitionKey(value = 1)
    @Column(name = CUSTOMER_TENANT_ID_PROPERTY)
    private UUID tenantId;
    
    @Column(name = CUSTOMER_TITLE_PROPERTY)
    private String title;
    
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;
    
    @Column(name = COUNTRY_PROPERTY)
    private String country;
    
    @Column(name = STATE_PROPERTY)
    private String state;

    @Column(name = CITY_PROPERTY)
    private String city;

    @Column(name = ADDRESS_PROPERTY)
    private String address;

    @Column(name = ADDRESS2_PROPERTY)
    private String address2;

    @Column(name = ZIP_PROPERTY)
    private String zip;

    @Column(name = PHONE_PROPERTY)
    private String phone;

    @Column(name = EMAIL_PROPERTY)
    private String email;

    @Column(name = CUSTOMER_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public CustomerEntity() {
        super();
    }

    public CustomerEntity(Customer customer) {
        if (customer.getId() != null) {
            this.id = customer.getId().getId();
        }
        this.tenantId = customer.getTenantId().getId();
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
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
    
    @Override
    public String getSearchTextSource() {
        return getTitle();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public String getSearchText() {
        return searchText;
    }

    @Override
    public Customer toData() {
        Customer customer = new Customer(new CustomerId(id));
        customer.setCreatedTime(UUIDs.unixTimestamp(id));
        customer.setTenantId(new TenantId(tenantId));
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