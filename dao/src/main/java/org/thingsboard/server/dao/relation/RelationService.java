/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 27.04.17.
 */
public interface RelationService {

    ListenableFuture<Boolean> checkRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    EntityRelation getRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<EntityRelation> getRelationAsync(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    boolean saveRelation(EntityRelation relation);

    ListenableFuture<Boolean> saveRelationAsync(EntityRelation relation);

    boolean deleteRelation(EntityRelation relation);

    ListenableFuture<Boolean> deleteRelationAsync(EntityRelation relation);

    boolean deleteRelation(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<Boolean> deleteRelationAsync(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    void deleteEntityRelations(EntityId entity);

    ListenableFuture<Void> deleteEntityRelationsAsync(EntityId entity);

    List<EntityRelation> findByFrom(EntityId from, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByFromAsync(EntityId from, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(EntityId from, RelationTypeGroup typeGroup);

    List<EntityRelation> findByFromAndType(EntityId from, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(EntityId from, String relationType, RelationTypeGroup typeGroup);

    List<EntityRelation> findByTo(EntityId to, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByToAsync(EntityId to, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelationInfo>> findInfoByTo(EntityId to, RelationTypeGroup typeGroup);

    List<EntityRelation> findByToAndType(EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(EntityId to, String relationType, RelationTypeGroup typeGroup);

    ListenableFuture<List<EntityRelation>> findByQuery(EntityRelationsQuery query);

    ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(EntityRelationsQuery query);

//    TODO: This method may be useful for some validations in the future
//    ListenableFuture<Boolean> checkRecursiveRelation(EntityId from, EntityId to);

}
