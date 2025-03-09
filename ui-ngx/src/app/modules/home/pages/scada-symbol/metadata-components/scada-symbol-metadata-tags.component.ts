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

import {
  Component,
  DestroyRef,
  forwardRef,
  HostBinding,
  Input,
  OnChanges,
  OnInit,
  QueryList,
  SimpleChanges,
  ViewChildren,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { ScadaSymbolTag } from '@home/components/widget/lib/scada/scada-symbol.models';
import {
  ScadaSymbolMetadataTagComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag.component';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const tagIsEmpty = (tag: ScadaSymbolTag): boolean =>
  !tag.stateRenderFunction && !tag.actions?.click?.actionFunction;

@Component({
  selector: 'tb-scada-symbol-metadata-tags',
  templateUrl: './scada-symbol-metadata-tags.component.html',
  styleUrls: ['./scada-symbol-metadata-tags.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagsComponent implements ControlValueAccessor, OnInit, Validator, OnChanges {

  @HostBinding('style.display') styleDisplay = 'flex';
  @HostBinding('style.overflow') styleOverflow = 'hidden';

  @ViewChildren(ScadaSymbolMetadataTagComponent)
  metadataTags: QueryList<ScadaSymbolMetadataTagComponent>;

  @Input()
  disabled: boolean;

  @Input()
  tags: string[];

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  clickActionFunctionCompleter: TbEditorCompleter;

  tagsFormGroup: UntypedFormGroup;

  private modelValue: ScadaSymbolTag[];

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
  }

  ngOnInit() {
    this.tagsFormGroup = this.fb.group({
      tags: this.fb.array([])
    });
    const tagsResult = this.setupTags();
    this.tagsFormGroup.setControl('tags', this.prepareTagsFormArray(tagsResult.tags), {emitEvent: false});

    this.tagsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(
      () => {
        let value: ScadaSymbolTag[] = this.tagsFormGroup.get('tags').value;
        if (value) {
          value = value.filter(t => !tagIsEmpty(t));
        }
        this.modelValue = value;
        this.propagateChange(this.modelValue);
      }
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['tags'].includes(propName)) {
          const tagsResult = this.setupTags(this.modelValue);
          const tagsControls = this.prepareTagsFormArray(tagsResult.tags);
          if (tagsResult.emitEvent) {
            setTimeout(() => {
              this.tagsFormGroup.setControl('tags', tagsControls, {emitEvent: true});
              this.setDisabledState(this.disabled);
            });
          } else {
            this.tagsFormGroup.setControl('tags', tagsControls, {emitEvent: false});
            this.setDisabledState(this.disabled);
          }
        }
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tagsFormGroup.disable({emitEvent: false});
    } else {
      this.tagsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolTag[] | undefined): void {
    this.modelValue = value || [];
    const tagsResult= this.setupTags(this.modelValue);
    this.tagsFormGroup.setControl('tags', this.prepareTagsFormArray(tagsResult.tags), {emitEvent: false});
    this.setDisabledState(this.disabled);
  }

  public validate(_c: UntypedFormControl) {
    const valid = this.tagsFormGroup.valid;
    return valid ? null : {
      tags: {
        valid: false,
      },
    };
  }

  tagsFormArray(): UntypedFormArray {
    return this.tagsFormGroup.get('tags') as UntypedFormArray;
  }

  trackByTag(_index: number, tagControl: AbstractControl): any {
    return tagControl;
  }

  editTagStateRenderFunction(tag: string): void {
    setTimeout(() => {
      const tags: ScadaSymbolTag[] = this.tagsFormGroup.get('tags').value;
      const index = tags.findIndex(t => t.tag === tag);
      const tagComponent = this.metadataTags.get(index);
      tagComponent?.editTagStateRenderFunction();
    });
  }

  editTagClickAction(tag: string): void {
    setTimeout(() => {
      const tags: ScadaSymbolTag[] = this.tagsFormGroup.get('tags').value;
      const index = tags.findIndex(t => t.tag === tag);
      const tagComponent = this.metadataTags.get(index);
      tagComponent?.editClickAction();
    });
  }

  private setupTags(existing?: ScadaSymbolTag[]): {tags: ScadaSymbolTag[]; emitEvent: boolean} {
    existing = (existing || []).filter(t => !tagIsEmpty(t));
    const result = (this.tags || []).sort().map(tag => ({
      tag,
      stateRenderFunction: null,
      actions: null
    }));
    for (const tag of existing) {
      const found = result.find(t => t.tag === tag.tag);
      if (found) {
        found.stateRenderFunction = tag.stateRenderFunction;
        if (tag.actions?.click?.actionFunction) {
          found.actions = {
            click: {
              actionFunction: tag.actions.click.actionFunction
            }
          };
        }
      }
    }
    const tagRemoved = !!existing.find(existingTag =>
      !result.find(t => t.tag === existingTag.tag));
    return {
      tags: result,
      emitEvent: tagRemoved
    };
  }

  private prepareTagsFormArray(tags: ScadaSymbolTag[] | undefined): UntypedFormArray {
    const tagsControls: Array<AbstractControl> = [];
    if (tags) {
      tags.forEach((tag) => {
        tagsControls.push(this.fb.control(tag, []));
      });
    }
    return this.fb.array(tagsControls);
  }
}
