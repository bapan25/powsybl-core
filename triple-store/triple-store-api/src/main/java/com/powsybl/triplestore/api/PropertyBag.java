/**
 * Copyright (c) 2017-2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.triplestore.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public class PropertyBag extends HashMap<String, String> {

    public PropertyBag(List<String> propertyNames) {
        this(propertyNames, false);
    }

    public PropertyBag(List<String> propertyNames, boolean removeUnderscore) {
        super(propertyNames.size());
        this.propertyNames = propertyNames;
        this.removeInitialUnderscoreForIdentifiers = removeUnderscore;
    }

    public List<String> propertyNames() {
        return propertyNames;
    }

    public void putNonNull(String key, String value) {
        if (key != null && value != null) {
            put(key, value);
        }
    }

    public String getLocal(String property) {
        String value = get(property);
        if (value == null) {
            return null;
        }
        return value.replaceAll("^.*#", "");
    }

    public String getId(String property) {
        String value = get(property);
        if (value == null) {
            return null;
        }
        // rdf:ID is the mRID plus an underscore added at the beginning of the string
        // We may decide if we want to preserve or not the underscore
        if (removeInitialUnderscoreForIdentifiers) {
            return value.replaceAll("^.*#_?", "");
        } else {
            return value.replaceAll("^.*#", "");
        }
    }

    public String getId0(String property) {
        // Return the first part of the Id (before he first hyphen)
        String id = getId(property);
        if (id == null) {
            return null;
        }
        int h = id.indexOf('-');
        if (h < 0) {
            return id;
        }
        return id.substring(0, h);
    }

    public double asDouble(String property) {
        return asDouble(property, Double.NaN);
    }

    public double asDouble(String property, double defaultValue) {
        return asDouble(property, () -> defaultValue);
    }

    public double asDouble(String property, Supplier<Double> defaultValueSupplier) {
        if (!containsKey(property)) {
            return defaultValueSupplier.get();
        }
        try {
            return Double.parseDouble(get(property));
        } catch (NumberFormatException x) {
            LOG.warn("Invalid value for property {} : {}", property, get(property));
            return Double.NaN;
        }
    }

    public boolean asBoolean(String property, boolean defaultValue) {
        if (!containsKey(property)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(get(property));
    }

    public int asInt(String property) {
        return Integer.parseInt(get(property));
    }

    public int asInt(String property, int defaultValue) {
        if (!containsKey(property)) {
            return defaultValue;
        }
        return Integer.parseInt(get(property));
    }

    public String tabulateLocals() {
        return tabulate("", PropertyBag::getLocal);
    }

    public String tabulate() {
        return tabulate("", PropertyBag::get);
    }

    public String tabulateLocals(String title) {
        return tabulate(title, PropertyBag::getLocal);
    }

    public String tabulate(String title) {
        return tabulate(title, HashMap::get);
    }

    private String tabulate(String title, BiFunction<PropertyBag, String, String> getValue) {
        if (size() == 0) {
            return "";
        }
        String lineSeparator = System.lineSeparator();
        Optional<Integer> maxLenName = propertyNames.stream()
                .map(String::length)
                .max(Integer::compare);
        if (maxLenName.isPresent()) {
            int lenPad = maxLenName.get();
            String format = String.format("%%-%ds", lenPad);

            // Performance : avoid using concat() -> use a StringBuilder instead.
            return new StringBuilder(title).append(lineSeparator).append(propertyNames.stream()
                    .map(n -> new StringBuilder(INDENTATION).append(String.format(format, n)).append(" : ").append(getValue.apply(this, n)).toString())
                    .collect(Collectors.joining(lineSeparator))).toString();
        }
        return "";
    }

    private static String padr(String s, int size) {
        String format = String.format("%%-%ds", size);
        return String.format(format, s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), propertyNames, removeInitialUnderscoreForIdentifiers);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PropertyBag)) {
            return false;
        }
        PropertyBag p = (PropertyBag) obj;
        if (removeInitialUnderscoreForIdentifiers != p.removeInitialUnderscoreForIdentifiers) {
            return false;
        }
        return propertyNames.equals(p.propertyNames);
    }

    public boolean isResource(String name) {
        // TODO do not rely on property name, use metadata or answer based on value?
        return RESOURCE_NAMES.contains(name) || resourceNames.contains(name);
    }

    public String namespacePrefix(String name) {
        LOG.trace("namespacePrefix for property name {}", name);
        return NAMESPACE_PREFIX;
    }

    public void setResourceNames(List<String> resourceNames) {
        this.resourceNames.clear();
        this.resourceNames.addAll(Objects.requireNonNull(resourceNames));
    }

    public void setClassPropertyNames(List<String> classPropertyNames) {
        this.classPropertyNames.clear();
        this.classPropertyNames.addAll(Objects.requireNonNull(classPropertyNames));
    }

    public boolean isClassProperty(String name) {
        return classPropertyNames.contains(name);
    }

    public void setMultivaluedProperty(List<String> multiValuedPropertyNames) {
        this.multiValuedPropertyNames.clear();
        this.multiValuedPropertyNames.addAll(Objects.requireNonNull(multiValuedPropertyNames));
    }

    public boolean isMultivaluedProperty(String name) {
        return multiValuedPropertyNames.contains(name);
    }

    public PropertyBag copy() {
        // Create just a shallow copy of this property bag
        PropertyBag pb1 = new PropertyBag(propertyNames, removeInitialUnderscoreForIdentifiers);
        pb1.setResourceNames(resourceNames);
        pb1.setClassPropertyNames(classPropertyNames);
        pb1.setMultivaluedProperty(multiValuedPropertyNames);
        pb1.putAll(this);
        return pb1;
    }

    private final List<String> propertyNames;
    private final boolean removeInitialUnderscoreForIdentifiers;
    private final List<String> resourceNames = new ArrayList<>();
    private final List<String> classPropertyNames = new ArrayList<>();
    private final List<String> multiValuedPropertyNames = new ArrayList<>();

    private static final String NAMESPACE_PREFIX = "data";
    private static final String INDENTATION = "    ";
    private static final List<String> RESOURCE_NAMES = Arrays.asList("TopologicalNode", "Terminal", "ShuntCompensator",
        "TapChanger", "ConductingEquipment", "Model.DependentOn", "TopologicalNodes",
        "AngleRefTopologicalNode");

    private static final Logger LOG = LoggerFactory.getLogger(PropertyBag.class);
}
