package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.json.ValueMap;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberRemoved<Member extends CaseTeamMember> extends CaseTeamMemberEvent<Member> {

    protected CaseTeamMemberRemoved(Team team, Member member) {
        super(team, member);
    }

    protected CaseTeamMemberRemoved(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json, reader);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
