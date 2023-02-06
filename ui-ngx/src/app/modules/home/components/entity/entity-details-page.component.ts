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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ComponentFactoryResolver,
  HostBinding,
  Injector,
  OnDestroy,
  OnInit
} from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { ActivatedRoute, Router } from '@angular/router';
import { FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { deepClone, isDefined, isUndefined } from '@core/utils';
import { BroadcastService } from '@core/services/broadcast.service';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { DialogService } from '@core/services/dialog.service';
import { EntityGroupStateInfo } from '@home/models/group/group-entities-table-config.models';
import { IEntityDetailsPageComponent } from '@home/models/entity/entity-details-page-component.models';

@Component({
  selector: 'tb-entity-details-page',
  templateUrl: './entity-details-page.component.html',
  styleUrls: ['./entity-details-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDetailsPageComponent extends EntityDetailsPanelComponent implements IEntityDetailsPageComponent, OnInit, OnDestroy {

  headerTitle: string;
  headerSubtitle: string;

  isReadOnly = false;

  entityGroup: EntityGroupStateInfo<BaseData<HasId>>;

  set entitiesTableConfig(entitiesTableConfig: EntityTableConfig<BaseData<HasId>>) {
    if (this.entitiesTableConfigValue !== entitiesTableConfig) {
      this.entitiesTableConfigValue = entitiesTableConfig;
      if (this.entitiesTableConfigValue) {
        this.entitiesTableConfigValue.setEntityDetailsPage(this);
        this.isEdit = false;
        this.entity = null;
      }
    }
  }

  get entitiesTableConfig(): EntityTableConfig<BaseData<HasId>> {
    return this.entitiesTableConfigValue;
  }

  @HostBinding('class') 'tb-absolute-fill';

  constructor(private route: ActivatedRoute,
              private router: Router,
              protected injector: Injector,
              protected cd: ChangeDetectorRef,
              protected componentFactoryResolver: ComponentFactoryResolver,
              private broadcast: BroadcastService,
              private translate: TranslateService,
              private dialogService: DialogService,
              protected store: Store<AppState>) {
    super(store, injector, cd, componentFactoryResolver);
    if (isDefined(this.route.snapshot.data.entityGroup) && isUndefined(this.route.snapshot.data.entitiesTableConfig)) {
      this.entityGroup = this.route.snapshot.data.entityGroup;
      this.entitiesTableConfig = this.entityGroup.entityGroupConfig;
    } else {
      this.entitiesTableConfig = this.route.snapshot.data.entitiesTableConfig;
    }
  }

  ngOnInit() {
    this.headerSubtitle = '';
    this.headerSubtitle = this.translate.instant(this.entitiesTableConfig.entityTranslations.details);
    super.init();
    this.entityComponent.isDetailsPage = true;
    this.subscriptions.push(this.entityAction.subscribe((action) => {
      if (action.action === 'delete') {
        this.deleteEntity(action.event, action.entity);
      }
    }));
    this.subscriptions.push(this.route.paramMap.subscribe( paramMap => {
      if (this.entitiesTableConfig) {
        const entityType = this.entitiesTableConfig.entityType;
        const id = paramMap.get('entityId');
        this.currentEntityId = { id, entityType };
        this.reload();
        this.selectedTab = 0;
      }
    }));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  reload(): void {
    this.reloadEntity().subscribe(() => {
      this.onUpdateEntity();
    });
  }

  onToggleDetailsEditMode() {
    if (this.isEdit) {
      this.entityComponent.entity = this.entity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.entity;
      }
      this.isEdit = !this.isEdit;
    } else {
      this.isEdit = !this.isEdit;
      this.editingEntity = deepClone(this.entity);
      this.entityComponent.entity = this.editingEntity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.editingEntity;
      }
      if (this.entitiesTableConfig.hideDetailsTabsOnEdit) {
        this.selectedTab = 0;
      }
    }
  }

  onApplyDetails() {
    this.saveEntity(false).subscribe((entity) => {
      if (entity) {
        this.onUpdateEntity();
      }
    });
  }

  confirmForm(): FormGroup {
    return this.detailsForm;
  }

  private onUpdateEntity() {
    this.broadcast.broadcast('updateBreadcrumb');
    this.isReadOnly = this.entitiesTableConfig.detailsReadonly(this.entity);
    this.headerTitle = this.entitiesTableConfig.entityTitle(this.entity);
  }

  private deleteEntity($event: Event, entity: BaseData<HasId>) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.entitiesTableConfig.deleteEntityTitle(entity),
      this.entitiesTableConfig.deleteEntityContent(entity),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((result) => {
      if (result) {
        this.entitiesTableConfig.deleteEntity(entity.id).subscribe(
          () => {
            this.router.navigate(['../'], {relativeTo: this.route});
          }
        );
      }
    });
  }
}
