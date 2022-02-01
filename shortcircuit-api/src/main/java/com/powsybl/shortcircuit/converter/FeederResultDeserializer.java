/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.shortcircuit.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.shortcircuit.FeederResult;

import java.io.IOException;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class FeederResultDeserializer extends StdDeserializer<FeederResult> {

    FeederResultDeserializer() {
        super(FeederResult.class);
    }

    @Override
    public FeederResult deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        String connectableId = "";
        double feederThreePhaseCurrent = 0;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "connectableId":
                    parser.nextToken();
                    connectableId = parser.readValueAs(String.class);
                    break;

                case "feederThreePhaseCurrent":
                    parser.nextToken();
                    feederThreePhaseCurrent = parser.readValueAs(Double.class);
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        return new FeederResult(connectableId, feederThreePhaseCurrent);
    }
}
