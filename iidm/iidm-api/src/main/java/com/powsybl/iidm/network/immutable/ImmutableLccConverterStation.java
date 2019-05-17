/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.network.immutable;

import com.powsybl.iidm.network.ConnectableType;
import com.powsybl.iidm.network.LccConverterStation;
import com.powsybl.iidm.network.Terminal;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An immutable {@link LccConverterStation}.
 * It is a read-only object, any modification on it will throw a runtime exception.
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
final class ImmutableLccConverterStation extends AbstractImmutableIdentifiable<LccConverterStation> implements LccConverterStation {

    ImmutableLccConverterStation(LccConverterStation identifiable, ImmutableCacheIndex cache) {
        super(identifiable, cache);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getPowerFactor() {
        return identifiable.getPowerFactor();
    }

    /**
     * Mutative operation is not allowed. It will throw an exception in runtime.
     * @return
     */
    @Override
    public LccConverterStation setPowerFactor(float powerFactor) {
        throw ImmutableNetwork.createUnmodifiableNetworkException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HvdcType getHvdcType() {
        return identifiable.getHvdcType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getLossFactor() {
        return identifiable.getLossFactor();
    }

    /**
     * Mutative operation is not allowed. It will throw an exception in runtime.
     * @return
     */
    @Override
    public LccConverterStation setLossFactor(float lossFactor) {
        throw ImmutableNetwork.createUnmodifiableNetworkException();
    }

    /**
     * {@inheritDoc}
     * @return Returns an {@link ImmutableTerminal}
     */
    @Override
    public Terminal getTerminal() {
        return cache.getTerminal(identifiable.getTerminal());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectableType getType() {
        return identifiable.getType();
    }

    /**
     * {@inheritDoc}
     * Terminals are wrapped in {@link ImmutableTerminal}.
     */
    @Override
    public List<? extends Terminal> getTerminals() {
        return identifiable.getTerminals().stream().map(cache::getTerminal).collect(Collectors.toList());
    }

    /**
     * Mutative operation is not allowed. It will throw an exception in runtime.
     * @return
     */
    @Override
    public void remove() {
        throw ImmutableNetwork.createUnmodifiableNetworkException();
    }
}