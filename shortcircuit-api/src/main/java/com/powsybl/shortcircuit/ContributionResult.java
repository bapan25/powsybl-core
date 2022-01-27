/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.shortcircuit;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class ContributionResult {
    private final String subjectId;
    private final String id;

    private final float contribution; //in kA

    public ContributionResult(String subjectId, String id, float contribution) {
        this.subjectId = subjectId;
        this.id = id;
        this.contribution = contribution;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getId() {
        return id;
    }

    public float getContribution() {
        return contribution;
    }
}
