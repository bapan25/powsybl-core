/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.measurements;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.MeasurementAdder;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.iidm.network.extensions.MeasurementsAdder;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.powsybl.iidm.network.extensions.Measurement.Side.THREE;
import static com.powsybl.iidm.network.extensions.Measurement.Side.TWO;
import static com.powsybl.iidm.network.extensions.Measurement.Type.*;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
final class CgmesAnalogPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CgmesAnalogPostProcessor.class);

    static void process(Network network, String id, String terminalId, String powerSystemResourceId, String measurementType, PropertyBags bays) {
        if (terminalId != null) {
            Identifiable identifiable = network.getIdentifiable(terminalId);
            if (identifiable != null) {
                createMeas(identifiable, id, terminalId, measurementType);
                return;
            }
            LOG.warn("Ignored terminal {} of {} {}: not found", terminalId, measurementType, id);
        }
        Identifiable<?> identifiable = network.getIdentifiable(powerSystemResourceId);
        if (identifiable != null) {
            createMeas(identifiable, id, terminalId, measurementType);
            return;
        }
        PropertyBag bay = bays.stream().filter(b -> b.getId("Bay").equals(powerSystemResourceId)).findFirst().orElse(null);
        if (bay != null) {
            String voltageLevelId = bay.getId("VoltageLevel");
            LOG.info("Power resource system {} of Analog {} is a Bay: Analog is attached to the associated voltage level {}",
                    powerSystemResourceId, id, voltageLevelId);
            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
            if (voltageLevel == null) {
                LOG.warn("Ignored {} {}: associated voltage level {} not found", measurementType, id, voltageLevelId);
                return;
            }
            voltageLevel.setProperty(Conversion.CGMES_PREFIX_ALIAS_PROPERTIES + measurementType, id);
        } else {
            LOG.warn("Ignored {} {}: attached power system resource {} not found", measurementType, id, powerSystemResourceId);
        }
    }

    private static void createMeas(Identifiable<?> identifiable, String id, String terminalId, String measurementType) {
        if (identifiable instanceof Connectable) {
            Connectable<?> c = (Connectable<?>) identifiable;
            Measurements meas = c.getExtension(Measurements.class);
            if (meas == null) {
                c.newExtension(MeasurementsAdder.class).add();
                meas = c.getExtension(Measurements.class);
            }
            Measurement.Type type = getType(measurementType);
            Measurement.Side side = null;
            MeasurementAdder adder = meas.newMeasurement()
                    .setValid(false)
                    .setId(id);
            if (!(c instanceof Injection)) {
                side = getSide(terminalId, c);
            }
            if (type != OTHER && side == null && !(c instanceof Injection)) {
                adder.setType(OTHER); // TODO: why are not OTHER measurements without terminal ID?
            } else {
                adder.setType(type);
            }
            adder.setSide(side);
            Measurement measurement = adder.add();
            if (measurement.getType() == OTHER) {
                measurement.putProperty("type", measurementType);
            }
        } else {
            identifiable.setProperty(Conversion.CGMES_PREFIX_ALIAS_PROPERTIES + "Analog_" + measurementType, id);
        }
    }

    private static Measurement.Type getType(String measurementType) {
        switch (measurementType) {
            case "ActivePower":
                return ACTIVE_POWER;
            case "ApparentPower":
                return APPARENT_POWER;
            case "Current":
                return CURRENT;
            case "Angle":
                return ANGLE;
            case "Frequency":
                return FREQUENCY;
            case "ReactivePower":
                return REACTIVE_POWER;
            case "Voltage":
                return VOLTAGE;
            default:
                return OTHER;
        }
    }

    private static Measurement.Side getSide(String terminalId, Connectable<?> c) {
        if (terminalId != null) {
            String terminalType = c.getAliasType(terminalId).orElse(null);
            if (terminalType != null) {
                if (terminalType.endsWith("1")) {
                    return Measurement.Side.ONE;
                } else if (terminalType.endsWith("2")) {
                    return TWO;
                } else if (terminalType.endsWith("3")) {
                    return THREE;
                }
            }
        }
        return null;
    }

    private CgmesAnalogPostProcessor() {
    }
}
