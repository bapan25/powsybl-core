/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.contingency;

import com.powsybl.contingency.tasks.AbstractTrippingTask;
import com.powsybl.contingency.tasks.ThreeWindingsTransformerTripping;

import java.util.Objects;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class ThreeWindingsTransformerContingency implements ContingencyElement {

    private final String id;

    public ThreeWindingsTransformerContingency(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ContingencyElementType getType() {
        return ContingencyElementType.THREE_WINDINGS_TRANSFORMER;
    }

    @Override
    public AbstractTrippingTask toTask() {
        return new ThreeWindingsTransformerTripping(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ThreeWindingsTransformerContingency) {
            ThreeWindingsTransformerContingency that = (ThreeWindingsTransformerContingency) o;
            return Objects.equals(id, that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
