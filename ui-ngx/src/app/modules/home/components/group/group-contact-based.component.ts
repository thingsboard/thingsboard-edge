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

import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { ContactBased } from '@shared/models/contact-based.model';
import { AfterViewInit, ChangeDetectorRef, Directive } from '@angular/core';
import { POSTAL_CODE_PATTERNS } from '@home/models/contact.models';
import { HasId } from '@shared/models/base-data';
import { GroupEntityComponent } from '@home/components/group/group-entity.component';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Directive()
export abstract class GroupContactBasedComponent<T extends ContactBased<HasId>> extends GroupEntityComponent<T> implements AfterViewInit {

  protected constructor(protected store: Store<AppState>,
                        protected fb: UntypedFormBuilder,
                        protected entityValue: T,
                        protected entitiesTableConfigValue: EntityTableConfig<T> | GroupEntityTableConfig<T>,
                        protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: T): UntypedFormGroup {
    const entityForm = this.buildEntityForm(entity);
    entityForm.addControl('country', this.fb.control(entity ? entity.country : '', []));
    entityForm.addControl('city', this.fb.control(entity ? entity.city : '', []));
    entityForm.addControl('state', this.fb.control(entity ? entity.state : '', []));
    entityForm.addControl('zip', this.fb.control(entity ? entity.zip : '',
      this.zipValidators(entity ? entity.country : '')
    ));
    entityForm.addControl('address', this.fb.control(entity ? entity.address : '', []));
    entityForm.addControl('address2', this.fb.control(entity ? entity.address2 : '', []));
    entityForm.addControl('phone', this.fb.control(entity ? entity.phone : '', []));
    entityForm.addControl('email', this.fb.control(entity ? entity.email : '', [Validators.email]));
    return entityForm;
  }

  updateForm(entity: T) {
    this.updateEntityForm(entity);
    this.entityForm.patchValue({country: entity.country});
    this.entityForm.patchValue({city: entity.city});
    this.entityForm.patchValue({state: entity.state});
    this.entityForm.get('zip').setValidators(this.zipValidators(entity.country));
    this.entityForm.patchValue({zip: entity.zip});
    this.entityForm.patchValue({address: entity.address});
    this.entityForm.patchValue({address2: entity.address2});
    this.entityForm.patchValue({phone: entity.phone});
    this.entityForm.patchValue({email: entity.email});
  }

  ngAfterViewInit() {
    this.entityForm.get('country').valueChanges.subscribe(
      (country) => {
        this.entityForm.get('zip').setValidators(this.zipValidators(country));
        this.entityForm.get('zip').updateValueAndValidity({onlySelf: true});
        this.entityForm.get('zip').markAsTouched({onlySelf: true});
      }
    );
  }

  zipValidators(country: string): ValidatorFn[] {
    const zipValidators = [];
    if (country && POSTAL_CODE_PATTERNS[country]) {
      const postalCodePattern = POSTAL_CODE_PATTERNS[country];
      zipValidators.push(Validators.pattern(postalCodePattern));
    }
    return zipValidators;
  }

  abstract buildEntityForm(entity: T): UntypedFormGroup;

  abstract updateEntityForm(entity: T);

}
