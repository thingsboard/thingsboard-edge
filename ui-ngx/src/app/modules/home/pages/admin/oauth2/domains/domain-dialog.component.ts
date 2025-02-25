///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import { AfterViewInit, Component, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef } from '@angular/material/dialog';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { Domain } from '@shared/models/oauth2.models';
import { DomainService } from '@core/http/domain.service';
import { DomainComponent } from '@home/pages/admin/oauth2/domains/domain.component';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-mobile-app-dialog',
  templateUrl: './domain-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DomainDialogComponent}],
  styleUrls: []
})
export class DomainDialogComponent extends DialogComponent<DomainDialogComponent, Domain> implements OnDestroy, AfterViewInit, ErrorStateMatcher {

  submitted = false;

  addTitle = entityTypeTranslations.get(EntityType.DOMAIN).add;
  helpId = entityTypeResources.get(EntityType.DOMAIN).helpLinkId;

  @ViewChild('domainComponent', {static: true}) domainComponent: DomainComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              protected dialogRef: MatDialogRef<DomainDialogComponent, Domain>,
              private domainService: DomainService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher) {
    super(store, router, dialogRef);
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.domainComponent.isEdit = true;
    }, 0);
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save() {
    this.submitted = true;
    if (this.domainComponent.entityForm.valid) {
      const oauth2ClientIds = this.domainComponent.entityFormValue().oauth2ClientInfos as Array<string> || [];
      this.domainService.saveDomain(
        this.domainComponent.entityFormValue(),
        oauth2ClientIds
      ).subscribe(
        domain => this.dialogRef.close(domain)
      )
    }
  }
}
