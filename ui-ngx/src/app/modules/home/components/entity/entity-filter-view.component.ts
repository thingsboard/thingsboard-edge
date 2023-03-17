///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AliasFilterType, EntityAliasFilter } from '@shared/models/alias.models';
import { AliasEntityType, EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-entity-filter-view',
  templateUrl: './entity-filter-view.component.html',
  styleUrls: ['./entity-filter-view.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => EntityFilterViewComponent),
      multi: true
    }
  ]
})
export class EntityFilterViewComponent implements ControlValueAccessor {

  constructor(private translate: TranslateService) {}

  filterDisplayValue: string;
  filter: EntityAliasFilter;

  registerOnChange(fn: any): void {
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
  }

  writeValue(filter: EntityAliasFilter): void {
    this.filter = filter;
    if (this.filter && this.filter.type) {
      let entityType: EntityType | AliasEntityType;
      let prefix: string;
      let allEntitiesText;
      let anyRelationText;
      let relationTypeText;
      let rootEntityText;
      let directionText;
      let count: number;
      switch (this.filter.type) {
        case AliasFilterType.singleEntity:
          entityType = this.filter.singleEntity.entityType;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).list,
            {count: 1});
          break;
        case AliasFilterType.entityGroup:
          if (this.filter.groupStateEntity) {
            this.filterDisplayValue = this.translate.instant('alias.entities-of-group-state-entity');
          } else {
            entityType = this.filter.groupType;
            this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).group);
          }
          break;
        case AliasFilterType.entityList:
          entityType = this.filter.entityType;
          count = this.filter.entityList.length;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).list,
            {count});
          break;
        case AliasFilterType.entityName:
          entityType = this.filter.entityType;
          prefix = this.filter.entityNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).nameStartsWith,
            {prefix});
          break;
        case AliasFilterType.entityType:
          entityType = this.filter.entityType;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).typePlural);
          break;
        case AliasFilterType.entityGroupList:
          entityType = this.filter.groupType;
          count = this.filter.entityGroupList.length;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).groupList,
            {count});
          break;
        case AliasFilterType.entityGroupName:
          entityType = this.filter.groupType;
          prefix = this.filter.entityGroupNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).groupNameStartsWith,
            {prefix});
          break;
        case AliasFilterType.entitiesByGroupName:
          entityType = this.filter.groupType;
          prefix = this.filter.entityGroupNameFilter;
          this.filterDisplayValue = this.translate.instant(entityTypeTranslations.get(entityType).group) + ': ' + prefix;
          break;
        case AliasFilterType.stateEntity:
          this.filterDisplayValue = this.translate.instant('alias.filter-type-state-entity-description');
          break;
        case AliasFilterType.stateEntityOwner:
          this.filterDisplayValue = this.translate.instant('alias.filter-type-state-entity-owner-description');
          break;
        case AliasFilterType.assetType:
          const assetType = this.filter.assetType;
          prefix = this.filter.assetNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-type-and-name-description',
              {assetType, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-type-description',
              {assetType});
          }
          break;
        case AliasFilterType.deviceType:
          const deviceType = this.filter.deviceType;
          prefix = this.filter.deviceNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-type-and-name-description',
              {deviceType, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-type-description',
              {deviceType});
          }
          break;
        case AliasFilterType.entityViewType:
          const entityView = this.filter.entityViewType;
          prefix = this.filter.entityViewNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-type-and-name-description',
              {entityView, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-type-description',
              {entityView});
          }
          break;
        case AliasFilterType.edgeType:
          const edgeType = this.filter.edgeType;
          prefix = this.filter.edgeNameFilter;
          if (prefix && prefix.length) {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-type-and-name-description',
              {edgeType, prefix});
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-type-description',
              {edgeType});
          }
          break;
        case AliasFilterType.relationsQuery:
          allEntitiesText = this.translate.instant('alias.all-entities');
          anyRelationText = this.translate.instant('alias.any-relation');
          if (this.filter.rootStateEntity) {
            rootEntityText = this.translate.instant('alias.state-entity');
          } else {
            rootEntityText = this.translate.instant(entityTypeTranslations.get(this.filter.rootEntity.entityType).type);
          }
          directionText = this.translate.instant('relation.direction-type.' + this.filter.direction);
          const relationFilters = this.filter.filters;
          if (relationFilters && relationFilters.length) {
            const relationFiltersDisplayValues = [];
            relationFilters.forEach((relationFilter) => {
              let entitiesText;
              if (relationFilter.entityTypes && relationFilter.entityTypes.length) {
                const entitiesNamesList = [];
                relationFilter.entityTypes.forEach((filterEntityType) => {
                  entitiesNamesList.push(
                    this.translate.instant(entityTypeTranslations.get(filterEntityType).typePlural)
                  );
                });
                entitiesText = entitiesNamesList.join(', ');
              } else {
                entitiesText = allEntitiesText;
              }
              if (relationFilter.relationType && relationFilter.relationType.length) {
                relationTypeText = `'${relationFilter.relationType}'`;
              } else {
                relationTypeText = anyRelationText;
              }
              const relationFilterDisplayValue = this.translate.instant('alias.filter-type-relations-query-description',
                {
                  entities: entitiesText,
                  relationType: relationTypeText,
                  direction: directionText,
                  rootEntity: rootEntityText
                }
              );
              relationFiltersDisplayValues.push(relationFilterDisplayValue);
            });
            this.filterDisplayValue = relationFiltersDisplayValues.join(', ');
          } else {
            this.filterDisplayValue = this.translate.instant('alias.filter-type-relations-query-description',
              {
                entities: allEntitiesText,
                relationType: anyRelationText,
                direction: directionText,
                rootEntity: rootEntityText
              }
            );
          }
          break;
        case AliasFilterType.assetSearchQuery:
        case AliasFilterType.deviceSearchQuery:
        case AliasFilterType.edgeSearchQuery:
        case AliasFilterType.entityViewSearchQuery:
          allEntitiesText = this.translate.instant('alias.all-entities');
          anyRelationText = this.translate.instant('alias.any-relation');
          if (this.filter.rootStateEntity) {
            rootEntityText = this.translate.instant('alias.state-entity');
          } else {
            rootEntityText = this.translate.instant(entityTypeTranslations.get(this.filter.rootEntity.entityType).type);
          }
          directionText = this.translate.instant('relation.direction-type.' + this.filter.direction);
          if (this.filter.relationType && this.filter.relationType.length) {
            relationTypeText = `'${filter.relationType}'`;
          } else {
            relationTypeText = anyRelationText;
          }

          const translationValues: any = {
            relationType: relationTypeText,
            direction: directionText,
            rootEntity: rootEntityText
          };

          if (this.filter.type === AliasFilterType.assetSearchQuery) {
            const assetTypesQuoted = [];
            this.filter.assetTypes.forEach((filterAssetType) => {
              assetTypesQuoted.push(`'${filterAssetType}'`);
            });
            const assetTypesText = assetTypesQuoted.join(', ');
            translationValues.assetTypes = assetTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-asset-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.deviceSearchQuery) {
            const deviceTypesQuoted = [];
            this.filter.deviceTypes.forEach((filterDeviceType) => {
              deviceTypesQuoted.push(`'${filterDeviceType}'`);
            });
            const deviceTypesText = deviceTypesQuoted.join(', ');
            translationValues.deviceTypes = deviceTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-device-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.edgeSearchQuery) {
            const edgeTypesQuoted = [];
            this.filter.edgeTypes.forEach((filterEdgeType) => {
              edgeTypesQuoted.push(`'${filterEdgeType}'`);
            });
            const edgeTypesText = edgeTypesQuoted.join(', ');
            translationValues.edgeTypes = edgeTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-edge-search-query-description',
              translationValues
            );
          } else if (this.filter.type === AliasFilterType.entityViewSearchQuery) {
            const entityViewTypesQuoted = [];
            this.filter.entityViewTypes.forEach((filterEntityViewType) => {
              entityViewTypesQuoted.push(`'${filterEntityViewType}'`);
            });
            const entityViewTypesText = entityViewTypesQuoted.join(', ');
            translationValues.entityViewTypes = entityViewTypesText;
            this.filterDisplayValue = this.translate.instant('alias.filter-type-entity-view-search-query-description',
              translationValues
            );
          }
          break;
        case AliasFilterType.schedulerEvent:
          if (this.filter.eventType) {
            const interpolateParams = {
              eventType: this.filter.eventType
            };
            if (this.filter.originator || this.filter.originatorStateEntity) {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-type-originator-description',
                interpolateParams
              );
            } else {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-type-description',
                interpolateParams
              );
            }
          } else {
            if (this.filter.originator || this.filter.originatorStateEntity) {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event-originator-description');
            } else {
              this.filterDisplayValue = this.translate.instant('alias.filter-type-scheduler-event');
            }
          }
          break;
        default:
          this.filterDisplayValue = this.filter.type;
          break;
      }
    } else {
      this.filterDisplayValue = '';
    }
  }
}
