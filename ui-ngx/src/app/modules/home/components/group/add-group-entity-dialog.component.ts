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
  ComponentFactoryResolver,
  HostBinding,
  Inject,
  Injector,
  OnDestroy,
  OnInit,
  SkipSelf,
  Type,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
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
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { EntityInfoData } from '@shared/models/entity.models';
import { MatStepper } from '@angular/material/stepper';
import { OwnerAndGroupsData } from '@home/components/group/owner-and-groups.component';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { MediaBreakpoints } from '@shared/models/constants';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-add-group-entity-dialog',
  templateUrl: './add-group-entity-dialog.component.html',
  styleUrls: ['./add-group-entity-dialog.component.scss']
})
export class AddGroupEntityDialogComponent extends
  DialogComponent<AddGroupEntityDialogComponent, BaseData<HasId>> implements OnInit, OnDestroy {

  @ViewChild('addGroupEntityWizardStepper', {static: true}) addGroupEntityWizardStepper: MatStepper;
  @ViewChild('detailsFormStep', {static: true, read: ViewContainerRef}) detailsFormStepContainerRef: ViewContainerRef;
  @ViewChild('entityDetailsForm', {static: true}) entityDetailsFormAnchor: TbAnchorComponent;

  @HostBinding('style')
  style = this.data.entitiesTableConfig.addDialogStyle;

  selectedIndex = 0;

  showNext = true;

  labelPosition = 'end';

  entityComponent: GroupEntityComponent<BaseData<HasId>>;
  detailsForm: UntypedFormGroup;
  ownerAndGroupsFormGroup: UntypedFormGroup;

  entitiesTableConfig: EntityTableConfig<BaseData<HasId>> | GroupEntityTableConfig<BaseData<HasId>>;
  customerId: string;
  entityGroup: EntityGroupInfo;
  entityType: EntityType;
  translations: EntityTypeTranslation;
  resources: EntityTypeResource<BaseData<HasId>>;
  entity: BaseData<EntityId>;
  groupMode = false;
  initialOwnerId: EntityId;
  initialGroups: EntityInfoData[];

  private subscriptions: Subscription[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddGroupEntityDialogData<BaseData<HasId>>,
              public dialogRef: MatDialogRef<AddGroupEntityDialogComponent, BaseData<HasId>>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private entityService: EntityService,
              private userPermissionsService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.entitiesTableConfig = this.data.entitiesTableConfig;
    this.customerId = this.entitiesTableConfig.customerId;
    this.groupMode = this.entitiesTableConfig instanceof GroupEntityTableConfig;
    this.entityGroup = this.groupMode ? (this.entitiesTableConfig as GroupEntityTableConfig<BaseData<HasId>>).entityGroup : null;
    this.entityType = this.groupMode ? this.entityGroup.type : this.entitiesTableConfig.entityType;
    this.translations = this.entitiesTableConfig.entityTranslations;
    this.resources = this.entitiesTableConfig.entityResources;
    this.entity = {
      id: {
        entityType: this.entityType,
        id: null
      }
    };
    this.initialGroups = [];
    if (this.groupMode) {
      this.initialOwnerId = this.entityGroup.ownerId;
      if (!this.entityGroup.groupAll) {
        this.initialGroups = [{id: this.entityGroup.id, name: this.entityGroup.name}];
      }
    } else {
      if (this.customerId) {
        this.initialOwnerId = new CustomerId(this.customerId);
      } else {
        this.initialOwnerId = this.userPermissionsService.getUserOwnerId();
      }
    }
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
        parent: this.detailsFormStepContainerRef.injector
      }
    );
    const componentRef = viewContainerRef.createComponent(
      this.entitiesTableConfig.entityComponent as Type<GroupEntityComponent<BaseData<HasId>>>,
      {index: 0, injector});
    this.entityComponent = componentRef.instance;
    this.entityComponent.isEdit = true;
    this.detailsForm = this.entityComponent.entityForm;

    const ownerAndGroups: OwnerAndGroupsData = {
      owner: this.initialOwnerId,
      groups: this.initialGroups
    };

    this.ownerAndGroupsFormGroup = this.fb.group({
      ownerAndGroups: [ownerAndGroups, [Validators.required]]
    });

    this.labelPosition = this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm']) ? 'end' : 'bottom';

    this.subscriptions.push(this.breakpointObserver
      .observe(MediaBreakpoints['gt-sm'])
      .subscribe((state: BreakpointState) => {
          if (state.matches) {
            this.labelPosition = 'end';
          } else {
            this.labelPosition = 'bottom';
          }
        }
      ));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  helpLinkId(): string {
    if (this.resources.helpLinkIdForEntity && this.entityComponent.entityForm) {
      return this.resources.helpLinkIdForEntity(this.entityComponent.entityForm.getRawValue());
    } else {
      return this.resources.helpLinkId;
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addGroupEntityWizardStepper.previous();
  }

  nextStep(): void {
    this.addGroupEntityWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return this.translations.details;
      case 1:
        return 'entity-group.owner-and-groups';
    }
  }

  get maxStepperIndex(): number {
    return this.addGroupEntityWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.allValid()) {
      this.entity = {...this.entity, ...this.entityComponent.entityFormValue()};
      this.entity.id = {
        entityType: this.entityType,
        id: null
      };
      const targetOwnerAndGroups: OwnerAndGroupsData = this.ownerAndGroupsFormGroup.get('ownerAndGroups').value;
      const targetOwner = targetOwnerAndGroups.owner;
      let targetOwnerId: EntityId;
      if ((targetOwner as EntityInfoData).name) {
        targetOwnerId = (targetOwner as EntityInfoData).id;
      } else {
        targetOwnerId = targetOwner as EntityId;
      }
      if (targetOwnerId.entityType === EntityType.CUSTOMER) {
        if (this.entityType === EntityType.CUSTOMER) {
          (this.entity as Customer).parentCustomerId = targetOwnerId as CustomerId;
        } else {
          this.entity.customerId = targetOwnerId as CustomerId;
        }
      }
      const entityGroupIds = targetOwnerAndGroups.groups.map(group => group.id.id);
      this.entityService.saveGroupEntity(this.entity, entityGroupIds).subscribe(
        (entity) => {
          this.dialogRef.close(entity);
        }
      );
    }
  }

  allValid(): boolean {
    return !this.addGroupEntityWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addGroupEntityWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.showNext = false;
    } else {
      this.showNext = true;
    }
  }
}
