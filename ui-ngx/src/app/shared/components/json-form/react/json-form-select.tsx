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
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import InputLabel from '@material-ui/core/InputLabel';
import Select from '@material-ui/core/Select';
import ThingsboardBaseComponent from '@shared/components/json-form/react/json-form-base-component';

interface ThingsboardSelectState extends JsonFormFieldState {
  currentValue: any;
}

class ThingsboardSelect extends React.Component<JsonFormFieldProps, ThingsboardSelectState> {

  constructor(props) {
    super(props);
    this.onSelected = this.onSelected.bind(this);
    const possibleValue = this.getModelKey(this.props.model, this.props.form.key);
    this.state = {
      currentValue: this.props.model !== undefined && possibleValue ? possibleValue : this.props.form.titleMap != null ?
        this.props.form.titleMap[0].value : ''
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.model && nextProps.form.key) {
      this.setState({
        currentValue: this.getModelKey(nextProps.model, nextProps.form.key)
          || (nextProps.form.titleMap != null ? nextProps.form.titleMap[0].value : '')
      });
    }
  }

  getModelKey(model, key) {
    if (Array.isArray(key)) {
      return key.reduce((cur, nxt) => (cur[nxt] || {}), model);
    } else {
      return model[key];
    }
  }

  onSelected(event: React.ChangeEvent<{ name?: string; value: any }>) {

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
        <InputLabel htmlFor='select-field'>{this.props.form.title}</InputLabel>
        <Select
          value={this.state.currentValue}
          onChange={this.onSelected}>
          {menuItems}
        </Select>
      </FormControl>
    );
  }
}

export default ThingsboardBaseComponent(ThingsboardSelect);
