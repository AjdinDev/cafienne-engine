/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.cmmn.actorapi.command.team.CurrentMember;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.UserEventDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Set;

public class UserEvent extends EventListener<UserEventDefinition> {
    public UserEvent(String id, int index, ItemDefinition itemDefinition, UserEventDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
    }

    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return getDefinition().getAuthorizedRoles();
    }

    @Override
    public void validateTransition(Transition transition) {
        super.validateTransition(transition);
        if (transition != Transition.Occur) { // Only validating whether current user can make this event 'Occur'
            return;
        }

        Collection<CaseRoleDefinition> authorizedRoles = getAuthorizedRoles();
        if (authorizedRoles.isEmpty()) { // No roles defined, so it is allowed.
            return;
        }

        CurrentMember currentUser = getCaseInstance().getCurrentTeamMember();
        // Now fetch roles of current user within this case and see if there is one that matches one of the authorized roles
        Set<CaseRoleDefinition> rolesOfCurrentUser = currentUser.getRoles();
        for (CaseRoleDefinition role : authorizedRoles) {
            if (rolesOfCurrentUser.contains(role)) {
                return; // You're free to go
            }
        }

        // Apparently no matching role was found.
        throw new AuthorizationException("User '"+currentUser.userId()+"' does not have the permission to raise the event " + getName());
    }

    @Override
    protected void dumpImplementationToXML(Element planItemXML) {
        super.dumpImplementationToXML(planItemXML);
        Collection<CaseRoleDefinition> roles = getAuthorizedRoles();
        for (CaseRoleDefinition role : roles) {
            String roleName = role.getName();
            Element roleElement = planItemXML.getOwnerDocument().createElement("Role");
            planItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }
    }
}
