/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sensitivity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Stopwatch;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Sensitivity factor to be computed in the sensitivity analysis.
 * It regroups in a single object a description of the variable to increase, a description of the function to monitor
 * and a contingency context. A factor corresponds to the definition of a partial derivative to be extracted from the
 * network in a given contingency context. Usually we compute the impact of an injection increase on a branch flow or current,
 * the impact of a shift of a phase tap changer on a branch flow or current or the impact of a voltage target increase on a bus voltage.
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class SensitivityFactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityFactor.class);

    private final SensitivityFunctionType functionType;

    private final String functionId;

    private final SensitivityVariableType variableType;

    private final String variableId;

    private final boolean variableSet;

    private final ContingencyContext contingencyContext;

    /**
     * Constructor
     * @param functionType see {@link com.powsybl.sensitivity.SensitivityFunctionType}
     * @param functionId the id of the equipment to monitor (in general the id of a branch). For BUS_VOLTAGE type, see
     * {@link com.powsybl.iidm.network.IdBasedBusRef}
     * @param variableType see {@link com.powsybl.sensitivity.SensitivityVariableType}
     * @param variableId id of the equipment affected by the injection increase, the angle sift, the voltage target
     *                   increase or the active power set point increase.
     * @param variableSet boolean to says if the variable is list of weighted variables (GLSK) or not
     * @param contingencyContext see {@link com.powsybl.contingency.ContingencyContext}
     */
    public SensitivityFactor(SensitivityFunctionType functionType, String functionId, SensitivityVariableType variableType,
                              String variableId, boolean variableSet, ContingencyContext contingencyContext) {
        this.functionType = Objects.requireNonNull(functionType);
        this.functionId = Objects.requireNonNull(functionId);
        this.variableType = Objects.requireNonNull(variableType);
        this.variableId = Objects.requireNonNull(variableId);
        this.variableSet = variableSet;
        this.contingencyContext = Objects.requireNonNull(contingencyContext);
    }

    public SensitivityFunctionType getFunctionType() {
        return functionType;
    }

    public String getFunctionId() {
        return functionId;
    }

    public SensitivityVariableType getVariableType() {
        return variableType;
    }

    public String getVariableId() {
        return variableId;
    }

    public boolean isVariableSet() {
        return variableSet;
    }

    public ContingencyContext getContingencyContext() {
        return contingencyContext;
    }

    @Override
    public String toString() {
        return "SensitivityFactor(" +
                "functionType=" + functionType +
                ", functionId='" + functionId + '\'' +
                ", variableType=" + variableType +
                ", variableId='" + variableId + '\'' +
                ", variableSet=" + variableSet +
                ", contingencyContext=" + contingencyContext +
                ')';
    }

    public static void writeJson(JsonGenerator jsonGenerator, SensitivityFactor factor) {
        writeJson(jsonGenerator, factor.getFunctionType(), factor.getFunctionId(), factor.getVariableType(),
                factor.getVariableId(), factor.isVariableSet(), factor.getContingencyContext());
    }

    static void writeJson(JsonGenerator generator, List<? extends SensitivityFactor> factorList) {
        Objects.requireNonNull(factorList);
        try {
            generator.writeStartArray();
            for (SensitivityFactor factor : factorList) {
                writeJson(generator, factor);
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeJson(Writer writer, List<? extends SensitivityFactor> factorList) {
        JsonUtil.writeJson(writer, generator -> writeJson(generator, factorList));
    }

    public static void writeJson(JsonGenerator jsonGenerator, SensitivityFunctionType functionType, String functionId, SensitivityVariableType variableType,
                          String variableId, boolean variableSet, ContingencyContext contingencyContext) {
        try {
            jsonGenerator.writeStartObject();

            jsonGenerator.writeStringField("functionType", functionType.name());
            jsonGenerator.writeStringField("functionId", functionId);
            jsonGenerator.writeStringField("variableType", variableType.name());
            jsonGenerator.writeStringField("variableId", variableId);
            jsonGenerator.writeBooleanField("variableSet", variableSet);
            jsonGenerator.writeStringField("contingencyContextType", contingencyContext.getContextType().name());
            if (contingencyContext.getContingencyId() != null) {
                jsonGenerator.writeStringField("contingencyId", contingencyContext.getContingencyId());
            }

            jsonGenerator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static final class ParsingContext {
        SensitivityFunctionType functionType;
        String functionId;
        SensitivityVariableType variableType;
        String variableId;
        Boolean variableSet;
        ContingencyContextType contingencyContextType;
        String contingencyId;

        void reset() {
            functionType = null;
            functionId = null;
            variableType = null;
            variableId = null;
            variableSet = null;
            contingencyContextType = null;
            contingencyId = null;
        }
    }

    public static List<SensitivityFactor> parseJsonArray(JsonParser parser) {
        Objects.requireNonNull(parser);

        var stopwatch = Stopwatch.createStarted();

        List<SensitivityFactor> factors = new ArrayList<>();
        try {
            var context = new ParsingContext();
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    parseJson(parser, context);
                } else if (token == JsonToken.END_OBJECT) {
                    factors.add(new SensitivityFactor(context.functionType, context.functionId, context.variableType, context.variableId, context.variableSet,
                            new ContingencyContext(context.contingencyId, context.contingencyContextType)));
                    context.reset();
                } else if (token == JsonToken.END_ARRAY) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        stopwatch.stop();
        LOGGER.info("{} factors read in {} ms", factors.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        return factors;
    }

    public static SensitivityFactor parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);

        var context = new ParsingContext();
        try {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    parseJson(parser, context);
                } else if (token == JsonToken.END_OBJECT) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new SensitivityFactor(context.functionType, context.functionId, context.variableType, context.variableId, context.variableSet,
                new ContingencyContext(context.contingencyId, context.contingencyContextType));
    }

    static void parseJson(JsonParser parser, ParsingContext context) throws IOException {
        String fieldName = parser.getCurrentName();
        switch (fieldName) {
            case "functionType":
                context.functionType = SensitivityFunctionType.valueOf(parser.nextTextValue());
                break;
            case "functionId":
                context.functionId = parser.nextTextValue();
                break;
            case "variableType":
                context.variableType = SensitivityVariableType.valueOf(parser.nextTextValue());
                break;
            case "variableId":
                context.variableId = parser.nextTextValue();
                break;
            case "variableSet":
                context.variableSet = parser.nextBooleanValue();
                break;
            case "contingencyContextType":
                context.contingencyContextType = ContingencyContextType.valueOf(parser.nextTextValue());
                break;
            case "contingencyId":
                context.contingencyId = parser.nextTextValue();
                break;
            default:
                break;
        }
    }

    public static List<SensitivityFactor> readJson(Reader reader) {
        return JsonUtil.parseJson(reader, SensitivityFactor::parseJsonArray);
    }

    public static List<SensitivityFactor> readJson(Path jsonFile) {
        try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
            return SensitivityFactor.readJson(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<SensitivityFactor> createMatrix(SensitivityFunctionType functionType, List<String> functionIds,
                                                        SensitivityVariableType variableType, List<String> variableIds,
                                                        boolean variableSet, ContingencyContext contingencyContext) {
        List<SensitivityFactor> factors = new ArrayList<>();
        for (String functionId : functionIds) {
            for (String variableId : variableIds) {
                factors.add(new SensitivityFactor(functionType, functionId, variableType, variableId, variableSet, contingencyContext));
            }
        }
        return factors;
    }
}
