package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a role is added to a case team member
 */
@Manifest
public class TeamRoleFilled extends CaseTeamRoleEvent {
    public TeamRoleFilled(Case caseInstance, MemberKey member, String roleName) {
        super(caseInstance, member, roleName);
    }

    public TeamRoleFilled(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        if (isMemberItself()) {
            // The event that adds the member to the team
            return getClass().getSimpleName() + "[" + getMemberDescription() + " is added to the case team]";
        } else {
            return getClass().getSimpleName() + "[" + getMemberDescription() + " now has role " + roleName() + " in the case team]";
        }
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}