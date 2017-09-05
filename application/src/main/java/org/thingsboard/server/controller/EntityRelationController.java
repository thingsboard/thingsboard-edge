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
package org.thingsboard.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardException;

import java.util.List;


@RestController
@RequestMapping("/api")
public class EntityRelationController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveRelation(@RequestBody EntityRelation relation) throws ThingsboardException {
        try {
            checkNotNull(relation);
            checkEntityId(relation.getFrom());
            checkEntityId(relation.getTo());
            if (relation.getTypeGroup() == null) {
                relation.setTypeGroup(RelationTypeGroup.COMMON);
            }
            relationService.saveRelation(relation).get();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.DELETE, params = {"fromId", "fromType", "relationType", "toId", "toType"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelation(@RequestParam("fromId") String strFromId,
                               @RequestParam("fromType") String strFromType,
                               @RequestParam("relationType") String strRelationType,
                               @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                               @RequestParam("toId") String strToId, @RequestParam("toType") String strToType) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        checkParameter("relationType", strRelationType);
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(fromId);
        checkEntityId(toId);
        RelationTypeGroup relationTypeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            Boolean found = relationService.deleteRelation(fromId, toId, strRelationType, relationTypeGroup).get();
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.DELETE, params = {"id", "type"})
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRelations(@RequestParam("entityId") String strId,
                                @RequestParam("entityType") String strType) throws ThingsboardException {
        checkParameter("entityId", strId);
        checkParameter("entityType", strType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strType, strId);
        checkEntityId(entityId);
        try {
            relationService.deleteEntityRelations(entityId).get();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relation", method = RequestMethod.GET, params = {"fromId", "fromType", "relationType", "toId", "toType"})
    @ResponseBody
    public EntityRelation getRelation(@RequestParam("fromId") String strFromId,
                              @RequestParam("fromType") String strFromType,
                              @RequestParam("relationType") String strRelationType,
                              @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup,
                              @RequestParam("toId") String strToId, @RequestParam("toType") String strToType) throws ThingsboardException {
        try {
            checkParameter("fromId", strFromId);
            checkParameter("fromType", strFromType);
            checkParameter("relationType", strRelationType);
            checkParameter("toId", strToId);
            checkParameter("toType", strToType);
            EntityId fromId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
            EntityId toId = EntityIdFactory.getByTypeAndId(strToType, strToId);
            checkEntityId(fromId);
            checkEntityId(toId);
            RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
            return checkNotNull(relationService.getRelation(fromId, toId, strRelationType, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"fromId", "fromType"})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam("fromId") String strFromId,
                                           @RequestParam("fromType") String strFromType,
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findByFrom(entityId, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {"fromId", "fromType"})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByFrom(@RequestParam("fromId") String strFromId,
                                                   @RequestParam("fromType") String strFromType,
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findInfoByFrom(entityId, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"fromId", "fromType", "relationType"})
    @ResponseBody
    public List<EntityRelation> findByFrom(@RequestParam("fromId") String strFromId,
                                           @RequestParam("fromType") String strFromType,
                                           @RequestParam("relationType") String strRelationType,
                                           @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("fromId", strFromId);
        checkParameter("fromType", strFromType);
        checkParameter("relationType", strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strFromType, strFromId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findByFromAndType(entityId, strRelationType, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"toId", "toType"})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam("toId") String strToId,
                                         @RequestParam("toType") String strToType,
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findByTo(entityId, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.GET, params = {"toId", "toType"})
    @ResponseBody
    public List<EntityRelationInfo> findInfoByTo(@RequestParam("toId") String strToId,
                                                   @RequestParam("toType") String strToType,
                                                   @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findInfoByTo(entityId, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.GET, params = {"toId", "toType", "relationType"})
    @ResponseBody
    public List<EntityRelation> findByTo(@RequestParam("toId") String strToId,
                                         @RequestParam("toType") String strToType,
                                         @RequestParam("relationType") String strRelationType,
                                         @RequestParam(value = "relationTypeGroup", required = false) String strRelationTypeGroup) throws ThingsboardException {
        checkParameter("toId", strToId);
        checkParameter("toType", strToType);
        checkParameter("relationType", strRelationType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strToType, strToId);
        checkEntityId(entityId);
        RelationTypeGroup typeGroup = parseRelationTypeGroup(strRelationTypeGroup, RelationTypeGroup.COMMON);
        try {
            return checkNotNull(relationService.findByToAndType(entityId, strRelationType, typeGroup).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelation> findByQuery(@RequestBody EntityRelationsQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId());
        try {
            return checkNotNull(relationService.findByQuery(query).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/relations/info", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityRelationInfo> findInfoByQuery(@RequestBody EntityRelationsQuery query) throws ThingsboardException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getFilters());
        checkEntityId(query.getParameters().getEntityId());
        try {
            return checkNotNull(relationService.findInfoByQuery(query).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private RelationTypeGroup parseRelationTypeGroup(String strRelationTypeGroup, RelationTypeGroup defaultValue) {
        RelationTypeGroup result = defaultValue;
        if (strRelationTypeGroup != null && strRelationTypeGroup.trim().length()>0) {
            try {
                result = RelationTypeGroup.valueOf(strRelationTypeGroup);
            } catch (IllegalArgumentException e) {
                result = defaultValue;
            }
        }
        return result;
    }

}
