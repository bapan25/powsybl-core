/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SensitivityValueModelWriter implements SensitivityValueWriter {

    private final List<SensitivityFactor> factors;

    private final List<SensitivityValue> values = new ArrayList<>();

    public SensitivityValueModelWriter(List<SensitivityFactor> factors) {
        this.factors = Objects.requireNonNull(factors);
    }

    public List<SensitivityValue> getValues() {
        return values;
    }

    @Override
    public void write(String contingencyId, String variableId, String functionId, int factorIndex, int contingencyIndex, double value, double functionReference) {
        values.add(new SensitivityValue(factors.get(factorIndex), contingencyId, value, functionReference));
    }
}
