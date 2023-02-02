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

import { Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { MatHorizontalStepper } from '@angular/material/stepper';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of, Subscription } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';
import { ErrorStateMatcher } from '@angular/material/core';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { EntityGroupInfo, ShareGroupRequest } from '@shared/models/entity-group.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { EntityGroupService } from '@core/http/entity-group.service';

export interface EntityGroupWizardDialogResult {
  entityGroup: EntityGroupInfo;
  shared: boolean;
}

@Component({
  selector: 'tb-entity-group-wizard',
  templateUrl: './entity-group-wizard-dialog.component.html',
  providers: [],
  styleUrls: ['./entity-group-wizard-dialog.component.scss']
})
export class EntityGroupWizardDialogComponent extends
  DialogComponent<EntityGroupWizardDialogComponent, EntityGroupWizardDialogResult> implements OnDestroy, ErrorStateMatcher {

  @ViewChild('addEntityGroupWizardStepper', {static: true}) addEntityGroupWizardStepper: MatHorizontalStepper;

  resource = Resource;

  operation = Operation;

  selectedIndex = 0;

  showNext = true;

  entityType = EntityType;

  entityGroupWizardFormGroup: FormGroup;

  shareEntityGroupFormGroup: FormGroup;

  labelPosition = 'end';

  entitiesTableConfig = this.data.entitiesTableConfig;

  private subscriptions: Subscription[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityDialogData<EntityGroupInfo>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EntityGroupWizardDialogComponent, EntityGroupWizardDialogResult>,
              private entityGroupService: EntityGroupService,
              private userPermissionService: UserPermissionsService,
              private breakpointObserver: BreakpointObserver,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.entityGroupWizardFormGroup = this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(255)]],
        description: ['']
      }
    );

    const shareGroupRequest: ShareGroupRequest = {
      ownerId: null,
      allUserGroup: true,
      readElseWrite: true
    };

    this.shareEntityGroupFormGroup = this.fb.group({
      shareEntityGroup: [false],
      shareGroupRequest: [shareGroupRequest, Validators.required]
    });

    this.subscriptions.push(this.shareEntityGroupFormGroup.get('shareEntityGroup').valueChanges.subscribe(
      (shareEntityGroup: boolean) => {
        if (shareEntityGroup) {
          this.shareEntityGroupFormGroup.get('shareGroupRequest').setValidators(Validators.required);
        } else {
          this.shareEntityGroupFormGroup.get('shareGroupRequest').clearValidators();
        }
        this.shareEntityGroupFormGroup.get('shareGroupRequest').updateValueAndValidity();
      }
    ));

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

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addEntityGroupWizardStepper.previous();
  }

  nextStep(): void {
    this.addEntityGroupWizardStepper.next();
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'entity-group.entity-group-details';
      case 1:
        return 'entity-group.share';
    }
  }

  get maxStepperIndex(): number {
    return this.addEntityGroupWizardStepper?._steps?.length - 1;
  }

  add(): void {
    if (this.allValid()) {
      this.createEntityGroup().pipe(
        mergeMap(entityGroup => this.shareEntityGroup(entityGroup).pipe(
            map((shared) => {
                return {entityGroup, shared} as EntityGroupWizardDialogResult;
              }
            )
          )
        )
      ).subscribe(
        (entityGroup) => {
          this.dialogRef.close(entityGroup);
        }
      );
    }
  }

  private createEntityGroup(): Observable<EntityGroupInfo> {
    const entityGroup = {
      name: this.entityGroupWizardFormGroup.get('name').value.trim(),
      additionalInfo: {
        description: this.entityGroupWizardFormGroup.get('description').value.trim()
      },
      customerId: null
    } as EntityGroupInfo;
    return this.entitiesTableConfig.saveEntity(entityGroup);
  }

  private shareEntityGroup(entityGroup: EntityGroupInfo): Observable<boolean> {
    const shareEntityGroup: boolean = this.shareEntityGroupFormGroup.get('shareEntityGroup').value;
    if (shareEntityGroup) {
      const shareGroupRequest = this.shareEntityGroupFormGroup.get('shareGroupRequest').value;
      return this.entityGroupService.shareEntityGroup(entityGroup.id.id, shareGroupRequest).pipe(
        map(() => true
      ));
    } else {
      return of(false);
    }
  }

  allValid(): boolean {
    if (this.addEntityGroupWizardStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addEntityGroupWizardStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    } )) {
      return false;
    } else {
      return true;
    }
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
