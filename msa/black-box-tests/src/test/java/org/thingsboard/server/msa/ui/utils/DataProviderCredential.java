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

import org.openqa.selenium.Keys;
import org.testng.annotations.DataProvider;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomSymbol;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DataProviderCredential {

    private static final String SYMBOL = String.valueOf(getRandomSymbol());
    private static final String NAME = ENTITY_NAME;
    private static final String NUMBER = "1";
    private static final String LONG_PHONE_NUMBER = "20155501231";
    private static final String SHORT_PHONE_NUMBER = "201555011";
    private static final String RULE_CHAIN_SECOND_WORD_NAME_PATH = "Rule";
    private static final String CUSTOMER_FIRST_WORD_NAME_PATH = "Customer";
    private static final String RULE_CHAIN_FIRST_WORD_NAME_PATH = "Root";
    private static final String ENTITY_GROUP_FIRST_WORD_NAME_PATH = "Entity";
    private static final String ENTITY_GROUP_SECOND_WORD_NAME_PATH = "Group";
    private static final String CUSTOMER_SECOND_WORD_NAME_PATH = "A";
    private static final String DEFAULT_DEVICE_PROFILE_NAME = "Device Profile";
    private static final String DEFAULT_ASSET_PROFILE_NAME = "Asset Profile";

    @DataProvider
    public static Object[][] ruleChainNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {RULE_CHAIN_SECOND_WORD_NAME_PATH},
                {RULE_CHAIN_FIRST_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] nameForSearchBySymbolAndNumber() {
        return new Object[][]{
                {NAME, ENTITY_NAME.split("`")[1]},
                {NAME, "~"},
                {NAME, "`"},
                {NAME, "!"},
                {NAME, "@"},
                {NAME, "#"},
                {NAME, "$"},
                {NAME, "^"},
                {NAME, "&"},
                {NAME, "*"},
                {NAME, "("},
                {NAME, ")"},
                {NAME, "+"},
                {NAME, "="},
                {NAME, "-"}};
    }

    @DataProvider
    public static Object[][] nameForSort() {
        return new Object[][]{
                {NAME},
                {SYMBOL},
                {NUMBER}};
    }

    @DataProvider
    public static Object[][] nameForAllSort() {
        return new Object[][]{
                {NAME, SYMBOL, NUMBER}};
    }

    @DataProvider
    public static Object[][] incorrectPhoneNumber() {
        return new Object[][]{
                {LONG_PHONE_NUMBER},
                {SHORT_PHONE_NUMBER},
                {ENTITY_NAME}};
    }

    @DataProvider
    public static Object[][] customerNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {CUSTOMER_FIRST_WORD_NAME_PATH},
                {CUSTOMER_SECOND_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] customerGroupNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {ENTITY_GROUP_FIRST_WORD_NAME_PATH},
                {ENTITY_GROUP_SECOND_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] deviceProfileSearch() {
        return new Object[][]{
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_DEVICE_PROFILE_NAME, DEFAULT_DEVICE_PROFILE_NAME.split(" ")[1]},
                {NAME, ENTITY_NAME.split("`")[1]},
                {NAME, "~"},
                {NAME, "`"},
                {NAME, "!"},
                {NAME, "@"},
                {NAME, "#"},
                {NAME, "$"},
                {NAME, "^"},
                {NAME, "&"},
                {NAME, "*"},
                {NAME, "("},
                {NAME, ")"},
                {NAME, "+"},
                {NAME, "="},
                {NAME, "-"}};
    }

    @DataProvider
    public static Object[][] assetProfileSearch() {
        return new Object[][]{
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[0]},
                {DEFAULT_ASSET_PROFILE_NAME, DEFAULT_ASSET_PROFILE_NAME.split(" ")[1]},
                {NAME, ENTITY_NAME.split("`")[1]},
                {NAME, "~"},
                {NAME, "`"},
                {NAME, "!"},
                {NAME, "@"},
                {NAME, "#"},
                {NAME, "$"},
                {NAME, "^"},
                {NAME, "&"},
                {NAME, "*"},
                {NAME, "("},
                {NAME, ")"},
                {NAME, "+"},
                {NAME, "="},
                {NAME, "-"}};
    }

    @DataProvider
    public static Object[][] editMenuDescription() {
        String newDescription = "Description" + getRandomNumber();
        String description = "Description";
        return new Object[][]{
                {"", newDescription, newDescription},
                {description, newDescription, description + newDescription},
                {description, Keys.CONTROL + "A" + Keys.BACK_SPACE, ""}};
    }
}
