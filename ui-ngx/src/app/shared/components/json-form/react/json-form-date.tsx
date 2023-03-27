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
import ThingsboardBaseComponent from './json-form-base-component';
import DateFnsUtils from '@date-io/date-fns';
import { KeyboardDatePicker, MuiPickersUtilsProvider } from '@material-ui/pickers';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';

interface ThingsboardDateState extends JsonFormFieldState {
  currentValue: Date | null;
}

class ThingsboardDate extends React.Component<JsonFormFieldProps, ThingsboardDateState> {

    constructor(props) {
        super(props);
        this.onDatePicked = this.onDatePicked.bind(this);
        let value: Date | null = null;
        if (this.props.value && typeof this.props.value === 'number') {
          value = new Date(this.props.value);
        }
        this.state = {
          currentValue: value
        };
    }


    onDatePicked(date: Date | null) {
        this.setState({
          currentValue: date
        });
        this.props.onChangeValidate(date ? date.getTime() : null);
    }

    render() {

        let fieldClass = 'tb-date-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.props.form.readonly) {
            fieldClass += ' tb-readonly';
        }

        return (
          <MuiPickersUtilsProvider utils={DateFnsUtils}>
            <div style={{width: '100%', display: 'block'}}>
                <KeyboardDatePicker
                    disableToolbar
                    variant='inline'
                    format='MM/dd/yyyy'
                    margin='normal'
                    className={fieldClass}
                    label={this.props.form.title}
                    value={this.state.currentValue}
                    onChange={this.onDatePicked}
                    disabled={this.props.form.readonly}
                    style={this.props.form.style || {width: '100%'}}/>

            </div>
          </MuiPickersUtilsProvider>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardDate);
