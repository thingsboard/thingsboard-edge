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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by igor on 2/27/18.
 */
@Slf4j
public class DatabaseHelper {

    public static final CSVFormat CSV_DUMP_FORMAT = CSVFormat.DEFAULT.withNullString("\\N");

    public static final String DEVICE = "device";
    public static final String ENTITY_ID = "entity_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENTITY_TYPE = "entity_type";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String SEARCH_TEXT = "search_text";
    public static final String ADDITIONAL_INFO = "additional_info";
    public static final String ASSET = "asset";
    public static final String DASHBOARD = "dashboard";
    public static final String ENTITY_VIEWS = "entity_views";
    public static final String ENTITY_VIEW = "entity_view";
    public static final String RULE_CHAIN = "rule_chain";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String KEYS = "keys";
    public static final String START_TS = "start_ts";
    public static final String END_TS = "end_ts";
    public static final String ASSIGNED_CUSTOMERS = "assigned_customers";
    public static final String CONFIGURATION = "configuration";

    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static void upgradeTo40_assignDashboards(Path dashboardsDump, DashboardService dashboardService, boolean sql) throws Exception {
        JavaType assignedCustomersType =
                objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(dashboardsDump), CSV_DUMP_FORMAT.withFirstRecordAsHeader())) {
            csvParser.forEach(record -> {
                String customerIdString = record.get(CUSTOMER_ID);
                String assignedCustomersString = record.get(ASSIGNED_CUSTOMERS);
                DashboardId dashboardId = new DashboardId(toUUID(record.get(ID), sql));
                List<CustomerId> customerIds = new ArrayList<>();
                if (!StringUtils.isEmpty(assignedCustomersString)) {
                    try {
                        Set<ShortCustomerInfo> assignedCustomers = objectMapper.readValue(assignedCustomersString, assignedCustomersType);
                        assignedCustomers.forEach((customerInfo) -> {
                            CustomerId customerId = customerInfo.getCustomerId();
                            if (!customerId.isNullUid()) {
                                customerIds.add(customerId);
                            }
                        });
                    } catch (IOException e) {
                        log.error("Unable to parse assigned customers field", e);
                    }
                }
                if (!StringUtils.isEmpty(customerIdString)) {
                    CustomerId customerId = new CustomerId(toUUID(customerIdString, sql));
                    if (!customerId.isNullUid()) {
                        customerIds.add(customerId);
                    }
                }
                for (CustomerId customerId : customerIds) {
                    dashboardService.assignDashboardToCustomer(TenantId.SYS_TENANT_ID, dashboardId, customerId);
                }
            });
        }
    }

    private static UUID toUUID(String src, boolean sql) {
        if (sql) {
            return UUIDConverter.fromString(src);
        } else {
            return UUID.fromString(src);
        }
    }

}
