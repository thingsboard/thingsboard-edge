///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
  Component,
  ComponentFactoryResolver,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  ViewChildren,
  QueryList,
  ContentChildren, AfterViewInit
} from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { BaseData, HasId } from '@shared/models/base-data';
import {
  EntityType,
  EntityTypeResource,
  EntityTypeTranslation
} from '@shared/models/entity-type.models';
import { NgForm } from '@angular/forms';
import { EntityComponent } from './entity.component';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { Subscription } from 'rxjs';
import { MatTabGroup, MatTab } from '@angular/material/tabs';
import { EntityTabsComponent } from '@home/components/entity/entity-tabs.component';

@Component({
  selector: 'tb-entity-details-panel',
  templateUrl: './entity-details-panel.component.html',
  styleUrls: ['./entity-details-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EntityDetailsPanelComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input() entitiesTableConfig: EntityTableConfig<BaseData<HasId>>;

  @Output()
  closeEntityDetails = new EventEmitter<void>();

  @Output()
  entityUpdated = new EventEmitter<BaseData<HasId>>();

  @Output()
  entityAction = new EventEmitter<EntityAction<BaseData<HasId>>>();

  entityComponent: EntityComponent<BaseData<HasId>>;
  entityTabsComponent: EntityTabsComponent<BaseData<HasId>>;
  detailsForm: NgForm;

  isEditValue = false;
  selectedTab = 0;

  entityTypes = EntityType;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  @ViewChild('entityTabs', {static: true}) entityTabsAnchor: TbAnchorComponent;

  @ViewChild(MatTabGroup, {static: true}) matTabGroup: MatTabGroup;

  @ViewChildren(MatTab) inclusiveTabs: QueryList<MatTab>;

  translations: EntityTypeTranslation;
  resources: EntityTypeResource;
  entity: BaseData<HasId>;

  private currentEntityId: HasId;
  private entityActionSubscription: Subscription;

  constructor(protected store: Store<AppState>,
              private componentFactoryResolver: ComponentFactoryResolver) {
    super(store);
  }

  @Input()
  set entityId(entityId: HasId) {
    if (entityId && entityId !== this.currentEntityId) {
      this.currentEntityId = entityId;
      this.reload();
    }
  }

  set isEdit(val: boolean) {
    this.isEditValue = val;
    this.entityComponent.isEdit = val;
    if (this.entityTabsComponent) {
      this.entityTabsComponent.isEdit = val;
    }
  }

  get isEdit() {
    return this.isEditValue;
  }

  ngOnInit(): void {
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.buildEntityComponent();
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.entityActionSubscription) {
      this.entityActionSubscription.unsubscribe();
    }
  }

  buildEntityComponent() {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.entitiesTableConfig.entityComponent);
    const viewContainerRef = this.entityDetailsFormAnchor.viewContainerRef;
    viewContainerRef.clear();
    const componentRef = viewContainerRef.createComponent(componentFactory);
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = this.isEdit;
    this.entityComponent.entitiesTableConfig = this.entitiesTableConfig;
    this.detailsForm = this.entityComponent.entityNgForm;
    this.entityActionSubscription = this.entityComponent.entityAction.subscribe((action) => {
      this.entityAction.emit(action);
    });
    this.buildEntityTabsComponent();
  }

  buildEntityTabsComponent() {
    if (this.entitiesTableConfig.entityTabsComponent) {
      const componentTabsFactory = this.componentFactoryResolver.resolveComponentFactory(this.entitiesTableConfig.entityTabsComponent);
      const viewContainerRef = this.entityTabsAnchor.viewContainerRef;
      viewContainerRef.clear();
      const componentTabsRef = viewContainerRef.createComponent(componentTabsFactory);
      this.entityTabsComponent = componentTabsRef.instance;
      this.entityTabsComponent.isEdit = this.isEdit;
      this.entityTabsComponent.entitiesTableConfig = this.entitiesTableConfig;
    }
  }

  reload(): void {
    this.isEdit = false;
    this.entitiesTableConfig.loadEntity(this.currentEntityId).subscribe(
      (entity) => {
        this.entity = entity;
        this.entityComponent.entity = entity;
        if (this.entityTabsComponent) {
          this.entityTabsComponent.entity = entity;
        }
      }
    );
  }

  onCloseEntityDetails() {
    this.closeEntityDetails.emit();
  }

  onToggleEditMode(isEdit: boolean) {
    this.isEdit = isEdit;
    if (!this.isEdit) {
      this.entityComponent.entity = this.entity;
      if (this.entityTabsComponent) {
        this.entityTabsComponent.entity = this.entity;
      }
    } else {
      this.selectedTab = 0;
    }
  }

  saveEntity() {
    if (this.detailsForm.valid) {
      const editingEntity = {...this.entity, ...this.entityComponent.entityFormValue()};
      this.entitiesTableConfig.saveEntity(editingEntity).subscribe(
        (entity) => {
          this.entity = entity;
          this.entityComponent.entity = entity;
          if (this.entityTabsComponent) {
            this.entityTabsComponent.entity = entity;
          }
          this.isEdit = false;
          this.entityUpdated.emit(this.entity);
        }
      );
    }
  }

  ngAfterViewInit(): void {
    if (this.entityTabsComponent) {
      this.entityTabsComponent.entityTabsChanged.subscribe(
        (entityTabs) => {
          if (entityTabs) {
            this.matTabGroup._tabs.reset([...this.inclusiveTabs.toArray(), ...entityTabs]);
            this.matTabGroup._tabs.notifyOnChanges();
          }
        }
      );
    }
  }

}
