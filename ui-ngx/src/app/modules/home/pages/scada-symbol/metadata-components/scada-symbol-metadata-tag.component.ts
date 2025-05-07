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
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewChild,
  ViewContainerRef,
  ViewEncapsulation
} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormControl,
  UntypedFormGroup,
  Validator
} from '@angular/forms';
import { ScadaSymbolTag } from '@home/components/widget/lib/scada/scada-symbol.models';
import { TbEditorCompleter } from '@shared/models/ace/completion.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  ScadaSymbolMetadataTagFunctionPanelComponent
} from '@home/pages/scada-symbol/metadata-components/scada-symbol-metadata-tag-function-panel.component';

@Component({
  selector: 'tb-scada-symbol-metadata-tag',
  templateUrl: './scada-symbol-metadata-tag.component.html',
  styleUrls: ['./scada-symbol-metadata-tag.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ScadaSymbolMetadataTagComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class ScadaSymbolMetadataTagComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('editStateRenderFunctionButton')
  editStateRenderFunctionButton: MatButton;

  @ViewChild('editClickActionButton')
  editClickActionButton: MatButton;

  @Input()
  disabled: boolean;

  @Input()
  elementStateRenderFunctionCompleter: TbEditorCompleter;

  @Input()
  clickActionFunctionCompleter: TbEditorCompleter;

  tagFormGroup: UntypedFormGroup;

  modelValue: ScadaSymbolTag;

  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {
  }

  ngOnInit() {
    this.tagFormGroup = this.fb.group({
      tag: [null, []],
      stateRenderFunction: [null, []],
      clickAction: [null, []]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tagFormGroup.disable({emitEvent: false});
    } else {
      this.tagFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ScadaSymbolTag): void {
    this.modelValue = value;
    const clickAction = value?.actions?.click?.actionFunction;
    this.tagFormGroup.patchValue(
      {
        tag: value?.tag,
        stateRenderFunction: value?.stateRenderFunction,
        clickAction
      }, {emitEvent: false}
    );
  }

  public validate(_c: UntypedFormControl) {
    const valid = this.tagFormGroup.valid;
    return valid ? null : {
      tag: {
        valid: false,
      },
    };
  }

  editTagStateRenderFunction(): void {
    this.openTagFunction('renderFunction', this.editStateRenderFunctionButton);
  }

  editClickAction(): void {
    this.openTagFunction('clickAction', this.editClickActionButton);
  }

  private openTagFunction(tagFunctionType: 'renderFunction' | 'clickAction',
                          button: MatButton) {
    const trigger = button._elementRef.nativeElement;
    trigger.scrollIntoView();
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      let tagFunctionControl: AbstractControl;
      let completer: TbEditorCompleter;
      if (tagFunctionType === 'renderFunction') {
        tagFunctionControl = this.tagFormGroup.get('stateRenderFunction');
        completer = this.elementStateRenderFunctionCompleter;
      } else if (tagFunctionType === 'clickAction') {
        tagFunctionControl = this.tagFormGroup.get('clickAction');
        completer = this.clickActionFunctionCompleter;
      }
      const scadaSymbolTagFunctionPanelPopover =  this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ScadaSymbolMetadataTagFunctionPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          tagFunction: tagFunctionControl.value,
          tagFunctionType,
          tag: this.tagFormGroup.get('tag').value,
          completer,
          disabled: this.disabled
        },
        isModal: true
      });
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.popover = scadaSymbolTagFunctionPanelPopover;
      scadaSymbolTagFunctionPanelPopover.tbComponentRef.instance.tagFunctionApplied.subscribe((tagFunction) => {
        scadaSymbolTagFunctionPanelPopover.hide();
        tagFunctionControl.patchValue(tagFunction, {emitEvent: false});
        this.updateModel();
      });
    }
  }

  private updateModel() {
    const value = this.tagFormGroup.value;
    this.modelValue = {
      tag: value.tag,
      stateRenderFunction: value.stateRenderFunction
    };
    if (value.clickAction) {
      this.modelValue.actions = {
        click: {
          actionFunction: value.clickAction
        }
      };
    } else {
      this.modelValue.actions = null;
    }
    this.propagateChange(this.modelValue);
  }
}
