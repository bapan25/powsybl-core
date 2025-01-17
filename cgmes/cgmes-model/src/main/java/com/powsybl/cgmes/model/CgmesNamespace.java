/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.model;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class CgmesNamespace {

    private CgmesNamespace() {
    }

    // cim14 is the CIM version corresponding to ENTSO-E Profile 1
    // It is used in this project to explore how to support future CGMES versions
    // We have sample models in cim14 and we use a different set of queries to obtain data

    public static final String CIM_100_NAMESPACE = "http://iec.ch/TC57/CIM100#";
    public static final Pattern CIM_100_PLUS_NAMESPACE_PATTERN = Pattern.compile(".*/CIM[0-9]+#$");
    public static final String CIM_16_NAMESPACE = "http://iec.ch/TC57/2013/CIM-schema-cim16#";
    public static final String CIM_14_NAMESPACE = "http://iec.ch/TC57/2009/CIM-schema-cim14#";
    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String ENTSOE_NAMESPACE = "http://entsoe.eu/CIM/SchemaExtension/3/1#";
    public static final String EU_NAMESPACE = "http://iec.ch/TC57/CIM100-European#";
    public static final String MD_NAMESPACE = "http://iec.ch/TC57/61970-552/ModelDescription/1#";

    public static final String EQ_PROFILE = "http://entsoe.eu/CIM/EquipmentCore/3/1";
    public static final String EQ_OPERATION_PROFILE = "http://entsoe.eu/CIM/EquipmentOperation/3/1";
    public static final String TP_PROFILE = "http://entsoe.eu/CIM/Topology/4/1";
    public static final String SV_PROFILE = "http://entsoe.eu/CIM/StateVariables/4/1";
    public static final String SSH_PROFILE = "http://entsoe.eu/CIM/SteadyStateHypothesis/1/1";

    public static final Set<String> CIM_NAMESPACES = Set.of(CIM_14_NAMESPACE, CIM_16_NAMESPACE, CIM_100_NAMESPACE);

    public static String getCimNamespace(int cimVersion) {
        if (cimVersion == 14) {
            return CIM_14_NAMESPACE;
        }
        if (cimVersion == 16) {
            return CIM_16_NAMESPACE;
        }
        if (cimVersion == 100) {
            return CIM_100_NAMESPACE;
        }
        throw new AssertionError("Unsupported CIM version " + cimVersion);
    }
}
