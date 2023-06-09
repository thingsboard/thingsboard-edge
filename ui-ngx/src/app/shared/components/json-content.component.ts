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
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  ViewChild, ViewEncapsulation
} from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { Ace } from 'ace-builds';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ContentType, contentTypesMap } from '@shared/models/constants';
import { CancelAnimationFrame, RafService } from '@core/services/raf.service';
import { guid } from '@core/utils';
import { ResizeObserver } from '@juggle/resize-observer';
import { getAce } from '@shared/models/ace/ace.models';
import { beautifyJs } from '@shared/models/beautify.models';

@Component({
  selector: 'tb-json-content',
  templateUrl: './json-content.component.html',
  styleUrls: ['./json-content.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => JsonContentComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => JsonContentComponent),
      multi: true,
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class JsonContentComponent implements OnInit, ControlValueAccessor, Validator, OnChanges, OnDestroy {

  @ViewChild('jsonEditor', {static: true})
  jsonEditorElmRef: ElementRef;

  private jsonEditor: Ace.Editor;
  private editorsResizeCaf: CancelAnimationFrame;
  private editorResize$: ResizeObserver;
  private ignoreChange = false;

  toastTargetId = `jsonContentEditor-${guid()}`;

  @Input() label: string;

  @Input() contentType: ContentType;

  @Input() disabled: boolean;

  @Input() fillHeight: boolean;

  @Input() editorStyle: {[klass: string]: any};

  @Input() tbPlaceholder: string;

  private readonlyValue: boolean;
  get readonly(): boolean {
    return this.readonlyValue;
  }
  @Input()
  set readonly(value: boolean) {
    this.readonlyValue = coerceBooleanProperty(value);
  }

  private validateContentValue: boolean;
  get validateContent(): boolean {
    return this.validateContentValue;
  }
  @Input()
  set validateContent(value: boolean) {
    this.validateContentValue = coerceBooleanProperty(value);
  }

  private validateOnChangeValue: boolean;
  get validateOnChange(): boolean {
    return this.validateOnChangeValue;
  }
  @Input()
  set validateOnChange(value: boolean) {
    this.validateOnChangeValue = coerceBooleanProperty(value);
  }

  private requiredValue: boolean;
  get required(): boolean {
    return this.requiredValue;
  }
  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  fullscreen = false;

  contentBody: string;

  contentValid: boolean;

  errorShowed = false;

  private propagateChange = null;

  constructor(public elementRef: ElementRef,
              protected store: Store<AppState>,
              private raf: RafService,
              private cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    const editorElement = this.jsonEditorElmRef.nativeElement;
    let mode = 'text';
    if (this.contentType) {
      mode = contentTypesMap.get(this.contentType).code;
    }
    let editorOptions: Partial<Ace.EditorOptions> = {
      mode: `ace/mode/${mode}`,
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
    getAce().subscribe(
      (ace) => {
        this.jsonEditor = ace.edit(editorElement, editorOptions);
        this.jsonEditor.session.setUseWrapMode(true);
        this.jsonEditor.setValue(this.contentBody ? this.contentBody : '', -1);
        this.jsonEditor.setReadOnly(this.disabled || this.readonly);
        this.jsonEditor.on('change', () => {
          if (!this.ignoreChange) {
            this.cleanupJsonErrors();
            this.updateView();
          }
        });
        if (this.validateContent) {
          this.jsonEditor.on('blur', () => {
            this.contentValid = this.doValidate(true);
            this.cd.markForCheck();
          });
        }
        if (this.tbPlaceholder && this.tbPlaceholder.length) {
            this.createPlaceholder();
        }
        this.editorResize$ = new ResizeObserver(() => {
          this.onAceEditorResize();
        });
        this.editorResize$.observe(editorElement);
      }
    );
  }

  private createPlaceholder() {
    this.jsonEditor.on('input', this.updateEditorPlaceholder.bind(this));
    setTimeout(this.updateEditorPlaceholder.bind(this), 100);
  }

  private updateEditorPlaceholder() {
    const shouldShow = !this.jsonEditor.session.getValue().length;
    let node: HTMLElement = (this.jsonEditor.renderer as any).emptyMessageNode;
    if (!shouldShow && node) {
      this.jsonEditor.renderer.getMouseEventTarget().removeChild(node);
      (this.jsonEditor.renderer as any).emptyMessageNode = null;
    } else if (shouldShow && !node) {
      const placeholderElement = $('<textarea></textarea>');
      placeholderElement.text(this.tbPlaceholder);
      placeholderElement.addClass('ace_invisible ace_emptyMessage');
      placeholderElement.css({
        padding: '0 9px',
        width: '100%',
        border: 'none',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        resize: 'none',
        fontSize: '15px'
      });
      const rows = this.tbPlaceholder.split('\n').length;
      placeholderElement.attr('rows', rows);
      node = placeholderElement[0];
      (this.jsonEditor.renderer as any).emptyMessageNode = node;
      this.jsonEditor.renderer.getMouseEventTarget().appendChild(node);
    }
  }

  ngOnDestroy(): void {
    if (this.editorResize$) {
      this.editorResize$.disconnect();
    }
    if (this.jsonEditor) {
      this.jsonEditor.destroy();
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

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'contentType') {
          if (this.jsonEditor) {
            let mode = 'text';
            if (this.contentType) {
              mode = contentTypesMap.get(this.contentType).code;
            }
            this.jsonEditor.session.setMode(`ace/mode/${mode}`);
          }
        }
      }
    }
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

  public validate(c: UntypedFormControl) {
    return (this.contentValid) ? null : {
      contentBody: {
        valid: false,
      },
    };
  }

  validateOnSubmit(): void {
    if (!this.disabled && !this.readonly) {
      this.cleanupJsonErrors();
      this.contentValid = true;
      this.propagateChange(this.contentBody);
      this.contentValid = this.doValidate(true);
      this.propagateChange(this.contentBody);
      this.cd.markForCheck();
    }
  }

  private doValidate(showErrorToast = false): boolean {
    try {
      if (this.contentType === ContentType.JSON) {
        JSON.parse(this.contentBody);
      }
      return true;
    } catch (ex) {
      if (showErrorToast) {
        let errorInfo = 'Error:';
        if (ex.name) {
          errorInfo += ' ' + ex.name + ':';
        }
        if (ex.message) {
          errorInfo += ' ' + ex.message;
        }
        this.store.dispatch(new ActionNotificationShow(
          {
            message: errorInfo,
            type: 'error',
            target: this.toastTargetId,
            verticalPosition: 'bottom',
            horizontalPosition: 'left'
          }));
        this.errorShowed = true;
      }
      return false;
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

  writeValue(value: string): void {
    this.contentBody = value;
    this.contentValid = true;
    if (this.jsonEditor) {
      this.ignoreChange = true;
      this.jsonEditor.setValue(this.contentBody ? this.contentBody : '', -1);
      this.ignoreChange = false;
    }
  }

  updateView() {
    const editorValue = this.jsonEditor.getValue();
    if (this.contentBody !== editorValue) {
      this.contentBody = editorValue;
      this.contentValid = !this.validateOnChange || this.doValidate();
      this.propagateChange(this.contentBody);
      this.cd.markForCheck();
    }
  }

  beautifyJSON() {
    beautifyJs(this.contentBody, {indent_size: 4, wrap_line_length: 60}).subscribe(
      (res) => {
        this.jsonEditor.setValue(res ? res : '', -1);
        this.updateView();
      }
    );
  }

  minifyJSON() {
    let res = null;
    try {
      res = JSON.stringify(JSON.parse(this.contentBody));
    } catch (e) {}
    if (res) {
      this.jsonEditor.setValue(res, -1);
      this.updateView();
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
