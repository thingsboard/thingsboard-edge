/**
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
package org.thingsboard.script.api.tbel;

import lombok.Data;
import java.time.chrono.IsoChronology;
@Data
public class TbDateTestEntity {
    private int year;
    private int month;
    private int date;
    private int hours;
    public TbDateTestEntity(int year, int month, int date, int hours) {
        this.year = year;
        this.month = month;
        this.date = date;
        this.hours = hours;
        if (hours > 23) {
            if (date == 31) {
                this.year++;
                this.month = 1;
                this.date = 1;
            } else {
                this.date++;
            }
            this.hours = hours - 24;
        } else if (hours < 0) {
            if (month== 1 && date == 1) {
                this.year--;
                this.month = 12;
                this.date = 31;
            } else {
                this.date--;
            }
            this.hours = hours + 24;
        }

        if (this.date > 28) {
            int dom = 31;
            switch (month) {
                case 2:
                    dom = IsoChronology.INSTANCE.isLeapYear((long) year) ? 29 : 28;
                case 3:
                case 5:
                case 7:
                case 8:
                case 10:
                default:
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    dom = 30;
            }
            if (this.date > dom) {
                this.date = this.date - dom;
                this.month++;
            }
        }
    }
    public int getYear(){
        return year < 70 ? 2000 + year : year <= 99 ? 1900 + year : year;
    }

    public String geMonthStr(){
        return String.format("%02d", month);
    }

    public String geDateStr(){
        return String.format("%02d", date);
    }

    public String geHoursStr(){
        return String.format("%02d", hours);
    }
}
