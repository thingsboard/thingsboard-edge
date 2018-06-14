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
public class TbSumIntervalState extends TbBaseIntervalState {

    private BigDecimal sum = BigDecimal.ZERO;

    public TbSumIntervalState(String sum) {
        this.sum = new BigDecimal(sum);
    }

    @Override
    protected boolean doUpdate(double value) {
        if (value != 0.0) {
            sum = sum.add(BigDecimal.valueOf(value));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getValue() {
        return sum.doubleValue();
    }

    @Override
    public String toJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("sum", sum.toString());
        return gson.toJson(object);
    }
}
