/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sensitivity;

import org.jgrapht.alg.util.Triple;

import java.util.*;

/**
 * Sensitivity analysis result
 *
 * <p>
 *     Mainly composed of the lists of sensitivity values in N, and optionally in N-1
 * </p>
 *
 * A single sensitivity analysis should return, besides its status and some stats on the
 * analysis itself, all the sensitivity values for each factor (combination of a monitoredBranch and a specific
 * equipment or group of equipments). The HADES2 sensitivity provider used with Powsybl offers the
 * possibility to calculate the sensitivity on a set of contingencies besides the N state.
 * The analysis is launched only once, but the solver itself
 * modifies the matrix for each state of the network to output a full set of results.
 * In the sensitivity API, it has been allowed to provide a list of contingencies as an optional input,
 * which then triggers such a sensitivity analysis.
 * The full set of results consists of :
 *  - the list of sensitivity values in N
 *  - the lists of sensitivity values for each N-1 situation
 *  - some metadata (status, stats, logs)
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @see SensitivityValue
 */
public class SensitivityAnalysisResult {

    private final boolean ok;

    private final Map<String, String> metrics;

    private final String logs;

    private final List<SensitivityValue> values;

    private Map<String, List<SensitivityValue>> valuesByContingencyId = new HashMap<>();

    private Map<Triple<String, String, String>, SensitivityValue> valuesByContingencyIdAndFunctionIdAndVariableId = new HashMap<>();

    /**
     * Hades2 sensitivity analysis result
     *
     * @param ok true if the analysis succeeded, false otherwise
     * @param metrics map of metrics about the analysis
     * @param logs analysis logs
     * @param values result values of the sensitivity analysis in N
     */
    public SensitivityAnalysisResult(boolean ok,
                                     Map<String, String> metrics,
                                     String logs,
                                     List<SensitivityValue> values) {
        this.ok = ok;
        this.metrics = Objects.requireNonNull(metrics);
        this.logs = Objects.requireNonNull(logs);
        this.values = Objects.requireNonNull(values);
        for (SensitivityValue value : values) {
            SensitivityFactor factor = value.getFactor();
            valuesByContingencyId.computeIfAbsent(value.getContingencyId(), k -> new ArrayList<>())
                    .add(value);
            valuesByContingencyIdAndFunctionIdAndVariableId.put(Triple.of(value.getContingencyId(), factor.getFunctionId(), factor.getVariableId()), value);
        }
    }

    /**
     * Get the status of the sensitivity analysis
     *
     * @return true if the analysis is ok, false otherwise
     */
    public boolean isOk() {
        return ok;
    }

    /**
     * Get some metrics about analysis execution.
     * Content may vary a lot depending of the implementation
     *
     * @return the metrics of the execution
     */
    public Map<String, String> getMetrics() {
        return metrics;
    }

    /**
     * Get analysis logs.
     *
     * @return the analysis logs
     */
    public String getLogs() {
        return logs;
    }

    /**
     * Get a collection of all the sensitivity values in state N.
     *
     * @return a collection of all the sensitivity values in state N.
     */
    public Collection<SensitivityValue> getValues() {
        return Collections.unmodifiableCollection(values);
    }

    /**
     * Get a collection of sensitivity value associated with given contingency ID
     *
     * @param contingencyId the ID of the considered contingency
     * @return the sensitivity value associated with given contingency ID
     */
    public List<SensitivityValue> getValues(String contingencyId) {
        return valuesByContingencyId.getOrDefault(contingencyId, Collections.emptyList());
    }

    /**
     * Get the sensitivity value associated with given function and given variable for a specific contingency.
     *
     * @param contingencyId the ID of the considered contingency
     * @param functionId sensitivity function ID
     * @param variableId sensitivity variable ID
     * @return the sensitivity value associated with given function and given variable for given contingency
     */
    public SensitivityValue getValue(String contingencyId, String functionId, String variableId) {
        return valuesByContingencyIdAndFunctionIdAndVariableId.get(Triple.of(contingencyId, functionId, variableId));
    }

    /**
     * Get the status of the presence of contingencies
     *
     * @return true if the analysis contains contingencies, false otherwise
     */
    public boolean contingenciesArePresent() {
        return !valuesByContingencyId.isEmpty();
    }

    /**
     * Get a collection of all the sensitivity values for all contingencies.
     *
     * @return a collection of all the sensitivity values for all contingencies.
     */
    public Map<String, List<SensitivityValue>> getValuesByContingencyId() {
        return valuesByContingencyId;
    }

    public static SensitivityAnalysisResult empty() {
        return new SensitivityAnalysisResult(false, Collections.emptyMap(), "", Collections.emptyList());
    }
}
