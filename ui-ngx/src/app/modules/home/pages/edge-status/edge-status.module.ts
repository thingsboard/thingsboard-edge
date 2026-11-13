// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { EdgeStatusComponent } from './edge-status.component';

@NgModule({
  declarations: [ EdgeStatusComponent ],
  imports: [
    CommonModule,
    SharedModule
  ]
})
export class EdgeStatusModule { }
