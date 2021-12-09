/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sensitivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.sensitivity.json.SensitivityJson;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SensitivityVariableSetTest extends AbstractConverterTest {

    private static final double EPSILON_COMPARISON = 1e-5;

    @Test
    public void test() {
        WeightedSensitivityVariable variable = new WeightedSensitivityVariable("v1", 3.4);
        assertEquals("v1", variable.getId());
        assertEquals(3.4, variable.getWeight(), EPSILON_COMPARISON);
        assertEquals("WeightedSensitivityVariable(id='v1', weight=3.4)", variable.toString());
        SensitivityVariableSet variableSet = new SensitivityVariableSet("id", List.of(variable));
        assertEquals("id", variableSet.getId());
        assertEquals(1, variableSet.getVariables().size());
        assertNotNull(variableSet.getVariables().stream().findFirst());
        assertEquals("v1", variableSet.getVariables().stream().findFirst().get().getId());
        assertEquals("v1", variableSet.getVariablesById().get("v1").getId());
        assertEquals(3.4, variableSet.getVariablesById().get("v1").getWeight(), EPSILON_COMPARISON);
        assertEquals("SensitivityVariableSet(id='id', variables={v1=WeightedSensitivityVariable(id='v1', weight=3.4)})", variableSet.toString());
    }

    @Test
    public void testJson() throws IOException {
        SensitivityVariableSet variableSet = new SensitivityVariableSet("id", List.of(new WeightedSensitivityVariable("v1", 3.4),
                                                                                      new WeightedSensitivityVariable("v2", 2.1)));
        ObjectMapper objectMapper = SensitivityJson.createObjectMapper();
        roundTripTest(variableSet, (variableSet2, jsonFile) -> JsonUtil.writeJson(jsonFile, variableSet, objectMapper),
            jsonFile -> JsonUtil.readJson(jsonFile, SensitivityVariableSet.class, objectMapper), "/variableSetRef.json");
    }
}