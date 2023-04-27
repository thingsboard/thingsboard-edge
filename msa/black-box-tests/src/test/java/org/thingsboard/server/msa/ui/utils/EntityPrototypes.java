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
package org.thingsboard.server.msa.ui.utils;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;

public class EntityPrototypes {

    public static Customer defaultCustomerPrototype(String entityName) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        return customer;
    }

    public static Customer defaultCustomerPrototype(String entityName, String description) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return customer;
    }

    public static Customer defaultCustomerPrototype(String entityName, int number) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setPhone("+1" + number);
        return customer;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, String description) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, boolean debugMode) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setDebugMode(debugMode);
        return ruleChain;
    }

    public static EntityGroup defaultEntityGroupPrototype(String entityName, EntityType entityType) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityName);
        entityGroup.setType(entityType);
        return entityGroup;
    }

    public static EntityGroup defaultEntityGroupPrototype(String entityName, EntityType entityType, String
            description) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityName);
        entityGroup.setType(entityType);
        entityGroup.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return entityGroup;
    }

    public static DeviceProfile defaultDeviceProfile(String entityName) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    public static DeviceProfile defaultDeviceProfile(String entityName, String description) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setDescription(description);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    public static AssetProfile defaultAssetProfile(String entityName) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        return assetProfile;
    }

    public static AssetProfile defaultAssetProfile(String entityName, String description) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        assetProfile.setDescription(description);
        return assetProfile;
    }

    public static Alarm defaultAlarm(EntityId id, String type) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setPropagate(propagate);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, UserId userId) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, UserId userId, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        alarm.setPropagate(propagate);
        return alarm;
    }

    public static User defaultUser(String email, CustomerId customerId) {
        User user = new User();
        user.setEmail(email);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    public static User defaultUser(String email, CustomerId customerId, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(name);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    public static Device defaultDevicePrototype(String name) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        return device;
    }

    public static Device defaultDevicePrototype(String name, CustomerId id) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setCustomerId(id);
        device.setType("DEFAULT");
        return device;
    }

    public static Asset defaultAssetPrototype(String name, CustomerId id) {
        Asset asset = new Asset();
        asset.setName(name + RandomStringUtils.randomAlphanumeric(7));
        asset.setCustomerId(id);
        asset.setType("DEFAULT");
        return asset;
    }

    public static EntityView defaultEntityViewPrototype(String name, String type, String entityType) {
        EntityView entityView = new EntityView();
        entityView.setName(name + RandomStringUtils.randomAlphanumeric(7));
        entityView.setType(type + RandomStringUtils.randomAlphanumeric(7));
        entityView.setAdditionalInfo(JacksonUtil.newObjectNode().put("entityType", entityType));
        return entityView;
    }
}
