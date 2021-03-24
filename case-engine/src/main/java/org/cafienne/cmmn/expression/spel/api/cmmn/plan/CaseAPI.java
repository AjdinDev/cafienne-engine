package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.expression.spel.api.cmmn.file.CaseFileAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.team.CaseTeamAPI;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.casefile.CaseFile;

import java.util.Collection;

public class CaseAPI extends APIObject<Case> {
    private final StageAPI casePlan;
    private final CaseTeamAPI caseTeam;
    private final CaseFileAPI caseFile;

    public CaseAPI(Case actor) {
        super(actor);
        this.casePlan = new StageAPI(actor.getCasePlan(), null);
        this.caseTeam = new CaseTeamAPI(actor.getCaseTeam());
        this.caseFile = new CaseFileAPI(actor.getCaseFile());
        addPropertyReader("plan", () -> casePlan);
        addPropertyReader("file", () -> caseFile);
        addPropertyReader("team", () -> caseTeam);
        addPropertyReader("id", actor::getId);
        addPropertyReader("name", () -> actor.getDefinition().getName());
        addPropertyReader("tenant", actor::getTenant);
        addPropertyReader("parent", actor::getParentCaseId);
        addPropertyReader("root", actor::getRootCaseId);
        addPropertyReader("createdOn", actor::getCreatedOn);
        addPropertyReader("lastModified", actor::getLastModified);
        addDeprecatedReader("caseFile", "file", () -> caseFile);
    }

    public String getId() {
        warnDeprecation("getId()", "id");
        return actor.getId();
    }

    public Collection<PlanItem> getPlanItems(String identifier) {
        warnDeprecation("getPlanItemByName(\"" + identifier + "\")", "plan");
        return actor.getPlanItems(identifier);
    }

    public PlanItemAPI getPlanItemByName(String identifier) {
        warnDeprecation("getPlanItemByName(\"" + identifier + "\")", "plan");
        return find(actor.getPlanItemByName(identifier));
    }

    public CaseTeamAPI getCaseTeam() {
        warnDeprecation("getCaseTeam()", "team");
        return caseTeam;
    }

    public CaseFileAPI getCaseFile() {
        warnDeprecation("getCaseFile()", "file");
        return caseFile;
    }

    public PlanItemAPI find(PlanItem item) {
        return casePlan.find(item);
    }
}