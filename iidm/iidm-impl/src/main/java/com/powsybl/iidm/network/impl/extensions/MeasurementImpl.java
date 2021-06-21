/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl.extensions;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.extensions.Measurement;

import java.util.Objects;
import java.util.Properties;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class MeasurementImpl implements Measurement {

    private final MeasurementsImpl measurements;
    private final String id;
    private final Measurement.Type type;
    private final Properties properties = new Properties();
    private final Measurement.Side side;

    private double value;
    private double standardDeviation;
    private boolean valid;

    MeasurementImpl(MeasurementsImpl measurements, String id, Measurement.Type type, Properties properties, double value, double standardDeviation, boolean valid, Measurement.Side side) {
        this.measurements = Objects.requireNonNull(measurements);
        this.id = id;
        this.type = type;
        this.properties.putAll(properties);
        this.value = value;
        this.standardDeviation = standardDeviation;
        this.valid = valid;
        this.side = side;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Object getProperty(String name) {
        return properties.getProperty(name);
    }

    @Override
    public Measurement putProperty(String name, Object property) {
        properties.put(Objects.requireNonNull(name), property);
        return this;
    }

    @Override
    public Measurement setValue(double value) {
        if (Double.isNaN(value)) {
            throw new PowsyblException("Undefined value for measurement");
        }
        this.value = value;
        return this;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public Measurement setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
        return this;
    }

    @Override
    public double getStandardDeviation() {
        return standardDeviation;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public Measurement setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    @Override
    public Side getSide() {
        return side;
    }

    @Override
    public void remove() {
        measurements.remove(this);
    }
}
