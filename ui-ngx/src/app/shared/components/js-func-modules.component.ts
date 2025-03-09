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

import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { AbstractControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { JsFuncModuleRow, moduleValid } from '@shared/components/js-func-module-row.component';

const modulesValidator: ValidatorFn = control => {
  const modulesArray = control.get('modules') as UntypedFormArray;
  const notUniqueControls =
    modulesArray.controls.filter(moduleControl => moduleControl.hasError('moduleAliasNotUnique'));
  if (notUniqueControls.length) {
    return {
      moduleAliasNotUnique: true
    };
  }
  let valid = !modulesArray.controls.some(c => !c.valid);
  valid = valid && control.valid;
  return valid ? null : {
    modules: {
      valid: false,
    },
  };
};

@Component({
  selector: 'tb-js-func-modules',
  templateUrl: './js-func-modules.component.html',
  styleUrls: ['./js-func-modules.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class JsFuncModulesComponent implements OnInit {

  @Input()
  modules: {[alias: string]: string };

  @Input()
  popover: TbPopoverComponent<JsFuncModulesComponent>;

  @Output()
  modulesApplied = new EventEmitter<{[alias: string]: string }>();

  modulesFormGroup: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const modulesControls: Array<AbstractControl> = [];
    if (this.modules && Object.keys(this.modules).length) {
      Object.keys(this.modules).forEach((alias) => {
        const moduleRow: JsFuncModuleRow = {
          alias,
          moduleLink: this.modules[alias]
        };
        modulesControls.push(this.fb.control(moduleRow, []));
      });
   }
    this.modulesFormGroup = this.fb.group({
      modules: this.fb.array(modulesControls)
    }, {validators: modulesValidator});
  }

  cancel() {
    this.popover?.hide();
  }

  applyModules() {
    let moduleRows: JsFuncModuleRow[] = this.modulesFormGroup.get('modules').value;
    if (moduleRows) {
      moduleRows = moduleRows.filter(m => moduleValid(m));
    }
    if (moduleRows?.length) {
      const modules: {[alias: string]: string } = {};
      moduleRows.forEach(row => {
        modules[row.alias] = row.moduleLink;
      });
      this.modulesApplied.emit(modules);
    } else {
      this.modulesApplied.emit(null);
    }
  }

  public moduleAliasUnique(alias: string, index: number): boolean {
    const modulesArray = this.modulesFormGroup.get('modules') as UntypedFormArray;
    for (let i = 0; i < modulesArray.controls.length; i++) {
      if (i !== index) {
        const otherControl = modulesArray.controls[i];
        if (alias === otherControl.value.alias) {
          return false;
        }
      }
    }
    return true;
  }

  modulesFormArray(): UntypedFormArray {
    return this.modulesFormGroup.get('modules') as UntypedFormArray;
  }

  trackByModule(_index: number, moduleControl: AbstractControl): any {
    return moduleControl;
  }

  removeModule(index: number, emitEvent = true) {
    (this.modulesFormGroup.get('modules') as UntypedFormArray).removeAt(index, {emitEvent});
    this.modulesFormGroup.get('modules').markAsDirty({emitEvent});
  }

  addModule() {
    const moduleRow: JsFuncModuleRow = {
      alias: '',
      moduleLink: ''
    };
    const modulesArray = this.modulesFormGroup.get('modules') as UntypedFormArray;
    const moduleControl = this.fb.control(moduleRow, []);
    modulesArray.push(moduleControl);
    this.cd.detectChanges();
  }

}
