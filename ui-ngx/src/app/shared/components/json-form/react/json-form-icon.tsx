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
import * as ReactDOM from 'react-dom';
import ThingsboardBaseComponent from './json-form-base-component';
import reactCSS from 'reactcss';
import TextField from '@material-ui/core/TextField';
import IconButton from '@material-ui/core/IconButton';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import Clear from '@material-ui/icons/Clear';
import Icon from '@material-ui/core/Icon';
import Tooltip from '@material-ui/core/Tooltip';

interface ThingsboardIconState extends JsonFormFieldState {
  icon: string | null;
  focused: boolean;
}

class ThingsboardIcon extends React.Component<JsonFormFieldProps, ThingsboardIconState> {

    constructor(props) {
        super(props);
        this.onBlur = this.onBlur.bind(this);
        this.onFocus = this.onFocus.bind(this);
        this.onValueChanged = this.onValueChanged.bind(this);
        this.onIconClick = this.onIconClick.bind(this);
        this.onClear = this.onClear.bind(this);
        const icon = props.value ? props.value : '';
        this.state = {
            icon,
            focused: false
        };
    }

    onBlur() {
      this.setState({focused: false});
    }

    onFocus() {
      this.setState({focused: true});
    }

    componentDidMount() {
        const node = ReactDOM.findDOMNode(this);
        const iconContainer = $(node).children('#icon-container');
        iconContainer.click((event) => {
          if (!this.props.form.readonly) {
            this.onIconClick(event);
          }
        });
    }

    componentWillUnmount() {
        const node = ReactDOM.findDOMNode(this);
        const iconContainer = $(node).children('#icon-container');
        iconContainer.off( 'click' );
    }

    onValueChanged(value: string | null) {
        const icon = value;
        this.setState({
            icon: value
        });
        this.props.onChange(this.props.form.key, value);
    }

    onIconClick(event) {
      this.props.onIconClick(this.props.form.key, this.state.icon,
        (color) => {
          this.onValueChanged(color);
        }
      );
    }

    onClear(event) {
        if (event) {
            event.stopPropagation();
        }
        this.onValueChanged('');
    }

    render() {

        const styles = reactCSS({
            default: {
                container: {
                    display: 'flex',
                    flexDirection: 'row',
                    alignItems: 'center'
                },
                icon: {
                    marginRight: '10px',
                    marginBottom: 'auto',
                    cursor: 'pointer',
                    border: 'solid 1px rgba(0, 0, 0, .27)',
                    borderRadius: '0'
                },
                iconContainer: {
                    display: 'flex',
                    width: '100%'
                },
                iconText: {
                    width: '100%'
                },
            },
        });

        let fieldClass = 'tb-field';
        if (this.props.form.required) {
            fieldClass += ' tb-required';
        }
        if (this.state.focused) {
            fieldClass += ' tb-focused';
        }

        let pickedIcon = 'more_horiz';
        let icon = '';
        if (this.state.icon !== '') {
            pickedIcon = this.state.icon;
            icon = this.state.icon;
        }

        return (
            <div style={ styles.container }>
                 <div id='icon-container' style={ styles.iconContainer }>
                    <IconButton style={ styles.icon }>
                      <Icon>{pickedIcon}</Icon>
                    </IconButton>
                    <TextField
                        className={fieldClass}
                        label={this.props.form.title}
                        error={!this.props.valid}
                        helperText={this.props.valid ? this.props.form.placeholder : this.props.error}
                        value={icon}
                        disabled={this.props.form.readonly}
                        onFocus={this.onFocus}
                        onBlur={this.onBlur}
                        style={ styles.iconText } />
                 </div>
                 <Tooltip title='Clear' placement='top'><IconButton onClick={this.onClear}><Clear/></IconButton></Tooltip>
            </div>
        );
    }
}

export default ThingsboardBaseComponent(ThingsboardIcon);
