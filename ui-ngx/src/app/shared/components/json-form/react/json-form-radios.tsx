/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
import * as React from 'react';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import { FormLabel, Radio, RadioGroup } from '@material-ui/core';
import FormControl from '@material-ui/core/FormControl';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';

class ThingsboardRadios extends React.Component<JsonFormFieldProps, JsonFormFieldState> {
  render() {
    const items = this.props.form.titleMap.map((item, index) => {
      return (
          <FormControlLabel value={item.value} control={<Radio />} label={item.name} key={index} />
      );
    });

    let row = false;
    if (this.props.form.direction === 'row') {
      row = true;
    }

    return (
      <FormControl component='fieldset'
                   className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}>
        <FormLabel component='legend'>{this.props.form.title}</FormLabel>
        <RadioGroup row={row} name={this.props.form.title} value={this.props.value} onChange={(e) => {
          this.props.onChangeValidate(e);
        }}>
          {items}
        </RadioGroup>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardRadios);
