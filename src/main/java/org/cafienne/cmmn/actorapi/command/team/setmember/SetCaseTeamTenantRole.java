/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.cmmn.actorapi.command.team.setmember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to add a tenant role to the case team
 */
@Manifest
public class SetCaseTeamTenantRole extends SetCaseTeamMemberCommand<CaseTeamTenantRole> {
    public SetCaseTeamTenantRole(CaseUserIdentity user, String caseInstanceId, CaseTeamTenantRole newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamTenantRole(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }

    @Override
    protected void process(Team team) {
        team.setTenantRole(newMember);
    }
}
