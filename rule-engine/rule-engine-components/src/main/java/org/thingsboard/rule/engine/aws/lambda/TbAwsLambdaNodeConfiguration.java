/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.rule.engine.aws.lambda;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbAwsLambdaNodeConfiguration implements NodeConfiguration<TbAwsLambdaNodeConfiguration> {

    public static final String DEFAULT_QUALIFIER = "$LATEST";

    @NotBlank
    private String accessKey;
    @NotBlank
    private String secretKey;
    @NotBlank
    private String region;
    @NotBlank
    private String functionName;
    private String qualifier;
    @Min(0)
    private int connectionTimeout;
    @Min(0)
    private int requestTimeout;
    private boolean tellFailureIfFuncThrowsExc;

    @Override
    public TbAwsLambdaNodeConfiguration defaultConfiguration() {
        TbAwsLambdaNodeConfiguration configuration = new TbAwsLambdaNodeConfiguration();
        configuration.setRegion("us-east-1");
        configuration.setQualifier(DEFAULT_QUALIFIER);
        configuration.setConnectionTimeout(10);
        configuration.setRequestTimeout(5);
        configuration.setTellFailureIfFuncThrowsExc(false);
        return configuration;
    }

}
