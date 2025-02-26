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

import { Component, ElementRef, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import {
  beforeSaveCustomMenuConfig,
  CustomMenuConfig,
  CustomMenuInfo,
  CustomMenuItem,
  defaultCustomMenuConfig,
  isDefaultMenuConfig,
  MenuItem
} from '@shared/models/custom-menu.models';
import { AbstractControl, FormControl, UntypedFormArray, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { HasDirtyFlag } from '@core/guards/confirm-on-exit.guard';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { deepClone, isDefined } from '@core/utils';
import {
  AddCustomMenuItemDialogComponent,
  AddCustomMenuItemDialogData
} from '@home/pages/custom-menu/add-custom-menu-item.dialog.component';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { Subject, Subscription } from 'rxjs';

@Component({
  selector: 'tb-custom-menu-config',
  templateUrl: './custom-menu-config.component.html',
  styleUrls: ['./custom-menu-config.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CustomMenuConfigComponent extends PageComponent implements OnInit, OnDestroy, HasDirtyFlag {

  @ViewChild('menuItemsContainer')
  menuItemsContainer: ElementRef<HTMLElement>;

  private forcePristine = false;

  private observeBreakpointSubscription: Subscription;

  private hideItemsSubject = new Subject<void>();

  get isDirty(): boolean {
    return this.customMenuFormGroup.dirty && !this.forcePristine;
  }

  set isDirty(value: boolean) {
    this.forcePristine = !value;
  }

  hideItems$ = this.hideItemsSubject.asObservable();

  maxIconNameBlockWidth = 256;

  customMenu: CustomMenuInfo;
  customMenuConfig: CustomMenuConfig;

  readonly: boolean;

  showHiddenItems: FormControl = new FormControl<boolean>(true);

  customMenuFormGroup: UntypedFormGroup;

  get dragEnabled(): boolean {
    return !this.readonly && this.visibleMenuItemsControls().length > 1;
  }

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private router: Router,
              private route: ActivatedRoute,
              private customMenuService: CustomMenuService,
              private userPermissionsService: UserPermissionsService,
              private dialog: MatDialog,
              private breakpointObserver: BreakpointObserver) {
    super(store);
    this.customMenu = this.route.snapshot.data.customMenu;
    this.customMenuConfig = this.route.snapshot.data.customMenuConfig;
  }

  ngOnInit(): void {
    this.customMenuFormGroup = this.fb.group({
      items: this.prepareMenuItemsFormArray(this.customMenuConfig.items)
    });
    this.readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);
    if (this.readonly) {
      this.customMenuFormGroup.disable({emitEvent: false});
    }
    this.observeBreakpointSubscription =
      this.breakpointObserver.observe([MediaBreakpoints.xs, MediaBreakpoints['gt-xs'], MediaBreakpoints['gt-sm']]).subscribe(() => {
         this.computeMaxIconNameBlockWidth();
    });
    this.computeMaxIconNameBlockWidth();
  }

  ngOnDestroy() {
    this.observeBreakpointSubscription.unsubscribe();
    super.ngOnDestroy();
  }

  goBack() {
    this.router.navigate(['..'], { relativeTo: this.route });
  }

  cancel() {
    this.customMenuFormGroup.setControl('items', this.prepareMenuItemsFormArray(this.customMenuConfig.items), {emitEvent: false});
    this.customMenuFormGroup.markAsPristine();
  }

  resetToDefault() {
    if (!isDefaultMenuConfig(this.customMenuFormGroup.value, this.customMenu.scope)) {
      const config = defaultCustomMenuConfig(this.customMenu.scope);
      this.customMenuFormGroup.setControl('items', this.prepareMenuItemsFormArray(config.items), {emitEvent: false});
      this.customMenuFormGroup.markAsDirty();
    }
  }

  hideAll() {
    this.hideItemsSubject.next();
    if (this.showHiddenItems.value) {
      this.showHiddenItems.setValue(false);
    }
  }

  save() {
    const config: CustomMenuConfig = beforeSaveCustomMenuConfig(this.customMenuFormGroup.value, this.customMenu.scope);
    this.customMenuService.updateCustomMenuConfig(this.customMenu.id.id, config).subscribe(
      () => {
        this.customMenuConfig = deepClone(this.customMenuFormGroup.value);
        this.customMenuFormGroup.markAsPristine();
      }
    );
  }

  menuItemDrop(event: CdkDragDrop<string[]>) {
    const menuItemsArray = this.menuItemsFormArray();
    const menuItem = this.visibleMenuItemsControls()[event.previousIndex];
    const previousIndex = this.actualItemIndex(event.previousIndex);
    const currentIndex = this.actualItemIndex(event.currentIndex);
    menuItemsArray.removeAt(previousIndex);
    menuItemsArray.insert(currentIndex, menuItem);
    this.customMenuFormGroup.markAsDirty();
  }

  visibleMenuItemsControls(): Array<AbstractControl> {
    return this.menuItemsFormArray().controls.filter(c => this.showHiddenItems.value || c.value.visible);
  }

  trackByMenuItem(_index: number, menuItemControl: AbstractControl): any {
    return menuItemControl;
  }

  removeCustomMenuItem(index: number) {
    this.menuItemsFormArray().removeAt(this.actualItemIndex(index));
    this.customMenuFormGroup.markAsDirty();
  }

  isCustom(menuItemControl: AbstractControl): boolean {
    return !menuItemControl.value.id;
  }

  addCustomMenuItem(index?: number) {
    this.dialog.open<AddCustomMenuItemDialogComponent, AddCustomMenuItemDialogData,
      CustomMenuItem>(AddCustomMenuItemDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        subItem: false,
        scope: this.customMenu.scope
      }
    }).afterClosed().subscribe((menuItem) => {
      if (menuItem) {
        const menuItemsArray = this.menuItemsFormArray();
        const menuItemControl = this.fb.control(menuItem, []);
        if (isDefined(index)) {
          const insertIndex = this.actualItemIndex(index) + 1;
          menuItemsArray.insert(insertIndex, menuItemControl);
        } else {
          menuItemsArray.push(menuItemControl);
          setTimeout(() => {
            this.menuItemsContainer.nativeElement.scrollTop = this.menuItemsContainer.nativeElement.scrollHeight;
          }, 0);
        }
        this.customMenuFormGroup.markAsDirty();
      }
    });
  }

  private menuItemsFormArray(): UntypedFormArray {
    return this.customMenuFormGroup.get('items') as UntypedFormArray;
  }

  private actualItemIndex(index: number): number {
    const menuItem = this.visibleMenuItemsControls()[index];
    return this.menuItemsFormArray().controls.indexOf(menuItem);
  }

  private prepareMenuItemsFormArray(items: MenuItem[]): UntypedFormArray {
    const menuItemsControls: Array<AbstractControl> = [];
    items.forEach((item) => {
      menuItemsControls.push(this.fb.control(deepClone(item), []));
    });
    return this.fb.array(menuItemsControls);
  }

  private computeMaxIconNameBlockWidth() {
    if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-sm'])) {
      this.maxIconNameBlockWidth = 256;
    } else if (this.breakpointObserver.isMatched(MediaBreakpoints['gt-xs'])) {
      this.maxIconNameBlockWidth = 200;
    } else {
      this.maxIconNameBlockWidth = 0;
    }
  }

}
