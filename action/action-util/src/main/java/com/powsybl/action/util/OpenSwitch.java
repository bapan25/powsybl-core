/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.network.modification.NetworkModification;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class OpenSwitch implements NetworkModification {

    private final String switchId;

    OpenSwitch(String switchId) {
        this.switchId = Objects.requireNonNull(switchId);
    }

    @Override
    public void apply(Network network, ComputationManager computationManager) {
        apply(network);
    }

    @Override
    public void apply(Network network) {
        Switch sw = network.getSwitch(switchId);
        if (sw == null) {
            throw new PowsyblException("Switch '" + switchId + "' not found");
        }
        sw.setOpen(true);
    }
}
