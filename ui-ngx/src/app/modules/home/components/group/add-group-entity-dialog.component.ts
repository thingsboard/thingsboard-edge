///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  Component,
  ComponentFactory,
  ComponentFactoryResolver,
  Inject,
  Injector,
  OnInit,
  SkipSelf,
  ViewChild
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormControl, FormGroupDirective, NgForm, FormGroup } from '@angular/forms';
import { EntityType, EntityTypeResource, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { TbAnchorComponent } from '@shared/components/tb-anchor.component';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { AddGroupEntityDialogData } from '@home/models/group/group-entity-component.models';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { Customer } from '@shared/models/customer.model';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityService } from '@core/http/entity.service';

@Component({
  selector: 'tb-add-group-entity-dialog',
  templateUrl: './add-group-entity-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: AddGroupEntityDialogComponent}],
  styleUrls: ['./add-group-entity-dialog.component.scss']
})
export class AddGroupEntityDialogComponent extends
  DialogComponent<AddGroupEntityDialogComponent, BaseData<HasId>> implements OnInit, ErrorStateMatcher {

  entityComponent: GroupEntityComponent<BaseData<HasId>>;
  detailsForm: FormGroup;

  entitiesTableConfig: GroupEntityTableConfig<BaseData<HasId>>;
  entityGroup: EntityGroupInfo;
  entityType: EntityType;
  translations: EntityTypeTranslation;
  resources: EntityTypeResource<BaseData<HasId>>;
  entity: BaseData<EntityId>;

  submitted = false;

  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddGroupEntityDialogData<BaseData<HasId>>,
              public dialogRef: MatDialogRef<AddGroupEntityDialogComponent, BaseData<HasId>>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityService: EntityService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.entitiesTableConfig = this.data.entitiesTableConfig;
    this.entityGroup = this.entitiesTableConfig.entityGroup;
    this.entityType = this.entityGroup.type;
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.entity = {
      id: {
        entityType: this.entityType,
        id: null
      }
    };
    const componentFactory = this.componentFactoryResolver.
         resolveComponentFactory(this.entitiesTableConfig.entityComponent) as ComponentFactory<GroupEntityComponent<BaseData<HasId>>>;
    const viewContainerRef = this.entityDetailsFormAnchor.viewContainerRef;
    viewContainerRef.clear();
    const injector: Injector = Injector.create(
      {
        providers: [
          {
            provide: 'entity',
            useValue: this.entity
          },
          {
            provide: 'entitiesTableConfig',
            useValue: this.entitiesTableConfig
          }
        ],
        parent: this.injector
      }
    );
    const componentRef = viewContainerRef.createComponent(componentFactory, 0, injector);
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = true;
    this.detailsForm = this.entityComponent.entityForm;
  }

  helpLinkId(): string {
    if (this.resources.helpLinkIdForEntity && this.entityComponent.entityForm) {
      return this.resources.helpLinkIdForEntity(this.entityComponent.entityForm.getRawValue());
    } else {
      return this.resources.helpLinkId;
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.submitted = true;
    if (this.detailsForm.valid) {
      this.entity = {...this.entity, ...this.entityComponent.entityFormValue()};
      this.entity.id = {
        entityType: this.entityType,
        id: null
      };
      if (this.entityGroup.ownerId.entityType === EntityType.CUSTOMER) {
        if (this.entityType === EntityType.CUSTOMER) {
          (this.entity as Customer).parentCustomerId = this.entityGroup.ownerId as CustomerId;
        } else {
          this.entity.customerId = this.entityGroup.ownerId as CustomerId;
        }
      }
      const entityGroupId = !this.entityGroup.groupAll ? this.entityGroup.id.id : null;
      this.entityService.saveGroupEntity(this.entity, entityGroupId).subscribe(
        (entity) => {
          this.dialogRef.close(entity);
        }
      );
    }
  }
}
