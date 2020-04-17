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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DetailsPanelComponent } from '@home/components/details-panel.component';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { ContactComponent } from '@home/components/contact.component';
import { AuditLogDetailsDialogComponent } from '@home/components/audit-log/audit-log-details-dialog.component';
import { AuditLogTableComponent } from '@home/components/audit-log/audit-log-table.component';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { RelationTableComponent } from '@home/components/relation/relation-table.component';
import { RelationDialogComponent } from '@home/components/relation/relation-dialog.component';
import { AlarmTableHeaderComponent } from '@home/components/alarm/alarm-table-header.component';
import { AlarmTableComponent } from '@home/components/alarm/alarm-table.component';
import { AttributeTableComponent } from '@home/components/attribute/attribute-table.component';
import { AddAttributeDialogComponent } from '@home/components/attribute/add-attribute-dialog.component';
import { EditAttributeValuePanelComponent } from '@home/components/attribute/edit-attribute-value-panel.component';
import { DashboardComponent } from '@home/components/dashboard/dashboard.component';
import { WidgetComponent } from '@home/components/widget/widget.component';
import { WidgetComponentService } from '@home/components/widget/widget-component.service';
import { LegendComponent } from '@home/components/widget/legend.component';
import { AliasesEntitySelectPanelComponent } from '@home/components/alias/aliases-entity-select-panel.component';
import { AliasesEntitySelectComponent } from '@home/components/alias/aliases-entity-select.component';
import { WidgetConfigComponent } from '@home/components/widget/widget-config.component';
import { EntityAliasesDialogComponent } from '@home/components/alias/entity-aliases-dialog.component';
import { EntityFilterViewComponent } from '@home/components/entity/entity-filter-view.component';
import { EntityAliasDialogComponent } from '@home/components/alias/entity-alias-dialog.component';
import { EntityFilterComponent } from '@home/components/entity/entity-filter.component';
import { RelationFiltersComponent } from '@home/components/relation/relation-filters.component';
import { EntityAliasSelectComponent } from '@home/components/alias/entity-alias-select.component';
import { DataKeysComponent } from '@home/components/widget/data-keys.component';
import { DataKeyConfigDialogComponent } from '@home/components/widget/data-key-config-dialog.component';
import { DataKeyConfigComponent } from '@home/components/widget/data-key-config.component';
import { LegendConfigPanelComponent } from '@home/components/widget/legend-config-panel.component';
import { LegendConfigComponent } from '@home/components/widget/legend-config.component';
import { ManageWidgetActionsComponent } from '@home/components/widget/action/manage-widget-actions.component';
import { WidgetActionDialogComponent } from '@home/components/widget/action/widget-action-dialog.component';
import { CustomActionPrettyResourcesTabsComponent } from '@home/components/widget/action/custom-action-pretty-resources-tabs.component';
import { CustomActionPrettyEditorComponent } from '@home/components/widget/action/custom-action-pretty-editor.component';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { CustomDialogContainerComponent } from '@home/components/widget/dialog/custom-dialog-container.component';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { ImportDialogComponent } from '@home/components/import-export/import-dialog.component';
import { AddWidgetToDashboardDialogComponent } from '@home/components/attribute/add-widget-to-dashboard-dialog.component';
import { ImportDialogCsvComponent } from '@home/components/import-export/import-dialog-csv.component';
import { TableColumnsAssignmentComponent } from '@home/components/import-export/table-columns-assignment.component';
import { EventContentDialogComponent } from '@home/components/event/event-content-dialog.component';
import { SharedHomeComponentsModule } from '@home/components/shared-home-components.module';
import { SelectTargetLayoutDialogComponent } from '@home/components/dashboard/select-target-layout-dialog.component';
import { SelectTargetStateDialogComponent } from '@home/components/dashboard/select-target-state-dialog.component';
import { ConverterAutocompleteComponent } from '@home/components/converter/converter-autocomplete.component';
import { OperationTypeListComponent } from '@home/components/role/operation-type-list.component';
import { ResourceTypeAutocompleteComponent } from '@home/components/role/resource-type-autocomplete.component';
import { PermissionListComponent } from '@home/components/role/permission-list.component';
import { GroupPermissionsComponent } from '@home/components/role/group-permissions.component';
import { OwnerAutocompleteComponent } from '@home/components/role/owner-autocomplete.component';
import { GroupPermissionDialogComponent } from '@home/components/role/group-permission-dialog.component';
import { ViewRoleDialogComponent } from '@home/components/role/view-role-dialog.component';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { GroupEntityTableHeaderComponent } from '@home/components/group/group-entity-table-header.component';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { RegistrationPermissionsComponent } from './role/registration-permissions.component';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';
import { EntityGroupSettingsComponent } from '@home/components/group/entity-group-settings.component';
import { EntityGroupColumnsComponent } from '@home/components/group/entity-group-columns.component';
import { EntityGroupColumnDialogComponent } from '@home/components/group/entity-group-column-dialog.component';
import { EntityGroupColumnComponent } from '@home/components/group/entity-group-column.component';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';

