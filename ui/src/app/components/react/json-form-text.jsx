/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import React from 'react';
import ThingsboardBaseComponent from './json-form-base-component.jsx';
import TextField from 'material-ui/TextField';

class ThingsboardText extends React.Component {

    constructor(props) {
        super(props);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.state = {
            focused: false
        };
    }

    onBlur() {
        this.setState({ focused: false })
    }

    onFocus() {
        this.setState({ focused: true })
    }

    render() {

        var fieldClass = "tb-field";
        if (this.props.form.required) {
            fieldClass += " tb-required";
        }
        if (this.props.form.readonly) {
            fieldClass += " tb-readonly";
        }
        if (this.state.focused) {
            fieldClass += " tb-focused";
        }

        var multiline = this.props.form.type === 'textarea';
        var rows = multiline ? this.props.form.rows : 1;
        var rowsMax = multiline ? this.props.form.rowsMax : 1;

        return (
            <div>
                <TextField
                    className={fieldClass}
                    type={this.props.form.type}
                    floatingLabelText={this.props.form.title}
                    hintText={this.props.form.placeholder}
                    errorText={this.props.error}
                    onChange={this.props.onChangeValidate}
                    defaultValue={this.props.value}
                    disabled={this.props.form.readonly}
                    multiLine={multiline}
                    rows={rows}
                    rowsMax={rowsMax}
                    onFocus={this.onFocus}
                    onBlur={this.onBlur}
                    style={this.props.form.style || {width: '100%'}} />
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardText);