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
  Attribute, ChangeDetectionStrategy,
  Component,
  ElementRef,
  forwardRef,
  Input, OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormControl, Validator, NG_VALIDATORS } from '@angular/forms';
import * as ace from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { guid } from '@core/utils';

@Component({
  selector: 'tb-json-object-edit',
  templateUrl: './json-object-edit.component.html',
  styleUrls: ['./json-object-edit.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonObjectEditComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsonObjectEditComponent),
      multi: true,
    }
  ]
})
export class JsonObjectEditComponent implements OnInit, ControlValueAccessor, Validator, OnDestroy {

  @ViewChild('jsonEditor', {static: true})
  jsonEditorElmRef: ElementRef;

  private jsonEditor: ace.Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResizeListener: any;

  toastTargetId = `jsonObjectEditor-${guid()}`;

  @Input() label: string;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  modelValue: any;

  contentValue: string;

  objectValid: boolean;

  validationError: string;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService) {
  }

  ngOnInit(): void {
    const editorElement = this.jsonEditorElmRef.nativeElement;
    let editorOptions: Partial<ace.Ace.EditorOptions> = {
      mode: 'ace/mode/json',
      showGutter: true,
      showPrintMargin: false,
      readOnly: this.disabled || this.readonly
    };

    const advancedOptions = {
      enableSnippets: true,
      enableBasicAutocompletion: true,
      enableLiveAutocompletion: true
    };

    editorOptions = {...editorOptions, ...advancedOptions};
    this.jsonEditor = ace.edit(editorElement, editorOptions);
    this.jsonEditor.session.setUseWrapMode(false);
    this.jsonEditor.setValue(this.contentValue ? this.contentValue : '', -1);
    this.jsonEditor.on('change', () => {
      this.cleanupJsonErrors();
      this.updateView();
    });
    this.editorResizeListener = this.onAceEditorResize.bind(this);
    // @ts-ignore
    addResizeListener(editorElement, this.editorResizeListener);
  }

  ngOnDestroy(): void {
    if (this.editorResizeListener) {
      const editorElement = this.jsonEditorElmRef.nativeElement;
      // @ts-ignore
      removeResizeListener(editorElement, this.editorResizeListener);
    }
  }

  private onAceEditorResize() {
    if (this.editorsResizeCaf) {
      this.editorsResizeCaf();
      this.editorsResizeCaf = null;
    }
    this.editorsResizeCaf = this.raf.raf(() => {
      this.jsonEditor.resize();
      this.jsonEditor.renderer.updateFull();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.jsonEditor) {
      this.jsonEditor.setReadOnly(this.disabled || this.readonly);
    }
  }

  public validate(c: FormControl) {
    return (this.objectValid) ? null : {
      jsonParseError: {
        valid: false,
      },
    };
  }

  validateOnSubmit(): void {
    if (!this.disabled && !this.readonly) {
      this.cleanupJsonErrors();
      if (!this.objectValid) {
        this.store.dispatch(new ActionNotificationShow(
          {
            message: this.validationError,
            type: 'error',
            target: this.toastTargetId,
            verticalPosition: 'bottom',
            horizontalPosition: 'left'
          }));
        this.errorShowed = true;
      }
    }
  }

  cleanupJsonErrors(): void {
    if (this.errorShowed) {
      this.store.dispatch(new ActionNotificationHide(
        {
          target: this.toastTargetId
        }));
      this.errorShowed = false;
    }
  }

  beautifyJSON() {
    const res = JSON.stringify(this.modelValue, null, 2);
    if (this.jsonEditor) {
      this.jsonEditor.setValue(res ? res : '', -1);
    }
    this.updateView();
  }

  minifyJSON() {
    const res = JSON.stringify(this.modelValue);
    if (this.jsonEditor) {
      this.jsonEditor.setValue(res ? res : '', -1);
    }
    this.updateView();
  }

  writeValue(value: any): void {
    this.modelValue = value;
    this.contentValue = '';
    this.objectValid = false;
    try {
      if (this.modelValue) {
        this.contentValue = JSON.stringify(this.modelValue, undefined, 2);
        this.objectValid = true;
      } else {
        this.objectValid = !this.required;
        this.validationError = 'Json object is required.';
      }
    } catch (e) {
      //
    }
    if (this.jsonEditor) {
      this.jsonEditor.setValue(this.contentValue ? this.contentValue : '', -1);
    }
  }

  updateView() {
    const editorValue = this.jsonEditor.getValue();
    if (this.contentValue !== editorValue) {
      this.contentValue = editorValue;
      let data = null;
      this.objectValid = false;
      if (this.contentValue && this.contentValue.length > 0) {
        try {
          data = JSON.parse(this.contentValue);
          this.objectValid = true;
          this.validationError = '';
        } catch (ex) {
          let errorInfo = 'Error:';
          if (ex.name) {
            errorInfo += ' ' + ex.name + ':';
          }
          if (ex.message) {
            errorInfo += ' ' + ex.message;
          }
          this.validationError = errorInfo;
        }
      } else {
        this.objectValid = !this.required;
        this.validationError = this.required ? 'Json object is required.' : '';
      }
      this.propagateChange(data);
    }
  }

  onFullscreen() {
    if (this.jsonEditor) {
      setTimeout(() => {
        this.jsonEditor.resize();
      }, 0);
    }
  }

}
