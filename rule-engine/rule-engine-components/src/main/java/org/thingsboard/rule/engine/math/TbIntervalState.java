package org.thingsboard.rule.engine.math;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by ashvayka on 07.06.18.
 */
@Data
@Builder
class TbIntervalState {

    private double min;
    private double max;
    private BigDecimal sum;
    private long count;

}
