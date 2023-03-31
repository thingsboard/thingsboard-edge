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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AddEntityDialogComponent } from '@home/components/entity/add-entity-dialog.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { DetailsPanelComponent } from '@home/components/details-panel.component';
import { EntityDetailsPanelComponent } from '@home/components/entity/entity-details-panel.component';
import { AuditLogDetailsDialogComponent } from '@home/components/audit-log/audit-log-details-dialog.component';
import { AuditLogTableComponent } from '@home/components/audit-log/audit-log-table.component';
import { EventTableHeaderComponent } from '@home/components/event/event-table-header.component';
import { EventTableComponent } from '@home/components/event/event-table.component';
import { EventFilterPanelComponent } from '@home/components/event/event-filter-panel.component';
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
import { LegendConfigComponent } from '@home/components/widget/legend-config.component';
import { ManageWidgetActionsComponent } from '@home/components/widget/action/manage-widget-actions.component';
import { WidgetActionDialogComponent } from '@home/components/widget/action/widget-action-dialog.component';
import { CustomActionPrettyResourcesTabsComponent } from '@home/components/widget/action/custom-action-pretty-resources-tabs.component';
import { CustomActionPrettyEditorComponent } from '@home/components/widget/action/custom-action-pretty-editor.component';
import { MobileActionEditorComponent } from '@home/components/widget/action/mobile-action-editor.component';
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
import { AliasesEntityAutocompleteComponent } from '@home/components/alias/aliases-entity-autocomplete.component';
import { BooleanFilterPredicateComponent } from '@home/components/filter/boolean-filter-predicate.component';
import { StringFilterPredicateComponent } from '@home/components/filter/string-filter-predicate.component';
import { NumericFilterPredicateComponent } from '@home/components/filter/numeric-filter-predicate.component';
import { ComplexFilterPredicateComponent } from '@home/components/filter/complex-filter-predicate.component';
import { FilterPredicateComponent } from '@home/components/filter/filter-predicate.component';
import { FilterPredicateListComponent } from '@home/components/filter/filter-predicate-list.component';
import { KeyFilterListComponent } from '@home/components/filter/key-filter-list.component';
import { ComplexFilterPredicateDialogComponent } from '@home/components/filter/complex-filter-predicate-dialog.component';
import { KeyFilterDialogComponent } from '@home/components/filter/key-filter-dialog.component';
import { FiltersDialogComponent } from '@home/components/filter/filters-dialog.component';
import { FilterDialogComponent } from '@home/components/filter/filter-dialog.component';
import { FilterSelectComponent } from '@home/components/filter/filter-select.component';
import { FiltersEditComponent } from '@home/components/filter/filters-edit.component';
import { FiltersEditPanelComponent } from '@home/components/filter/filters-edit-panel.component';
import { UserFilterDialogComponent } from '@home/components/filter/user-filter-dialog.component';
import { FilterUserInfoComponent } from '@home/components/filter/filter-user-info.component';
import { FilterUserInfoDialogComponent } from '@home/components/filter/filter-user-info-dialog.component';
import { FilterPredicateValueComponent } from '@home/components/filter/filter-predicate-value.component';
import { TenantProfileAutocompleteComponent } from '@home/components/profile/tenant-profile-autocomplete.component';
import { TenantProfileComponent } from '@home/components/profile/tenant-profile.component';
import { TenantProfileDialogComponent } from '@home/components/profile/tenant-profile-dialog.component';
import { TenantProfileDataComponent } from '@home/components/profile/tenant-profile-data.component';
import { DefaultDeviceProfileConfigurationComponent } from '@home/components/profile/device/default-device-profile-configuration.component';
import { DeviceProfileConfigurationComponent } from '@home/components/profile/device/device-profile-configuration.component';
import { DeviceProfileComponent } from '@home/components/profile/device-profile.component';
import { DefaultDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/default-device-profile-transport-configuration.component';
import { DeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/device-profile-transport-configuration.component';
import { DeviceProfileDialogComponent } from '@home/components/profile/device-profile-dialog.component';
import { DeviceProfileAutocompleteComponent } from '@home/components/profile/device-profile-autocomplete.component';
import { MqttDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/mqtt-device-profile-transport-configuration.component';
import { CoapDeviceProfileTransportConfigurationComponent } from '@home/components/profile/device/coap-device-profile-transport-configuration.component';
import { DeviceProfileAlarmsComponent } from '@home/components/profile/alarm/device-profile-alarms.component';
import { DeviceProfileAlarmComponent } from '@home/components/profile/alarm/device-profile-alarm.component';
import { CreateAlarmRulesComponent } from '@home/components/profile/alarm/create-alarm-rules.component';
import { AlarmRuleComponent } from '@home/components/profile/alarm/alarm-rule.component';
import { AlarmRuleConditionComponent } from '@home/components/profile/alarm/alarm-rule-condition.component';
import { FilterTextComponent } from '@home/components/filter/filter-text.component';
import { AddDeviceProfileDialogComponent } from '@home/components/profile/add-device-profile-dialog.component';
import { RuleChainAutocompleteComponent } from '@home/components/rule-chain/rule-chain-autocomplete.component';
import { DeviceProfileProvisionConfigurationComponent } from '@home/components/profile/device-profile-provision-configuration.component';
import { AlarmScheduleComponent } from '@home/components/profile/alarm/alarm-schedule.component';
import { DeviceWizardDialogComponent } from '@home/components/wizard/device-wizard-dialog.component';
import { AlarmScheduleInfoComponent } from '@home/components/profile/alarm/alarm-schedule-info.component';
import { AlarmScheduleDialogComponent } from '@home/components/profile/alarm/alarm-schedule-dialog.component';
import { EditAlarmDetailsDialogComponent } from '@home/components/profile/alarm/edit-alarm-details-dialog.component';
import { AlarmRuleConditionDialogComponent } from '@home/components/profile/alarm/alarm-rule-condition-dialog.component';
import { DefaultTenantProfileConfigurationComponent } from '@home/components/profile/tenant/default-tenant-profile-configuration.component';
import { TenantProfileConfigurationComponent } from '@home/components/profile/tenant/tenant-profile-configuration.component';
import { SmsProviderConfigurationComponent } from '@home/components/sms/sms-provider-configuration.component';
import { AwsSnsProviderConfigurationComponent } from '@home/components/sms/aws-sns-provider-configuration.component';
import { SmppSmsProviderConfigurationComponent } from '@home/components/sms/smpp-sms-provider-configuration.component';
import { TwilioSmsProviderConfigurationComponent } from '@home/components/sms/twilio-sms-provider-configuration.component';
import { Lwm2mProfileComponentsModule } from '@home/components/profile/device/lwm2m/lwm2m-profile-components.module';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { DashboardToolbarComponent } from '@home/components/dashboard-page/dashboard-toolbar.component';
import { StatesControllerModule } from '@home/components/dashboard-page/states/states-controller.module';
import { DashboardLayoutComponent } from '@home/components/dashboard-page/layout/dashboard-layout.component';
import { EditWidgetComponent } from '@home/components/dashboard-page/edit-widget.component';
import { DashboardWidgetSelectComponent } from '@home/components/dashboard-page/dashboard-widget-select.component';
import { AddWidgetDialogComponent } from '@home/components/dashboard-page/add-widget-dialog.component';
import { ManageDashboardLayoutsDialogComponent } from '@home/components/dashboard-page/layout/manage-dashboard-layouts-dialog.component';
import { DashboardSettingsDialogComponent } from '@home/components/dashboard-page/dashboard-settings-dialog.component';
import { ManageDashboardStatesDialogComponent } from '@home/components/dashboard-page/states/manage-dashboard-states-dialog.component';
import { DashboardStateDialogComponent } from '@home/components/dashboard-page/states/dashboard-state-dialog.component';
import { EmbedDashboardDialogComponent } from '@home/components/widget/dialog/embed-dashboard-dialog.component';
import { EMBED_DASHBOARD_DIALOG_TOKEN } from '@home/components/widget/dialog/embed-dashboard-dialog-token';
import { EdgeDownlinkTableComponent } from '@home/components/edge/edge-downlink-table.component';
import { EdgeDownlinkTableHeaderComponent } from '@home/components/edge/edge-downlink-table-header.component';
import { EntityGroupWizardDialogComponent } from '@home/components/wizard/entity-group-wizard-dialog.component';
import { GroupConfigTableConfigService } from '@home/components/group/group-config-table-config.service';
import { EntityGroupsTableConfigResolver } from '@home/components/group/entity-groups-table-config.resolver';
import { EntityGroupConfigResolver } from '@home/components/group/entity-group-config.resolver';
import { ConverterAutocompleteComponent } from '@home/components/converter/converter-autocomplete.component';
import { AddConverterDialogComponent } from '@home/components/converter/add-converter-dialog.component';
import { OperationTypeListComponent } from '@home/components/role/operation-type-list.component';
import { ResourceTypeAutocompleteComponent } from '@home/components/role/resource-type-autocomplete.component';
import { PermissionListComponent } from '@home/components/role/permission-list.component';
import { ViewRoleDialogComponent } from '@home/components/role/view-role-dialog.component';
import { GroupEntitiesTableComponent } from '@home/components/group/group-entities-table.component';
import { GroupEntityTabsComponent } from '@home/components/group/group-entity-tabs.component';
import { GroupEntityTableHeaderComponent } from '@home/components/group/group-entity-table-header.component';
import { EntityGroupTabsComponent } from '@home/components/group/entity-group-tabs.component';
import { EntityGroupSettingsComponent } from '@home/components/group/entity-group-settings.component';
import { EntityGroupColumnsComponent } from '@home/components/group/entity-group-columns.component';
import { EntityGroupColumnDialogComponent } from '@home/components/group/entity-group-column-dialog.component';
import { AddGroupEntityDialogComponent } from '@home/components/group/add-group-entity-dialog.component';
import { RegistrationPermissionsComponent } from '@home/components/role/registration-permissions.component';
import { EntityGroupComponent } from '@home/components/group/entity-group.component';
import { HomeDialogsModule } from '@home/dialogs/home-dialogs.module';
import { EntityGroupColumnComponent } from '@home/components/group/entity-group-column.component';
import { DisplayWidgetTypesPanelComponent } from '@home/components/dashboard-page/widget-types-panel.component';
import { AlarmDurationPredicateValueComponent } from '@home/components/profile/alarm/alarm-duration-predicate-value.component';
import { OtaUpdateEventConfigComponent } from '@home/components/scheduler/config/ota-update-event-config.component';
import { TargetSelectComponent } from '@home/components/scheduler/config/target-select.component';
import { DashboardImageDialogComponent } from '@home/components/dashboard-page/dashboard-image-dialog.component';
import { WidgetContainerComponent } from '@home/components/widget/widget-container.component';
import { SnmpDeviceProfileTransportModule } from '@home/components/profile/device/snmp/snmp-device-profile-transport.module';
import { DeviceCredentialsModule } from '@home/components/device/device-credentials.module';
import { DeviceProfileCommonModule } from '@home/components/profile/device/common/device-profile-common.module';
import { SolutionInstallDialogComponent } from '@home/components/solution/solution-install-dialog.component';
import {
  COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN,
  DASHBOARD_PAGE_COMPONENT_TOKEN,
  HOME_COMPONENTS_MODULE_TOKEN
} from '@home/components/tokens';
import { DashboardStateComponent } from '@home/components/dashboard-page/dashboard-state.component';
import { AlarmDynamicValue } from '@home/components/profile/alarm/alarm-dynamic-value.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { TenantProfileQueuesComponent } from '@home/components/profile/queue/tenant-profile-queues.component';
import { QueueFormComponent } from '@home/components/queue/queue-form.component';
import { WidgetSettingsModule } from '@home/components/widget/lib/settings/widget-settings.module';
import { WidgetSettingsComponent } from '@home/components/widget/widget-settings.component';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';
import { VersionControlComponent } from '@home/components/vc/version-control.component';
import { EntityVersionsTableComponent } from '@home/components/vc/entity-versions-table.component';
import { EntityVersionCreateComponent } from '@home/components/vc/entity-version-create.component';
import { EntityVersionRestoreComponent } from '@home/components/vc/entity-version-restore.component';
import { EntityVersionDiffComponent } from '@home/components/vc/entity-version-diff.component';
import { ComplexVersionCreateComponent } from '@home/components/vc/complex-version-create.component';
import { EntityTypesVersionCreateComponent } from '@home/components/vc/entity-types-version-create.component';
import { EntityTypesVersionLoadComponent } from '@home/components/vc/entity-types-version-load.component';
import { ComplexVersionLoadComponent } from '@home/components/vc/complex-version-load.component';
import { RemoveOtherEntitiesConfirmComponent } from '@home/components/vc/remove-other-entities-confirm.component';
import { AutoCommitSettingsComponent } from '@home/components/vc/auto-commit-settings.component';
import { OwnerEntityGroupListComponent } from '@home/components/vc/owner-entity-group-list.component';
import { RateLimitsComponent } from '@home/components/profile/tenant/rate-limits/rate-limits.component';
import { RateLimitsTextComponent } from '@home/components/profile/tenant/rate-limits/rate-limits-text.component';
import { RateLimitsListComponent } from '@home/components/profile/tenant/rate-limits/rate-limits-list.component';
import { RateLimitsDetailsDialogComponent } from '@home/components/profile/tenant/rate-limits/rate-limits-details-dialog.component';
import { AssetProfileComponent } from '@home/components/profile/asset-profile.component';
import { AssetProfileDialogComponent } from '@home/components/profile/asset-profile-dialog.component';
import { AssetProfileAutocompleteComponent } from '@home/components/profile/asset-profile-autocomplete.component';
import { IntegrationWizardDialogComponent } from '@home/components/wizard/integration-wizard-dialog.component';
import { ConverterComponent } from '@home/components/converter/converter.component';
import { ConverterTestDialogComponent } from '@home/components/converter/converter-test-dialog.component';
import { IntegrationComponentModule } from '@home/components/integration/integration-component.module';
import { MODULES_MAP } from '@shared/models/constants';
import { modulesMap } from '@modules/common/modules-map';
import { AlarmAssigneePanelComponent } from '@home/components/alarm/alarm-assignee-panel.component';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { AllEntitiesTableConfigService } from '@home/components/entity/all-entities-table-config.service';
import { SlackConversationAutocompleteComponent } from '@home/components/notification/slack-conversation-autocomplete.component';
import { SendNotificationButtonComponent } from '@home/components/notification/send-notification-button.component';
import { GroupChipsComponent } from '@home/components/group/group-chips.component';
import { GroupEntityInfoComponent } from '@home/components/group/group-entity-info.component';
import { ManageOwnerAndGroupsDialogComponent } from '@home/components/group/manage-owner-and-groups-dialog.component';
import { OwnerAndGroupsComponent } from '@home/components/group/owner-and-groups.component';

@NgModule({
  declarations:
    [
      RouterTabsComponent,
      EntitiesTableComponent,
      AddEntityDialogComponent,
      DetailsPanelComponent,
      EntityDetailsPanelComponent,
      EntityDetailsPageComponent,
      AuditLogTableComponent,
      AuditLogDetailsDialogComponent,
      EventContentDialogComponent,
      EventTableHeaderComponent,
      EventTableComponent,
      EventFilterPanelComponent,
      EdgeDownlinkTableHeaderComponent,
      EdgeDownlinkTableComponent,
      RelationTableComponent,
      RelationDialogComponent,
      RelationFiltersComponent,
      AlarmTableHeaderComponent,
      AlarmTableComponent,
      AlarmAssigneePanelComponent,
      AttributeTableComponent,
      AddAttributeDialogComponent,
      EditAttributeValuePanelComponent,
      AliasesEntitySelectPanelComponent,
      AliasesEntitySelectComponent,
      AliasesEntityAutocompleteComponent,
      EntityAliasesDialogComponent,
      EntityAliasDialogComponent,
      DashboardComponent,
      WidgetContainerComponent,
      WidgetComponent,
      LegendComponent,
      WidgetSettingsComponent,
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
      MobileActionEditorComponent,
      CustomDialogContainerComponent,
      ImportDialogComponent,
      ImportDialogCsvComponent,
      SelectTargetLayoutDialogComponent,
      SelectTargetStateDialogComponent,
      AddWidgetToDashboardDialogComponent,
      TableColumnsAssignmentComponent,
      ConverterAutocompleteComponent,
      AddConverterDialogComponent,
      OperationTypeListComponent,
      ResourceTypeAutocompleteComponent,
      PermissionListComponent,
      ViewRoleDialogComponent,
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
      GroupChipsComponent,
      GroupEntityInfoComponent,
      OwnerAndGroupsComponent,
      ManageOwnerAndGroupsDialogComponent,
      RegistrationPermissionsComponent,
      BooleanFilterPredicateComponent,
      StringFilterPredicateComponent,
      NumericFilterPredicateComponent,
      ComplexFilterPredicateComponent,
      ComplexFilterPredicateDialogComponent,
      FilterPredicateComponent,
      FilterPredicateListComponent,
      KeyFilterListComponent,
      KeyFilterDialogComponent,
      FilterDialogComponent,
      FiltersDialogComponent,
      FilterSelectComponent,
      FilterTextComponent,
      FiltersEditComponent,
      FiltersEditPanelComponent,
      UserFilterDialogComponent,
      FilterUserInfoComponent,
      FilterUserInfoDialogComponent,
      FilterPredicateValueComponent,
      TenantProfileAutocompleteComponent,
      DefaultTenantProfileConfigurationComponent,
      TenantProfileConfigurationComponent,
      TenantProfileDataComponent,
      TenantProfileComponent,
      TenantProfileDialogComponent,
      DeviceProfileAutocompleteComponent,
      DefaultDeviceProfileConfigurationComponent,
      DeviceProfileConfigurationComponent,
      DefaultDeviceProfileTransportConfigurationComponent,
      MqttDeviceProfileTransportConfigurationComponent,
      CoapDeviceProfileTransportConfigurationComponent,
      DeviceProfileTransportConfigurationComponent,
      CreateAlarmRulesComponent,
      AlarmRuleComponent,
      AlarmRuleConditionDialogComponent,
      AlarmRuleConditionComponent,
      DeviceProfileAlarmComponent,
      DeviceProfileAlarmsComponent,
      DeviceProfileComponent,
      DeviceProfileDialogComponent,
      AddDeviceProfileDialogComponent,
      AssetProfileComponent,
      AssetProfileDialogComponent,
      AssetProfileAutocompleteComponent,
      RuleChainAutocompleteComponent,
      AlarmScheduleInfoComponent,
      DeviceProfileProvisionConfigurationComponent,
      AlarmScheduleComponent,
      AlarmDynamicValue,
      AlarmDurationPredicateValueComponent,
      DeviceWizardDialogComponent,
      AlarmScheduleDialogComponent,
      EditAlarmDetailsDialogComponent,
      SmsProviderConfigurationComponent,
      AwsSnsProviderConfigurationComponent,
      SmppSmsProviderConfigurationComponent,
      TwilioSmsProviderConfigurationComponent,
      EntityGroupWizardDialogComponent,
      DashboardToolbarComponent,
      DashboardPageComponent,
      DashboardStateComponent,
      DashboardLayoutComponent,
      EditWidgetComponent,
      DashboardWidgetSelectComponent,
      AddWidgetDialogComponent,
      ManageDashboardLayoutsDialogComponent,
      DashboardSettingsDialogComponent,
      ManageDashboardStatesDialogComponent,
      DashboardStateDialogComponent,
      DashboardImageDialogComponent,
      EmbedDashboardDialogComponent,
      OtaUpdateEventConfigComponent,
      TargetSelectComponent,
      DisplayWidgetTypesPanelComponent,
      SolutionInstallDialogComponent,
      TenantProfileQueuesComponent,
      QueueFormComponent,
      RepositorySettingsComponent,
      VersionControlComponent,
      EntityVersionsTableComponent,
      EntityVersionCreateComponent,
      EntityVersionRestoreComponent,
      EntityVersionDiffComponent,
      ComplexVersionCreateComponent,
      EntityTypesVersionCreateComponent,
      EntityTypesVersionLoadComponent,
      ComplexVersionLoadComponent,
      RemoveOtherEntitiesConfirmComponent,
      AutoCommitSettingsComponent,
      OwnerEntityGroupListComponent,
      RateLimitsDetailsDialogComponent,
      RateLimitsComponent,
      RateLimitsListComponent,
      RateLimitsTextComponent,
      IntegrationWizardDialogComponent,
      ConverterComponent,
      ConverterTestDialogComponent,
      SlackConversationAutocompleteComponent,
      SendNotificationButtonComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    SharedHomeComponentsModule,
    HomeDialogsModule,
    WidgetSettingsModule,
    Lwm2mProfileComponentsModule,
    SnmpDeviceProfileTransportModule,
    StatesControllerModule,
    DeviceCredentialsModule,
    DeviceProfileCommonModule,
    IntegrationComponentModule
  ],
  exports: [
    SharedHomeComponentsModule,
    RouterTabsComponent,
    EntitiesTableComponent,
    AddEntityDialogComponent,
    DetailsPanelComponent,
    EntityDetailsPanelComponent,
    EntityDetailsPageComponent,
    AuditLogTableComponent,
    EventTableComponent,
    EdgeDownlinkTableHeaderComponent,
    EdgeDownlinkTableComponent,
    RelationTableComponent,
    RelationFiltersComponent,
    AlarmTableComponent,
    AlarmAssigneePanelComponent,
    AttributeTableComponent,
    AliasesEntitySelectComponent,
    AliasesEntityAutocompleteComponent,
    EntityAliasesDialogComponent,
    EntityAliasDialogComponent,
    DashboardComponent,
    WidgetContainerComponent,
    WidgetComponent,
    LegendComponent,
    WidgetSettingsComponent,
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
    MobileActionEditorComponent,
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
    ViewRoleDialogComponent,
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
    GroupChipsComponent,
    GroupEntityInfoComponent,
    OwnerAndGroupsComponent,
    ManageOwnerAndGroupsDialogComponent,
    RegistrationPermissionsComponent,
    BooleanFilterPredicateComponent,
    StringFilterPredicateComponent,
    NumericFilterPredicateComponent,
    ComplexFilterPredicateComponent,
    ComplexFilterPredicateDialogComponent,
    FilterPredicateComponent,
    FilterPredicateListComponent,
    KeyFilterListComponent,
    KeyFilterDialogComponent,
    FilterDialogComponent,
    FiltersDialogComponent,
    FilterSelectComponent,
    FilterTextComponent,
    FiltersEditComponent,
    UserFilterDialogComponent,
    TenantProfileAutocompleteComponent,
    TenantProfileDataComponent,
    TenantProfileComponent,
    TenantProfileDialogComponent,
    DeviceProfileAutocompleteComponent,
    DefaultDeviceProfileConfigurationComponent,
    DeviceProfileConfigurationComponent,
    DefaultDeviceProfileTransportConfigurationComponent,
    MqttDeviceProfileTransportConfigurationComponent,
    CoapDeviceProfileTransportConfigurationComponent,
    DeviceProfileTransportConfigurationComponent,
    CreateAlarmRulesComponent,
    AlarmRuleComponent,
    AlarmRuleConditionDialogComponent,
    AlarmRuleConditionComponent,
    DeviceProfileAlarmComponent,
    DeviceProfileAlarmsComponent,
    DeviceProfileComponent,
    DeviceProfileDialogComponent,
    AddDeviceProfileDialogComponent,
    RuleChainAutocompleteComponent,
    DeviceWizardDialogComponent,
    AssetProfileComponent,
    AssetProfileDialogComponent,
    AssetProfileAutocompleteComponent,
    AlarmScheduleInfoComponent,
    AlarmScheduleComponent,
    AlarmDynamicValue,
    AlarmScheduleDialogComponent,
    AlarmDurationPredicateValueComponent,
    EditAlarmDetailsDialogComponent,
    DeviceProfileProvisionConfigurationComponent,
    SmsProviderConfigurationComponent,
    AwsSnsProviderConfigurationComponent,
    SmppSmsProviderConfigurationComponent,
    TwilioSmsProviderConfigurationComponent,
    EntityGroupWizardDialogComponent,
    DashboardToolbarComponent,
    DashboardPageComponent,
    DashboardStateComponent,
    DashboardLayoutComponent,
    EditWidgetComponent,
    DashboardWidgetSelectComponent,
    AddWidgetDialogComponent,
    ManageDashboardLayoutsDialogComponent,
    DashboardSettingsDialogComponent,
    ManageDashboardStatesDialogComponent,
    DashboardStateDialogComponent,
    DashboardImageDialogComponent,
    EmbedDashboardDialogComponent,
    OtaUpdateEventConfigComponent,
    TargetSelectComponent,
    DisplayWidgetTypesPanelComponent,
    SolutionInstallDialogComponent,
    TenantProfileQueuesComponent,
    QueueFormComponent,
    RepositorySettingsComponent,
    VersionControlComponent,
    EntityVersionsTableComponent,
    EntityVersionCreateComponent,
    EntityVersionRestoreComponent,
    EntityVersionDiffComponent,
    ComplexVersionCreateComponent,
    EntityTypesVersionCreateComponent,
    EntityTypesVersionLoadComponent,
    ComplexVersionLoadComponent,
    RemoveOtherEntitiesConfirmComponent,
    AutoCommitSettingsComponent,
    OwnerEntityGroupListComponent,
    RateLimitsDetailsDialogComponent,
    RateLimitsComponent,
    RateLimitsListComponent,
    RateLimitsTextComponent,
    IntegrationWizardDialogComponent,
    SlackConversationAutocompleteComponent,
    SendNotificationButtonComponent
  ],
  providers: [
    WidgetComponentService,
    CustomDialogService,
    ImportExportService,
    AllEntitiesTableConfigService,
    GroupConfigTableConfigService,
    EntityGroupsTableConfigResolver,
    EntityGroupConfigResolver,
    {provide: EMBED_DASHBOARD_DIALOG_TOKEN, useValue: EmbedDashboardDialogComponent},
    {provide: COMPLEX_FILTER_PREDICATE_DIALOG_COMPONENT_TOKEN, useValue: ComplexFilterPredicateDialogComponent},
    {provide: DASHBOARD_PAGE_COMPONENT_TOKEN, useValue: DashboardPageComponent},
    {provide: HOME_COMPONENTS_MODULE_TOKEN, useValue: HomeComponentsModule },
    {provide: MODULES_MAP, useValue: modulesMap}
  ]
})
export class HomeComponentsModule {
}
