package org.thingsboard.rule.engine.math.state;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by ashvayka on 13.06.18.
 */
@Data
@NoArgsConstructor
public class TbMinIntervalState extends TbBaseIntervalState {

    private double min = Double.MAX_VALUE;

    public TbMinIntervalState(double min) {
        this.min = min;
    }

    @Override
    protected boolean doUpdate(double value) {
        if (value < min) {
            min = value;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getValue() {
        return min;
    }

    @Override
    public String toJson(Gson gson) {
        JsonObject object = new JsonObject();
        object.addProperty("min", min);
        return gson.toJson(object);
    }
}
