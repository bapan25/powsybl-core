/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NodeAccessRights {

    private final Map<String, Integer> usersRights;

    private final Map<String, Integer> groupsRights;

    private Integer othersRights;

    public NodeAccessRights() {
        this(new HashMap<>(), new HashMap<>(), null);
    }

    public NodeAccessRights(Map<String, Integer> usersRights, Map<String, Integer> groupsRights, Integer othersRights) {
        this.usersRights = Objects.requireNonNull(usersRights);
        this.groupsRights = Objects.requireNonNull(groupsRights);
        this.othersRights = othersRights;
    }

    public Map<String, Integer> getUsersRights() {
        return usersRights;
    }

    public NodeAccessRights setUserRights(String user, Integer rights) {
        Objects.requireNonNull(user);
        usersRights.put(user, rights);
        return this;
    }

    public Map<String, Integer> getGroupsRights() {
        return groupsRights;
    }

    public NodeAccessRights setGroupRights(String group, Integer rights) {
        Objects.requireNonNull(group);
        groupsRights.put(group, rights);
        return this;
    }

    public Integer getOthersRights() {
        return othersRights;
    }

    public NodeAccessRights setOthersRights(Integer rights) {
        othersRights = rights;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usersRights, groupsRights, othersRights);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeAccessRights) {
            NodeAccessRights other = (NodeAccessRights) obj;
            return usersRights.equals(other.usersRights) && groupsRights.equals(other.groupsRights) &&
                    (Objects.isNull(othersRights) ? (Objects.isNull(other.othersRights)) : (othersRights.equals(other.othersRights)));
        }
        return false;
    }

    @Override
    public String toString() {
        return "NodeAccessRights(usersRights=" + usersRights + ", groupsRights=" + groupsRights +
                ", othersRights=" + othersRights + ")";

    }

}
