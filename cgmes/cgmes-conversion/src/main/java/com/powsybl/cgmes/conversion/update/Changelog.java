/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cgmes.conversion.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.powsybl.cgmes.model.CgmesNames;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkListener;

/**
 * @author Elena Kaltakova <kaltakovae at aia.es>
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class Changelog implements NetworkListener {

    /**
     * Register Network changes
     *
     * @param network the IIDM Network
     */
    public Changelog(Network network) {
        Objects.requireNonNull(network);
        network.addListener(this);
        this.baseChanges = new ArrayList<>();
        this.changesByVariant = new HashMap<>();
    }

    @Override
    public void onCreation(Identifiable identifiable) {
        baseChanges.add(new IidmChangeCreation(identifiable));
    }

    @Override
    public void onRemoval(Identifiable identifiable) {
        baseChanges.add(new IidmChangeRemoval(identifiable));
    }

    @Override
    public void onUpdate(Identifiable identifiable, String attribute, Object oldValue, Object newValue) {
        if (!ignoredAttribute(identifiable, attribute)) {
            baseChanges.add(new IidmChangeUpdate(identifiable, attribute, oldValue, newValue));
        }
    }

    @Override
    public void onUpdate(Identifiable identifiable, String attribute, String variantId, Object oldValue,
        Object newValue) {
        // Create a new list of changes if no changelog is found for the variant
        // or if the previous changelog was null
        if (!ignoredAttribute(identifiable, attribute)) {
            changesByVariant.computeIfAbsent(variantId, k -> new ArrayList<>())
                .add(new IidmChangeUpdate(identifiable, attribute, oldValue, newValue));
        }
    }

    private boolean ignoredAttribute(Identifiable identifiable, String attribute) {
        String name = identifiable.getClass().getSimpleName();
        if (IGNORED_ATTRIBUTES.contains(attribute)) {
            return true;
        }
        return IGNORED_ATTRIBUTES_PER_CLASS.containsKey(name)
            && (IGNORED_ATTRIBUTES_PER_CLASS.get(name).contains(attribute));
    }

    @Override
    public void onVariantCreated(String sourceVariantId, String targetVariantId) {
        // Will overwrite any previous changelog saved for target variant
        List<IidmChange> sourceChanges = changesByVariant.get(sourceVariantId);
        if (sourceChanges != null) {
            changesByVariant.put(targetVariantId, new ArrayList<>(sourceChanges));
        } else {
            // Source changelog is empty
            // Remove any previous changelog existing for target variant
            changesByVariant.remove(targetVariantId);
            // Specific changelog for target will be created with first change received
        }
    }

    public List<IidmChange> getChangesForVariant(String variantId) {
        if (!changesByVariant.containsKey(variantId)) {
            // If we only have baseChanges we assume they are already ordered
            return Collections.unmodifiableList(baseChanges);
        } else {
            SortedSet<IidmChange> ss = Collections.synchronizedSortedSet(new TreeSet<>(
                Comparator.comparing(IidmChange::getIndex)));
            ss.addAll(baseChanges);
            ss.addAll(changesByVariant.get(variantId));
            return new ArrayList<>(Collections.unmodifiableCollection(ss));
        }
    }

    private final List<IidmChange> baseChanges;
    private final Map<String, List<IidmChange>> changesByVariant;

    private static final String CONNECTED_COMPONENT_NUMBER = "connectedComponentNumber";
    private static final String SYNCHRONOUS_COMPONENT_NUMBER = "synchronousComponentNumber";
    private static final Set<String> IGNORED_ATTRIBUTES = new HashSet<>(Arrays.asList(
        CONNECTED_COMPONENT_NUMBER,
        SYNCHRONOUS_COMPONENT_NUMBER));
    private static final Map<String, Set<String>> IGNORED_ATTRIBUTES_PER_CLASS = new HashMap<>();

    static {
        Set<String> load = Stream.of(CgmesNames.VOLTAGE, CgmesNames.ANGLE, CgmesNames.P, CgmesNames.Q)
            .collect(Collectors.toCollection(HashSet::new));
        IGNORED_ATTRIBUTES_PER_CLASS.put("LoadImpl", load);

        Set<String> line = Stream
            .of(CgmesNames.VOLTAGE, CgmesNames.ANGLE, CgmesNames.P1, CgmesNames.Q1, CgmesNames.P2, CgmesNames.Q2)
            .collect(Collectors.toCollection(HashSet::new));
        IGNORED_ATTRIBUTES_PER_CLASS.put("LineImpl", line);

        Set<String> generator = Stream.of(CgmesNames.VOLTAGE, CgmesNames.ANGLE, CgmesNames.P, CgmesNames.Q)
            .collect(Collectors.toCollection(HashSet::new));
        IGNORED_ATTRIBUTES_PER_CLASS.put("GeneratorImpl", generator);

        Set<String> shuntCompensator = Stream.of(CgmesNames.VOLTAGE, CgmesNames.ANGLE, CgmesNames.Q)
            .collect(Collectors.toCollection(HashSet::new));
        IGNORED_ATTRIBUTES_PER_CLASS.put("ShuntCompensatorImpl", shuntCompensator);

        Set<String> twoWindingsTransformer = Stream
            .of(CgmesNames.VOLTAGE, CgmesNames.ANGLE, CgmesNames.P1, CgmesNames.Q1, CgmesNames.P2, CgmesNames.Q2)
            .collect(Collectors.toCollection(HashSet::new));
        IGNORED_ATTRIBUTES_PER_CLASS.put("TwoWindingsTransformerImpl", twoWindingsTransformer);
    }

}
