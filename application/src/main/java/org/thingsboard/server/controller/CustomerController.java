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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.customer.TbCustomerService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GROUP_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD_HIDE_TOOLBAR;
import static org.thingsboard.server.controller.ControllerConstants.HOME_DASHBOARD_ID;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_GROUP_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_WRITE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class CustomerController extends BaseController {

    private final TbCustomerService tbCustomerService;

    public static final String IS_PUBLIC = "isPublic";
    public static final String CUSTOMER_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the customer is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the user belongs to the customer.";

    @ApiOperation(value = "Get Customer (getCustomerById)",
            notes = "Get the Customer object based on the provided Customer Id. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.GET)
    @ResponseBody
    public Customer getCustomerById(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        if (!customer.getAdditionalInfo().isNull()) {
            processDashboardIdFromAdditionalInfo((ObjectNode) customer.getAdditionalInfo(), HOME_DASHBOARD);
        }
        return customer;
    }

    @ApiOperation(value = "Get Customer info (getCustomerInfoById)",
            notes = "Get the Customer info object based on the provided Customer Id. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/info/{customerId}", method = RequestMethod.GET)
    @ResponseBody
    public CustomerInfo getCustomerInfoById(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        CustomerInfo customer = checkCustomerInfoId(customerId, Operation.READ);
        if (!customer.getAdditionalInfo().isNull()) {
            processDashboardIdFromAdditionalInfo((ObjectNode) customer.getAdditionalInfo(), HOME_DASHBOARD);
        }
        return customer;
    }


    @ApiOperation(value = "Get short Customer info (getShortCustomerInfoById)",
            notes = "Get the short customer object that contains only the title and 'isPublic' flag. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/shortInfo", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getShortCustomerInfoById(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode infoObject = objectMapper.createObjectNode();
        infoObject.put("title", customer.getTitle());
        infoObject.put(IS_PUBLIC, customer.isPublic());
        return infoObject;
    }

    @ApiOperation(value = "Get Customer Title (getCustomerTitleById)",
            notes = "Get the title of the customer. "
                    + CUSTOMER_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/title", method = RequestMethod.GET, produces = "application/text")
    @ResponseBody
    public String getCustomerTitleById(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        return customer.getTitle();
    }

    @ApiOperation(value = "Create or update Customer (saveCustomer)",
            notes = "Creates or Updates the Customer. When creating customer, platform generates Customer Id as " + UUID_WIKI_LINK +
                    "The newly created Customer Id will be present in the response. " +
                    "Specify existing Customer Id to update the Customer. " +
                    "Referencing non-existing Customer Id will cause 'Not Found' error." + "Remove 'id', 'tenantId' from the request body example (below) to create new Customer entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer", method = RequestMethod.POST)
    @ResponseBody
    public Customer saveCustomer(@ApiParam(value = "A JSON value representing the customer.") @RequestBody Customer customer,
                                 @ApiParam(value = ENTITY_GROUP_ID_CREATE_PARAM_DESCRIPTION)
                                 @RequestParam(name = "entityGroupId", required = false) String strEntityGroupId,
                                 @ApiParam(value = ENTITY_GROUP_IDS_CREATE_PARAM_DESCRIPTION)
                                 @RequestParam(name = "entityGroupIds", required = false) String[] strEntityGroupIds) throws ThingsboardException {
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.WHITE_LABELING, Operation.WRITE)) {
            String prevHomeDashboardId = null;
            boolean prevHideDashboardToolbar = true;
            if (customer.getId() != null) {
                Customer prevCustomer = customerService.findCustomerById(getTenantId(), customer.getId());
                JsonNode additionalInfo = prevCustomer.getAdditionalInfo();
                if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID)) {
                    prevHomeDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                    if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                        prevHideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                    }
                }
            }
            JsonNode additionalInfo = customer.getAdditionalInfo();
            if (additionalInfo == null) {
                additionalInfo = JacksonUtil.newObjectNode();
                customer.setAdditionalInfo(additionalInfo);
            }
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, prevHomeDashboardId);
            ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, prevHideDashboardToolbar);
        }
        SecurityUser user = getCurrentUser();
        return saveGroupEntity(customer, strEntityGroupId, strEntityGroupIds, (customer1, entityGroups) -> {
            try {
                return tbCustomerService.save(customer, entityGroups, user);
            } catch (Exception e) {
                throw handleException(e);
            }
        });
    }

    @ApiOperation(value = "Delete Customer (deleteCustomer)",
            notes = "Deletes the Customer and all customer Users. " +
                    "All assigned Dashboards, Assets, Devices, etc. will be unassigned but not deleted. " +
                    "Referencing non-existing Customer Id will cause an error." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteCustomer(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
                               @PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.DELETE);
        tbCustomerService.delete(customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Customers (getCustomers)",
            notes = "Returns a page of customers owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getCustomers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
    }

    @ApiOperation(value = "Get Tenant Customer by Customer title (getTenantCustomer)",
            notes = "Get the Customer using Customer Title. " + TENANT_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/customers", params = {"customerTitle"}, method = RequestMethod.GET)
    @ResponseBody
    public Customer getTenantCustomer(
            @ApiParam(value = "A string value representing the Customer title.")
            @RequestParam String customerTitle) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(customerService.findCustomerByTenantIdAndTitle(tenantId, customerTitle), "Customer with title [" + customerTitle + "] is not found");
    }

    @ApiOperation(value = "Get Customers (getUserCustomers)",
            notes = "Returns a page of customers available for the user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getUserCustomers(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        SecurityUser currentUser = getCurrentUser();
        MergedUserPermissions mergedUserPermissions = currentUser.getUserPermissions();
        return entityService.findUserEntities(currentUser.getTenantId(), currentUser.getCustomerId(), mergedUserPermissions, EntityType.CUSTOMER,
                Operation.READ, null, pageLink);
    }

    @ApiOperation(value = "Get All Customer Infos for current user (getAllCustomerInfos)",
            notes = "Returns a page of customer info objects owned by the tenant or the customer of a current user. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customerInfos/all", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<CustomerInfo> getAllCustomerInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (Authority.TENANT_ADMIN.equals(getCurrentUser().getAuthority())) {
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(customerService.findCustomerInfosByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(customerService.findTenantCustomerInfosByTenantId(tenantId, pageLink));
            }
        } else {
            CustomerId customerId = getCurrentUser().getCustomerId();
            if (includeCustomers != null && includeCustomers) {
                return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        }
    }

    @ApiOperation(value = "Get Customer sub-customers Infos (getCustomerCustomerInfos)",
            notes = "Returns a page of customer info objects owned by the specified customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/customerInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<CustomerInfo> getCustomerCustomerInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = INCLUDE_CUSTOMERS_OR_SUB_CUSTOMERS)
            @RequestParam(required = false) Boolean includeCustomers,
            @ApiParam(value = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(CUSTOMER_ID, strCustomerId);
        accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (includeCustomers != null && includeCustomers) {
            return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, customerId, pageLink));
        } else {
            return checkNotNull(customerService.findCustomerInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get customers by Customer Ids (getCustomersByEntityGroupId)",
            notes = "Returns a list of Customer objects based on the provided ids. Filters the list based on the user permissions. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH + RBAC_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customers", params = {"customerIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Customer> getCustomersByIds(
            @ApiParam(value = "A list of customer ids, separated by comma ','", required = true)
            @RequestParam("customerIds") String[] strCustomerIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("customerIds", strCustomerIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<CustomerId> customerIds = new ArrayList<>();
        for (String strCustomerId : strCustomerIds) {
            customerIds.add(new CustomerId(toUUID(strCustomerId)));
        }
        List<Customer> customers = checkNotNull(customerService.findCustomersByTenantIdAndIdsAsync(tenantId, customerIds).get());
        return filterCustomersByReadPermission(customers);
    }

    @ApiOperation(value = "Get customers by Entity Group Id (getCustomersByEntityGroupId)",
            notes = "Returns a page of Customer objects that belongs to specified Entity Group Id. " +
                    PAGE_DATA_PARAMETERS + "\n\n" + RBAC_GROUP_READ_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityGroup/{entityGroupId}/customers", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Customer> getCustomersByEntityGroupId(
            @ApiParam(value = ENTITY_GROUP_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ENTITY_GROUP_ID) String strEntityGroupId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true, allowableValues = "range[1, infinity]")
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true, allowableValues = "range[0, infinity]")
            @RequestParam int page,
            @ApiParam(value = CUSTOMER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = CUSTOMER_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        checkParameter(ENTITY_GROUP_ID, strEntityGroupId);
        EntityGroupId entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
        EntityGroup entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
        checkEntityGroupType(EntityType.CUSTOMER, entityGroup.getType());
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(customerService.findCustomersByEntityGroupId(entityGroupId, pageLink));
    }

    private List<Customer> filterCustomersByReadPermission(List<Customer> customers) {
        return customers.stream().filter(customer -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.CUSTOMER, Operation.READ, customer.getId(), customer);
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
    }
}
