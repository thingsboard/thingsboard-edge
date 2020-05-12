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

import { CertUploadComponent } from './cert-upload/cert-upload.component';
import { HttpIntegrationFormComponent } from './http-integration-form/http-integration-form.component';
import { MqttIntegrationFormComponent } from './mqtt-integration-form/mqtt-integration-form.component';
import { OpcUaIntegrationFormComponent } from './opc-ua-integration-form/opc-ua-integration-form.component';
import { AwsKinesisIntegrationFormComponent } from './aws-kinesis-integration-form/aws-kinesis-integration-form.component';
import { AwsIotIntegrationFormComponent } from './aws-iot-integration-form/aws-iot-integration-form.component';
import { AwsSqsIntegrationFormComponent } from './aws-sqs-integration-form/aws-sqs-integration-form.component';
import { AzureEventHubIntegrationFormComponent } from './azure-event-hub-integration-form/azure-event-hub-integration-form.component';
import { IbmWatsonIotIntegrationFormComponent } from './ibm-watson-iot-integration-form/ibm-watson-iot-integration-form.component';
import { KafkaIntegrationFormComponent } from './kafka-integration-form/kafka-integration-form.component';
import { TcpIntegrationFormComponent } from './tcp-integration-form/tcp-integration-form.component';
import { TtnIntegrationFormComponent } from './ttn-integration-form/ttn-integration-form.component';
import { UdpIntegrationFormComponent } from './udp-integration-form/udp-integration-form.component';
import { MqttTopicFiltersComponent } from './mqtt-topic-filters/mqtt-topic-filters.component';
import { OpcUaSubscriptionTagsComponent } from './opc-ua-subscription-tags/opc-ua-subscription-tags.component';
import { CustomIntegrationFormComponent } from './custom-integration-form/custom-integration-form.component';

export const integrations = [
    CertUploadComponent,
    CustomIntegrationFormComponent,
    OpcUaSubscriptionTagsComponent,
    MqttTopicFiltersComponent,
    HttpIntegrationFormComponent,
    MqttIntegrationFormComponent,
    OpcUaIntegrationFormComponent,
    AwsKinesisIntegrationFormComponent,
    AwsIotIntegrationFormComponent,
    AwsSqsIntegrationFormComponent,
    AzureEventHubIntegrationFormComponent,
    IbmWatsonIotIntegrationFormComponent,
    KafkaIntegrationFormComponent,
    TcpIntegrationFormComponent,
    TtnIntegrationFormComponent,
    UdpIntegrationFormComponent,
    OpcUaSubscriptionTagsComponent
];