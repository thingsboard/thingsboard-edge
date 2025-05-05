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

import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, } from '@angular/forms';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  extractParamsFromImageResourceUrl,
  IMAGE_BASE64_URL_PREFIX,
  ImageResourceInfo,
  prependTbImagePrefix,
  removeTbImagePrefix,
  ResourceSubType
} from '@shared/models/resource.models';
import { ImageService } from '@core/http/image.service';
import { MatDialog } from '@angular/material/dialog';
import {
  ImageGalleryDialogComponent,
  ImageGalleryDialogData
} from '@shared/components/image/image-gallery-dialog.component';
import { ScadaSymbolMetadata } from '@home/components/widget/lib/scada/scada-symbol.models';
import { stringToBase64 } from '@core/utils';

export enum ScadaSymbolLinkType {
  none = 'none',
  content = 'content',
  resource = 'resource'
}

@Component({
  selector: 'tb-scada-symbol-input',
  templateUrl: './scada-symbol-input.component.html',
  styleUrls: ['./scada-symbol-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ScadaSymbolInputComponent),
      multi: true
    }
  ]
})
export class ScadaSymbolInputComponent extends PageComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input()
  label: string;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  @Input()
  scadaSymbolContent: string;

  @Input()
  scadaSymbolMetadata: ScadaSymbolMetadata;

  scadaSymbolUrl: string;

  imageResource: ImageResourceInfo;

  loadingImageResource = false;

  ScadaSymbolLinkType = ScadaSymbolLinkType;

  linkType: ScadaSymbolLinkType = ScadaSymbolLinkType.none;

  private propagateChange = null;

  constructor(protected store: Store<AppState>,
              private imageService: ImageService,
              private dialog: MatDialog,
              private cd: ChangeDetectorRef) {
    super(store);
  }

  ngOnInit() {
    if (this.scadaSymbolContent && this.scadaSymbolMetadata) {
      this.scadaSymbolUrl = IMAGE_BASE64_URL_PREFIX + 'svg+xml;base64,' + stringToBase64(this.scadaSymbolContent);
      this.linkType = ScadaSymbolLinkType.content;
    }
  }

  ngOnDestroy() {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.detectLinkType();
    }
  }

  writeValue(value: string): void {
    value = removeTbImagePrefix(value);
    if (this.scadaSymbolUrl !== value) {
      this.reset();
      this.scadaSymbolUrl = value;
      this.detectLinkType();
      if (this.linkType === ScadaSymbolLinkType.resource) {
        const params = extractParamsFromImageResourceUrl(this.scadaSymbolUrl);
        if (params) {
          this.loadingImageResource = true;
          this.imageService.getImageInfo(params.type, params.key, {ignoreLoading: true, ignoreErrors: true}).subscribe(
            {
              next: (res) => {
                this.imageResource = res;
                this.loadingImageResource = false;
                this.cd.markForCheck();
              },
              error: () => {
                this.reset();
                this.loadingImageResource = false;
                this.cd.markForCheck();
              }
            }
          );
        } else {
          this.reset();
          this.cd.markForCheck();
        }
      }
    }
  }

  private detectLinkType() {
    if (this.scadaSymbolUrl) {
      this.linkType = ScadaSymbolLinkType.resource;
    } else {
      this.linkType = ScadaSymbolLinkType.none;
    }
  }

  private updateModel(value: string) {
    this.cd.markForCheck();
    if (this.scadaSymbolUrl !== value) {
      this.scadaSymbolUrl = value;
      this.propagateChange(prependTbImagePrefix(this.scadaSymbolUrl));
    }
  }

  private reset() {
    this.linkType = ScadaSymbolLinkType.none;
    this.imageResource = null;
  }

  clearSymbol() {
    this.reset();
    this.updateModel(null);
  }

  openGallery($event: Event): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<ImageGalleryDialogComponent, ImageGalleryDialogData,
      ImageResourceInfo>(ImageGalleryDialogComponent, {
      autoFocus: false,
      disableClose: false,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        imageSubType: ResourceSubType.SCADA_SYMBOL
      }
    }).afterClosed().subscribe((image) => {
      if (image) {
        this.linkType = ScadaSymbolLinkType.resource;
        this.imageResource = image;
        this.updateModel(image.link);
      }
    });
  }

}
