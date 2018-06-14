package org.thingsboard.rule.engine.math.state;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbAvgIntervalState extends TbBaseIntervalState {

    private BigDecimal sum = BigDecimal.ZERO;
    private long count = 0L;

    public TbAvgIntervalState(String sum, long count) {
        this.sum = new BigDecimal(sum);
        this.count = count;
    }

    @Override
    protected boolean doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
        }
        this.count++;
        return true;
    }

    @Override
    public double getValue() {
        return sum.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @Override
    public String toJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("sum", sum.toString());
        object.addProperty("count", Long.toString(count));
        return gson.toJson(object);
    }
}
