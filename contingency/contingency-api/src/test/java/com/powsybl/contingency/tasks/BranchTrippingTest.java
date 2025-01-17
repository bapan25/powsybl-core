/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.contingency.tasks;

import com.google.common.collect.Sets;
import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FictitiousSwitchFactory;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Mathieu Bague <mathieu.bague at rte-france.com>
 */
public class BranchTrippingTest extends AbstractTrippingTest {

    @Test
    public void lineTrippingTest() {
        Network network = EurostagTutorialExample1Factory.create();
        Line line = network.getLine("NHV1_NHV2_1");

        assertTrue(line.getTerminal1().isConnected());
        assertTrue(line.getTerminal2().isConnected());

        Contingency contingency = Contingency.line("NHV1_NHV2_1", "VLHV2");
        contingency.toTask().modify(network, null);

        assertTrue(line.getTerminal1().isConnected());
        assertFalse(line.getTerminal2().isConnected());

        contingency = Contingency.line("NHV1_NHV2_1");
        contingency.toTask().modify(network, null);

        assertFalse(line.getTerminal1().isConnected());
        assertFalse(line.getTerminal2().isConnected());

        Exception e1 = assertThrows(PowsyblException.class, () -> Contingency.line("NOT_EXISTS").toTask().modify(network, null));
        assertEquals("Line 'NOT_EXISTS' not found", e1.getMessage());

        Exception e2 = assertThrows(PowsyblException.class, () -> Contingency.line("NHV1_NHV2_1", "NOT_EXISTS_VL").toTask().modify(network, null));
        assertEquals("VoltageLevel 'NOT_EXISTS_VL' not connected to line 'NHV1_NHV2_1'", e2.getMessage());
    }

    @Test
    public void transformerTrippingTest() {
        Network network = EurostagTutorialExample1Factory.create();
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer("NHV2_NLOAD");

        assertTrue(transformer.getTerminal1().isConnected());
        assertTrue(transformer.getTerminal2().isConnected());

        Contingency contingency = Contingency.twoWindingsTransformer("NHV2_NLOAD", "VLHV2");
        contingency.toTask().modify(network, null);

        assertFalse(transformer.getTerminal1().isConnected());
        assertTrue(transformer.getTerminal2().isConnected());

        contingency = Contingency.twoWindingsTransformer("NHV2_NLOAD");
        contingency.toTask().modify(network, null);

        assertFalse(transformer.getTerminal1().isConnected());
        assertFalse(transformer.getTerminal2().isConnected());

        Exception e1 = assertThrows(PowsyblException.class, () -> Contingency.twoWindingsTransformer("NOT_EXISTS").toTask().modify(network, null));
        assertEquals("Two windings transformer 'NOT_EXISTS' not found", e1.getMessage());
        Exception e2 = assertThrows(PowsyblException.class, () -> Contingency.twoWindingsTransformer("NHV2_NLOAD", "NOT_EXISTS_VL").toTask().modify(network, null));
        assertEquals("VoltageLevel 'NOT_EXISTS_VL' not connected to the two windings transformer 'NHV2_NLOAD'", e2.getMessage());
    }

    @Test
    public void legacyTest() {
        Network network = EurostagTutorialExample1Factory.create();
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer("NHV2_NLOAD");

        assertTrue(transformer.getTerminal1().isConnected());
        assertTrue(transformer.getTerminal2().isConnected());

        Contingency contingency = Contingency.branch("NHV2_NLOAD", "VLHV2");
        contingency.toTask().modify(network, null);

        assertFalse(transformer.getTerminal1().isConnected());
        assertTrue(transformer.getTerminal2().isConnected());

        contingency = Contingency.branch("NHV2_NLOAD");
        contingency.toTask().modify(network, null);

        assertFalse(transformer.getTerminal1().isConnected());
        assertFalse(transformer.getTerminal2().isConnected());
    }

    @Test(expected = PowsyblException.class)
    public void unknownBranchTrippingTest() {
        Network network = EurostagTutorialExample1Factory.create();

        BranchTripping tripping = new BranchTripping("transformer");
        tripping.modify(network, null);
    }

    @Test(expected = PowsyblException.class)
    public void unknownSubstationTrippingTest() {
        Network network = EurostagTutorialExample1Factory.create();

        BranchTripping tripping = new BranchTripping("NHV2_NLOAD", "UNKNOWN");
        tripping.modify(network, null);
    }

    @Test
    public void fictitiousSwitchTest() {
        Set<String> switchIds = Sets.newHashSet("BD", "BL");

        Network network = FictitiousSwitchFactory.create();
        List<Boolean> expectedSwitchStates = getSwitchStates(network, switchIds);

        BranchTripping tripping = new BranchTripping("CJ", "C");

        Set<Switch> switchesToOpen = new HashSet<>();
        Set<Terminal> terminalsToDisconnect = new HashSet<>();
        tripping.traverse(network, null, switchesToOpen, terminalsToDisconnect);
        assertEquals(switchIds, switchesToOpen.stream().map(Switch::getId).collect(Collectors.toSet()));
        assertEquals(Collections.emptySet(), terminalsToDisconnect);

        tripping.modify(network, null);
        assertTrue(network.getSwitch("BD").isOpen());
        assertTrue(network.getSwitch("BL").isOpen());

        List<Boolean> switchStates = getSwitchStates(network, switchIds);
        assertEquals(expectedSwitchStates, switchStates);
    }
}
