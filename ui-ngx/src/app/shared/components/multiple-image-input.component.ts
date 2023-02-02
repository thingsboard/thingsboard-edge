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

import { AfterViewInit, ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, FormArray, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { Subscription } from 'rxjs';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import { DropDirective, FlowDirective } from '@flowjs/ngx-flow';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { UtilsService } from '@core/services/utils.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { DndDropEvent } from 'ngx-drag-drop';
import { isUndefined } from '@core/utils';

@Component({
  selector: 'tb-multiple-image-input',
  templateUrl: './multiple-image-input.component.html',
  styleUrls: ['./multiple-image-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MultipleImageInputComponent),
      multi: true
    }
  ]
})
export class MultipleImageInputComponent extends PageComponent implements AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  maxSizeByte: number;

  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    const newVal = coerceBooleanProperty(value);
    if (this.requiredValue !== newVal) {
      this.requiredValue = newVal;
    }
  }

  @Input()
  disabled: boolean;

  @Input()
  inputId = this.utils.guid();

  imageUrls: string[];
  safeImageUrls: SafeUrl[];

  dragIndex: number;

  @ViewChild('flow', {static: true})
  flow: FlowDirective;

  @ViewChild('flowDrop', {static: true})
  flowDrop: DropDirective;

  autoUploadSubscription: Subscription;

  private propagateChange = null;

  private viewInited = false;

  constructor(protected store: Store<AppState>,
              private utils: UtilsService,
              private sanitizer: DomSanitizer,
              private dialog: DialogService,
              private translate: TranslateService,
              private fileSize: FileSizePipe,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngAfterViewInit() {
    this.autoUploadSubscription = this.flow.events$.subscribe(event => {
      if (event.type === 'filesAdded') {
        const readers = [];
        (event.event[0] as flowjs.FlowFile[]).forEach(file => {
          readers.push(this.readImageUrl(file));
        });
        if (readers.length) {
          Promise.all(readers).then((files) => {
            files = files.filter(file => file.imageUrl != null || file.safeImageUrl != null);
            this.imageUrls = this.imageUrls.concat(files.map(content => content.imageUrl));
            this.safeImageUrls = this.safeImageUrls.concat(files.map(content => content.safeImageUrl));
            this.updateModel();
          });
        }
      }
    });
    if (this.disabled) {
      this.flowDrop.disable();
    } else {
      this.flowDrop.enable();
    }
    this.viewInited = true;
  }

  private readImageUrl(file: flowjs.FlowFile): Promise<any> {
    return new Promise((resolve) => {
      if (this.maxSizeByte && this.maxSizeByte < file.size) {
        resolve({imageUrl: null, safeImageUrl: null});
      }
      const reader = new FileReader();
      reader.onload = () => {
        let imageUrl = null;
        let safeImageUrl = null;
        if (typeof reader.result === 'string' && reader.result.startsWith('data:image/')) {
          imageUrl = reader.result;
          if (imageUrl && imageUrl.length > 0) {
            safeImageUrl = this.sanitizer.bypassSecurityTrustUrl(imageUrl);
          }
        }
        resolve({imageUrl, safeImageUrl});
      };
      reader.onerror = () => {
        resolve({imageUrl: null, safeImageUrl: null});
      };
      reader.readAsDataURL(file.file);
    });
  }

  ngOnDestroy() {
    this.autoUploadSubscription.unsubscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.viewInited) {
      if (this.disabled) {
        this.flowDrop.disable();
      } else {
        this.flowDrop.enable();
      }
    }
  }

  writeValue(value: string[]): void {
    this.imageUrls = value || [];
    this.safeImageUrls = this.imageUrls.map(imageUrl => this.sanitizer.bypassSecurityTrustUrl(imageUrl));
  }

  private updateModel() {
    this.cd.markForCheck();
    this.propagateChange(this.imageUrls);
  }

  clearImage(index: number) {
    this.imageUrls.splice(index, 1);
    this.safeImageUrls.splice(index, 1);
    this.updateModel();
  }

  imageDragStart(index: number) {
    setTimeout(() => {
      this.dragIndex = index;
      this.cd.markForCheck();
    });
  }

  imageDragEnd() {
    this.dragIndex = -1;
    this.cd.markForCheck();
  }

  imageDrop(event: DndDropEvent) {
    let index = event.index;
    if (isUndefined(index)) {
      index = this.safeImageUrls.length;
    }
    moveItemInArray(this.imageUrls, this.dragIndex, index);
    moveItemInArray(this.safeImageUrls, this.dragIndex, index);
    this.dragIndex = -1;
    this.updateModel();
  }
}