@NgModule({
  declarations:
    [
      EntitiesTableComponent,
      AddEntityDialogComponent,
      DetailsPanelComponent,
      EntityDetailsPanelComponent,
      ContactComponent,
      AuditLogTableComponent,
      AuditLogDetailsDialogComponent,
      EventContentDialogComponent,
      EventTableHeaderComponent,
      EventTableComponent,
      RelationTableComponent,
      RelationDialogComponent,
      RelationFiltersComponent,
      AlarmTableHeaderComponent,
      AlarmTableComponent,
      AttributeTableComponent,
      AddAttributeDialogComponent,
      EditAttributeValuePanelComponent,
      AliasesEntitySelectPanelComponent,
      AliasesEntitySelectComponent,
      EntityAliasesDialogComponent,
      EntityAliasDialogComponent,
      DashboardComponent,
      WidgetComponent,
      LegendComponent,
      WidgetConfigComponent,
      EntityFilterViewComponent,
      EntityFilterComponent,
      EntityAliasSelectComponent,
      DataKeysComponent,
      DataKeyConfigComponent,
      DataKeyConfigDialogComponent,
      LegendConfigPanelComponent,
      LegendConfigComponent,
      ManageWidgetActionsComponent,
      WidgetActionDialogComponent,
      CustomActionPrettyResourcesTabsComponent,
      CustomActionPrettyEditorComponent,
      CustomDialogContainerComponent,
      ImportDialogComponent,
      ImportDialogCsvComponent,
      SelectTargetLayoutDialogComponent,
      SelectTargetStateDialogComponent,
      AddWidgetToDashboardDialogComponent,
      TableColumnsAssignmentComponent,
      ConverterAutocompleteComponent,
      OperationTypeListComponent,
      ResourceTypeAutocompleteComponent,
      PermissionListComponent,
      GroupPermissionsComponent,
      GroupPermissionDialogComponent,
      ViewRoleDialogComponent,
      OwnerAutocompleteComponent,
      GroupEntitiesTableComponent,
      GroupEntityTabsComponent,
      GroupEntityTableHeaderComponent,
      EntityGroupComponent,
      EntityGroupTabsComponent,
      EntityGroupSettingsComponent,
      EntityGroupColumnComponent,
      EntityGroupColumnsComponent,
      EntityGroupColumnDialogComponent,
      AddGroupEntityDialogComponent,
      RegistrationPermissionsComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    HomeDialogsModule
  ],
  exports: [
    EntitiesTableComponent,
    AddEntityDialogComponent,
    DetailsPanelComponent,
    EntityDetailsPanelComponent,
    ContactComponent,
    AuditLogTableComponent,
    EventTableComponent,
    RelationTableComponent,
    RelationFiltersComponent,
    AlarmTableComponent,
    AttributeTableComponent,
    AliasesEntitySelectComponent,
    EntityAliasesDialogComponent,
    EntityAliasDialogComponent,
    DashboardComponent,
    WidgetComponent,
    LegendComponent,
    WidgetConfigComponent,
    EntityFilterViewComponent,
    EntityFilterComponent,
    EntityAliasSelectComponent,
    DataKeysComponent,
    DataKeyConfigComponent,
    DataKeyConfigDialogComponent,
    LegendConfigComponent,
    ManageWidgetActionsComponent,
    WidgetActionDialogComponent,
    CustomActionPrettyResourcesTabsComponent,
    CustomActionPrettyEditorComponent,
    CustomDialogContainerComponent,
    ImportDialogComponent,
    ImportDialogCsvComponent,
    TableColumnsAssignmentComponent,
    SelectTargetLayoutDialogComponent,
    SelectTargetStateDialogComponent,
    ConverterAutocompleteComponent,
    OperationTypeListComponent,
    ResourceTypeAutocompleteComponent,
    PermissionListComponent,
    GroupPermissionsComponent,
    GroupPermissionDialogComponent,
    ViewRoleDialogComponent,
    OwnerAutocompleteComponent,
    GroupEntitiesTableComponent,
    GroupEntityTabsComponent,
    GroupEntityTableHeaderComponent,
    EntityGroupComponent,
    EntityGroupTabsComponent,
    EntityGroupSettingsComponent,
    EntityGroupColumnComponent,
    EntityGroupColumnsComponent,
    EntityGroupColumnDialogComponent,
    AddGroupEntityDialogComponent,
    RegistrationPermissionsComponent
  ],
  providers: [
    WidgetComponentService,
    CustomDialogService,
    ImportExportService,
    GroupConfigTableConfigService,
    EntityGroupsTableConfigResolver,
    EntityGroupConfigResolver
  ]
})
export class HomeComponentsModule { }
