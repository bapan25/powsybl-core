/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml.util;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.util.TopologyHypothesisUtils;
import com.powsybl.iidm.xml.AbstractXmlConverterTest;
import com.powsybl.iidm.xml.IidmXmlConstants;
import com.powsybl.iidm.xml.NetworkXml;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Miora Vedelago <miora.ralambotiana at rte-france.com>
 */
public class TopologyHypothesisUtilsXmlTest extends AbstractXmlConverterTest {

    @Test
    public void createVoltageLevelOnLineTest() throws IOException {
        Network network = createNetwork();
        TopologyHypothesisUtils.createVoltageLevelOnLine(50, "NHV1_NHV2_1_VL#0", "bbs", network.getLine("NHV1_NHV2_1"));
        roundTripXmlTest(network, NetworkXml::writeAndValidate, NetworkXml::validateAndRead,
                getVersionDir(IidmXmlConstants.CURRENT_IIDM_XML_VERSION) + "eurostag-line-split-vl.xml");
    }

    @Test
    public void attachLineOnLine() throws IOException {
        Network network = createNetwork();
        Line line = network.getLine("NHV1_NHV2_1");
        LineAdder adder = network.newLine()
                .setId("testLine")
                .setR(line.getR())
                .setX(line.getX())
                .setB1(line.getB1())
                .setG1(line.getG1())
                .setB2(line.getB2())
                .setG2(line.getG2());
        TopologyHypothesisUtils.attachNewLineOnLine(50, "NHV1_NHV2_1_VL#0", "bbs", line, adder);
        roundTripTest(network, NetworkXml::writeAndValidate, NetworkXml::validateAndRead,
                getVersionDir(IidmXmlConstants.CURRENT_IIDM_XML_VERSION) + "eurostag-line-split-l.xml");
    }

    private static Network createNetwork() {
        Network network = EurostagTutorialExample1Factory.create();
        VoltageLevel vl = network.newVoltageLevel().setId("NHV1_NHV2_1_VL#0").setNominalV(380).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        vl.getNodeBreakerView().newBusbarSection().setId("bbs").setNode(0).add();
        network.setCaseDate(DateTime.parse("2021-08-27T14:44:56.567+02:00"));
        return network;
    }
}
