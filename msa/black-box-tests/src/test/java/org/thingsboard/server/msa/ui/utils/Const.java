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
package org.thingsboard.server.msa.ui.utils;

import static org.thingsboard.server.msa.TestProperties.getBaseUrl;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;

public class Const {

    public static final String URL = getBaseUrl();
    public static final String TENANT_EMAIL = "tenant@thingsboard.org";
    public static final String TENANT_PASSWORD = "tenant";
    public static final String ENTITY_NAME = "Aaz!@#$%^&*()_-+=~`" + getRandomNumber();
    public static final String ROOT_RULE_CHAIN_NAME = "Root Rule Chain";
    public static final String IMPORT_RULE_CHAIN_NAME = "Rule Chain For Import";
    public static final String IMPORT_DEVICE_PROFILE_NAME = "Device Profile For Import";
    public static final String IMPORT_ASSET_PROFILE_NAME = "Asset Profile For Import";
    public static final String IMPORT_RULE_CHAIN_FILE_NAME = "ruleChainForImport.json";
    public static final String IMPORT_DEVICE_PROFILE_FILE_NAME = "deviceProfileForImport.json";
    public static final String IMPORT_ASSET_PROFILE_FILE_NAME = "assetProfileForImport.json";
    public static final String IMPORT_TXT_FILE_NAME = "forImport.txt";
    public static final String EMPTY_IMPORT_MESSAGE = "No file selected";
    public static final String EMPTY_RULE_CHAIN_MESSAGE = "Rule chain name should be specified!";
    public static final String EMPTY_CUSTOMER_MESSAGE = "Customer title should be specified!";
    public static final String EMPTY_DEVICE_PROFILE_MESSAGE = "Device profile name should be specified!";
    public static final String EMPTY_ASSET_PROFILE_MESSAGE = "Asset profile name should be specified!";
    public static final String EMPTY_GROUP_NAME_MESSAGE = "Entity group name should be specified!";
    public static final String DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE = "The rule chain referenced by the device profiles cannot be deleted!";
    public static final String SAME_NAME_WARNING_CUSTOMER_MESSAGE = "Customer with such title already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE = "Device profile with such name already exists!";
    public static final String SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE = "Asset profile with such name already exists!";
    public static final String SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE = "Entity Group with such name, type and owner already exists!";
    public static final String PHONE_NUMBER_ERROR_MESSAGE = "Phone number is invalid or not possible";
    public static final String NAME_IS_REQUIRED_MESSAGE = "Name is required.";
    public static final String COPY_ENTITY_GROUP_ID_MESSAGE = "Entity group Id has been copied to clipboard";
    public static final String OWNER_NOT_SELECTED_ERROR = "Target owner is required.";
}