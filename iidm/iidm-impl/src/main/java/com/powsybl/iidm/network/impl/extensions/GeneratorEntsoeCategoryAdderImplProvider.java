/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.impl.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.extensions.ExtensionAdderProvider;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.extensions.GeneratorEntsoeCategory;

/**
 * @author Jérémy Labous <jlabous at silicom.fr>
 */
@AutoService(ExtensionAdderProvider.class)
public class GeneratorEntsoeCategoryAdderImplProvider
        implements ExtensionAdderProvider<Generator, GeneratorEntsoeCategory, GeneratorEntsoeCategoryAdderImpl> {

    @Override
    public String getImplementationName() {
        return "Default";
    }

    @Override
    public String getExtensionsName() {
        return GeneratorEntsoeCategory.NAME;
    }

    @Override
    public Class<GeneratorEntsoeCategoryAdderImpl> getAdderClass() {
        return GeneratorEntsoeCategoryAdderImpl.class;
    }

    @Override
    public GeneratorEntsoeCategoryAdderImpl newAdder(Generator extendable) {
        return new GeneratorEntsoeCategoryAdderImpl(extendable);
    }
}
