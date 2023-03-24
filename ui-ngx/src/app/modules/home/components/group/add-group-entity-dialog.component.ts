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
import { UntypedFormControl, FormGroupDirective, NgForm, UntypedFormGroup } from '@angular/forms';
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
  detailsForm: UntypedFormGroup;

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

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
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
