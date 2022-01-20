/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.util;

import com.powsybl.iidm.network.*;

import java.util.function.BiConsumer;

/**
 * Some useful utility methods to create network hypotheses.
 *
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class TopologyHypothesisUtils {

    public static void attachNewLineOnLine(double percent, String voltageLevelId, String bbsOrBusId, Line line, LineAdder lineAdder) {
        attachNewLineOnLine(percent, voltageLevelId, bbsOrBusId, line.getId() + "_VL", line.getId() + "_1",
                line.getId() + "_2", line, lineAdder);
    }

    public static void attachNewLineOnLine(double percent, String voltageLevelId, String bbsOrBusId, String fictitiousVlId,
                                           String line1Id, String line2Id, Line line, LineAdder lineAdder) {
        Network network = line.getNetwork();
        VoltageLevel fictitiousVl = network.newVoltageLevel().setId(fictitiousVlId).setFictitious(true).setNominalV(line.getTerminal1().getVoltageLevel().getNominalV()).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        LineAdder adder1 = createLineAdder(percent, line1Id, line.getTerminal1().getVoltageLevel().getId(), fictitiousVlId, network, line);
        LineAdder adder2 = createLineAdder(percent, line2Id, fictitiousVlId, line.getTerminal2().getVoltageLevel().getId(), network, line);
        attachLine(line.getTerminal1(), adder1, (bus, adder) -> adder.setConnectableBus1(bus.getId()), (bus, adder) -> adder.setBus1(bus.getId()), (node, adder) -> adder.setNode1(node));
        attachLine(line.getTerminal2(), adder2, (bus, adder) -> adder.setConnectableBus2(bus.getId()), (bus, adder) -> adder.setBus2(bus.getId()), (node, adder) -> adder.setNode2(node));
        Line line1 = adder1.setNode2(0).add();
        Line line2 = adder2.setNode1(2).add();
        addCurrentLimits(line1.newCurrentLimits1(), line.getCurrentLimits1());
        addCurrentLimits(line2.newCurrentLimits2(), line.getCurrentLimits2());
        fictitiousVl.getNodeBreakerView()
                .newInternalConnection()
                .setNode1(0)
                .setNode2(1)
                .add();
        fictitiousVl.getNodeBreakerView()
                .newInternalConnection()
                .setNode1(1)
                .setNode2(2)
                .add();
        fictitiousVl.getNodeBreakerView()
                .newInternalConnection()
                .setNode1(1)
                .setNode2(3)
                .add();
        lineAdder.setNode1(3).setVoltageLevel1(fictitiousVlId).setVoltageLevel2(voltageLevelId);
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        TopologyKind topologyKind = voltageLevel.getTopologyKind();
        if (topologyKind == TopologyKind.BUS_BREAKER) {
            Bus bus = network.getBusBreakerView().getBus(bbsOrBusId);
            Bus bus1 = voltageLevel.getBusBreakerView()
                    .newBus()
                    .setId(line.getId() + "_BUS")
                    .add();
            lineAdder.setBus2(bus1.getId());
            voltageLevel.getBusBreakerView().newSwitch()
                    .setId(line.getId() + "_SW")
                    .setOpen(false)
                    .setBus1(bus1.getId())
                    .setBus2(bus.getId())
                    .add();
        } else if (topologyKind == TopologyKind.NODE_BREAKER) {
            BusbarSection bbs = network.getBusbarSection(bbsOrBusId);
            int bbsNode = bbs.getTerminal().getNodeBreakerView().getNode();
            int firstAvailableNode = voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1;
            lineAdder.setNode2(firstAvailableNode);
            createNodeBreakerSwitches(firstAvailableNode, firstAvailableNode + 1, bbsNode, line.getId(), voltageLevel.getNodeBreakerView());
        } else {
            throw new AssertionError();
        }
        lineAdder.add();
        line.remove();
    }

    public static void createVoltageLevelOnLine(double percent, String voltageLevelId, String bbsOrBusId, Line line) {
        createVoltageLevelOnLine(percent, voltageLevelId, bbsOrBusId, line.getId() + "_1", line.getId() + "_2", line);
    }

    /**
     * Split a given line and create a fictitious voltage level at the junction.<br>
     * The characteristics of the two new lines respect the given ratios such as this:<br>
     * <code>r1 = percent * r</code><br>
     * <code>r2 = (1 - percent) * r</code><br>
     */
    public static void createVoltageLevelOnLine(double percent, String voltageLevelId, String bbsOrBusId,
                                                String line1Id, String line2Id, Line line) {
        Network network = line.getNetwork();
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        LineAdder adder1 = createLineAdder(percent, line1Id, line.getTerminal1().getVoltageLevel().getId(), voltageLevelId, network, line);
        LineAdder adder2 = createLineAdder(100 - percent, line2Id, voltageLevelId, line.getTerminal2().getVoltageLevel().getId(), network, line);
        attachLine(line.getTerminal1(), adder1, (bus, adder) -> adder.setConnectableBus1(bus.getId()), (bus, adder) -> adder.setBus1(bus.getId()), (node, adder) -> adder.setNode1(node));
        attachLine(line.getTerminal2(), adder2, (bus, adder) -> adder.setConnectableBus2(bus.getId()), (bus, adder) -> adder.setBus2(bus.getId()), (node, adder) -> adder.setNode2(node));
        TopologyKind topologyKind = voltageLevel.getTopologyKind();
        if (topologyKind == TopologyKind.BUS_BREAKER) {
            Bus bus = network.getBusBreakerView().getBus(bbsOrBusId);
            Bus bus1 = voltageLevel.getBusBreakerView()
                    .newBus()
                    .setId(line.getId() + "_BUS_1")
                    .add();
            Bus bus2 = voltageLevel.getBusBreakerView()
                    .newBus()
                    .setId(line.getId() + "_BUS_2")
                    .add();
            createBusBreakerSwitches(bus1.getId(), bus.getId(), bus2.getId(), line.getId(), voltageLevel.getBusBreakerView());
            adder1.setBus2(bus1.getId());
            adder2.setBus1(bus2.getId());
        } else if (topologyKind == TopologyKind.NODE_BREAKER) {
            BusbarSection bbs = network.getBusbarSection(bbsOrBusId);
            int bbsNode = bbs.getTerminal().getNodeBreakerView().getNode();
            int firstAvailableNode = voltageLevel.getNodeBreakerView().getMaximumNodeIndex() + 1;
            createNodeBreakerSwitches(firstAvailableNode, firstAvailableNode + 1, bbsNode, "_1", line.getId(), voltageLevel.getNodeBreakerView());
            createNodeBreakerSwitches(bbsNode, firstAvailableNode + 2, firstAvailableNode + 3, "_2", line.getId(), voltageLevel.getNodeBreakerView());
            adder1.setNode2(firstAvailableNode);
            adder2.setNode1(firstAvailableNode + 3);
        } else {
            throw new AssertionError();
        }
        Line line1 = adder1.add();
        Line line2 = adder2.add();
        addCurrentLimits(line1.newCurrentLimits1(), line.getCurrentLimits1());
        addCurrentLimits(line2.newCurrentLimits2(), line.getCurrentLimits2());
        line.remove();
    }

    private static LineAdder createLineAdder(double percent, String id, String voltageLevelId1, String voltageLevelId2, Network network, Line line) {
        return network.newLine()
                .setId(id)
                .setVoltageLevel1(voltageLevelId1)
                .setVoltageLevel2(voltageLevelId2)
                .setR(line.getR() * percent / 100)
                .setX(line.getX() * percent / 100)
                .setG1(line.getG1() * percent / 100)
                .setB1(line.getB1() * percent / 100)
                .setG2(line.getG2() * percent / 100)
                .setB2(line.getB2() * percent / 100);
    }

    private static void attachLine(Terminal terminal, LineAdder adder, BiConsumer<Bus, LineAdder> connectableBusSetter,
                                   BiConsumer<Bus, LineAdder> busSetter, BiConsumer<Integer, LineAdder> nodeSetter) {
        if (terminal.getVoltageLevel().getTopologyKind() == TopologyKind.BUS_BREAKER) {
            connectableBusSetter.accept(terminal.getBusBreakerView().getConnectableBus(), adder);
            Bus bus = terminal.getBusBreakerView().getBus();
            if (bus != null) {
                busSetter.accept(bus, adder);
            }
        } else if (terminal.getVoltageLevel().getTopologyKind() == TopologyKind.NODE_BREAKER) {
            int node = terminal.getNodeBreakerView().getNode();
            nodeSetter.accept(node, adder);
        } else {
            throw new AssertionError();
        }
    }

    private static void createBusBreakerSwitches(String busId1, String middleBusId, String busId2, String lineId, VoltageLevel.BusBreakerView view) {
        view.newSwitch()
                .setId(lineId + "_SW_1")
                .setOpen(false)
                .setBus1(busId1)
                .setBus2(middleBusId)
                .add();
        view.newSwitch()
                .setId(lineId + "_SW_2")
                .setOpen(false)
                .setBus1(middleBusId)
                .setBus2(busId2)
                .add();
    }

    private static void createNodeBreakerSwitches(int node1, int middleNode, int node2, String lineId, VoltageLevel.NodeBreakerView view) {
        createNodeBreakerSwitches(node1, middleNode, node2, "", lineId, view);
    }

    private static void createNodeBreakerSwitches(int node1, int middleNode, int node2, String suffix, String lineId, VoltageLevel.NodeBreakerView view) {
        view.newSwitch()
                .setId(lineId + "_BREAKER" + suffix)
                .setKind(SwitchKind.BREAKER)
                .setOpen(false)
                .setRetained(true)
                .setNode1(node1)
                .setNode2(middleNode)
                .add();
        view.newSwitch()
                .setId(lineId + "_DISCONNECTOR" + suffix)
                .setKind(SwitchKind.DISCONNECTOR)
                .setOpen(false)
                .setNode1(middleNode)
                .setNode2(node2)
                .add();
    }

    private static void addCurrentLimits(CurrentLimitsAdder adder, CurrentLimits currentLimits) {
        if (currentLimits != null) {
            adder.setPermanentLimit(currentLimits.getPermanentLimit());
            for (LoadingLimits.TemporaryLimit tl : currentLimits.getTemporaryLimits()) {
                adder.beginTemporaryLimit()
                        .setName(tl.getName())
                        .setAcceptableDuration(tl.getAcceptableDuration())
                        .setFictitious(tl.isFictitious())
                        .setValue(tl.getValue())
                        .endTemporaryLimit();
            }
            adder.add();
        }
    }

    private TopologyHypothesisUtils() {
    }
}
