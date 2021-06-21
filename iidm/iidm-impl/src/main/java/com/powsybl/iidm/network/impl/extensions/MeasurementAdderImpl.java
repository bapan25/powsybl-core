/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.MeasurementAdder;
import com.powsybl.iidm.network.extensions.Measurements;

import java.util.Objects;
import java.util.Properties;

import static com.powsybl.iidm.network.extensions.util.MeasurementValidationUtil.checkId;
import static com.powsybl.iidm.network.extensions.util.MeasurementValidationUtil.checkSide;
import static com.powsybl.iidm.network.extensions.util.MeasurementValidationUtil.checkValue;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class MeasurementAdderImpl implements MeasurementAdder {

    private final MeasurementsImpl measurements;
    private final Properties properties = new Properties();

    private String id;
    private Measurement.Type type;
    private double value = Double.NaN;
    private double standardDeviation = Double.NaN;
    private boolean valid = true;
    private Measurement.Side side;

    MeasurementAdderImpl(MeasurementsImpl measurements) {
        this.measurements = Objects.requireNonNull(measurements);
    }

    @Override
    public MeasurementAdder setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public MeasurementAdder putProperty(String name, Object property) {
        properties.put(name, property);
        return this;
    }

    @Override
    public MeasurementAdder setType(Measurement.Type type) {
        this.type = type;
        return this;
    }

    @Override
    public MeasurementAdder setValue(double value) {
        this.value = value;
        return this;
    }

    @Override
    public MeasurementAdder setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
        return this;
    }

    @Override
    public MeasurementAdder setSide(Measurement.Side side) {
        this.side = side;
        return this;
    }

    @Override
    public MeasurementAdder setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    @Override
    public Measurements add() {
        checkId(id, measurements);
        if (type == null) {
            throw new PowsyblException("Measurement type can not be null");
        }
        checkValue(value);
        checkSide(side, (Connectable) measurements.getExtendable());
        return measurements.add(new MeasurementImpl(measurements, id, type, properties, value, standardDeviation, valid, side));
    }
}
