/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.extensions;

import java.util.Set;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public interface Measurement {

    enum Side {
        ONE,
        TWO,
        THREE
    }

    enum Type {
        ANGLE,
        ACTIVE_POWER,
        APPARENT_POWER,
        REACTIVE_POWER,
        CURRENT,
        VOLTAGE,
        FREQUENCY,
        OTHER
    }

    String getId();

    Type getType();

    Set<String> getPropertyNames();

    Object getProperty(String name);

    Measurement putProperty(String name, Object property);

    Measurement removeProperty(String name);

    Measurement setValue(double value);

    double getValue();

    Measurement setStandardDeviation(double standardDeviation);

    double getStandardDeviation();

    boolean isValid();

    Measurement setValid(boolean valid);

    Side getSide();

    void remove();
}
