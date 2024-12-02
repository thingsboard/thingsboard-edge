/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';
import { isObject } from '@core/utils';

interface ThingsboardSelectState extends JsonFormFieldState {
  currentValue: any;
}

class ThingsboardSelect extends React.Component<JsonFormFieldProps, ThingsboardSelectState> {

  static getDerivedStateFromProps(props: JsonFormFieldProps) {
    if (props.model && props.form.key) {
      return {
        currentValue: ThingsboardSelect.getModelKey(props.model, props.form.key)
          || (props.form.titleMap != null ? props.form.titleMap[0].value : '')
      }
    }
  }

  static getModelKey(model: any, key: (string | number)[]) {
    if (Array.isArray(key)) {
      const res = key.reduce((cur, nxt) => (cur[nxt] || {}), model);
      if (res && isObject(res)) {
        return undefined;
      } else {
        return res;
      }
    } else {
      return model[key];
    }
  }

  constructor(props: JsonFormFieldProps) {
    super(props);
    this.onSelected = this.onSelected.bind(this);
    const possibleValue = ThingsboardSelect.getModelKey(this.props.model, this.props.form.key);
    this.state = {
      currentValue: this.props.model !== undefined && possibleValue ? possibleValue : this.props.form.titleMap != null ?
        this.props.form.titleMap[0].value : ''
    };
  }

  onSelected(event: SelectChangeEvent<any>) {

    this.setState({
      currentValue: event.target.value
    });
    this.props.onChangeValidate(event);
  }

  render() {
    const menuItems = this.props.form.titleMap.map((item, idx) => (
      <MenuItem key={idx}
                value={item.value}>{item.name}</MenuItem>
    ));

    return (
      <FormControl className={this.props.form.htmlClass}
                   disabled={this.props.form.readonly}
                   fullWidth={true}>
        <InputLabel variant={'standard'} htmlFor='select-field'>{this.props.form.title}</InputLabel>
        <Select
          variant={'standard'}
          value={this.state.currentValue}
          onChange={this.onSelected}>
          {menuItems}
        </Select>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardSelect);
