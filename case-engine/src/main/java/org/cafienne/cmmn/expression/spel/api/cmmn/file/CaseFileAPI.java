package org.cafienne.cmmn.expression.spel.api.cmmn.file;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFile;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CaseFileAPI extends APIObject<Case> {
    private final CaseFile file;
    private final Map<String, CaseFileItemAPI> items = new HashMap();

    public CaseFileAPI(CaseFile file) {
        super(file.getCaseInstance());
        this.file = file;
        this.file.getCaseFileItems().forEach((definition, item) -> {
            // Enable directly accessing the JSON structure of the CaseFileItem by name
            addPropertyReader(definition.getName(), () -> new ValueAPI(item));
            // And enable CaseFileItem wrapper to be accessed by getItem() method
            CaseFileItemAPI itemAPI = new CaseFileItemAPI(item);
            items.put(definition.getName(), itemAPI);
        });
    }

    public CaseFileItemAPI getItem(String name) {
        return items.get(name);
    }
}