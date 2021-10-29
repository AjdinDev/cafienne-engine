/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.DeserializationError;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Base class for parsing XML elements defined in the CMMN specification.
 */
public abstract class CMMNElementDefinition extends XMLElementDefinition {
    private final static Logger logger = LoggerFactory.getLogger(CafienneSerializer.class);
    public final CMMNDocumentationDefinition documentation;

    protected CMMNElementDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement, boolean... identifierRequired) {
        super(element, modelDefinition, parentElement, identifierRequired);
        this.documentation = parseDocumentation();
        if (modelDefinition != null) {
            modelDefinition.addCMMNElement(this);
        }
    }

    /**
     * If documentation is not present, we'll create dummy place holder to avoid nullpointerexceptions when reading the documentation.
     * Note that the dummy placeholder also converts a potential CMMN1.0 "description" attribute if that is still present
     *
     * @return
     */
    private CMMNDocumentationDefinition parseDocumentation() {
        CMMNDocumentationDefinition documentation = parse("documentation", CMMNDocumentationDefinition.class, false);
        if (documentation == null) {
            documentation = new CMMNDocumentationDefinition(this.getModelDefinition(), this);
        }

        return documentation;
    }

    /**
     * Returns the documentation object of the element
     *
     * @return
     */
    public CMMNDocumentationDefinition getDocumentation() {
        return this.documentation;
    }

    /**
     * Returns a description of the context this element provides to it's children. Can be used e.g. in expressions or on parts
     * to get the description of the parent element when encountering validation errors.
     *
     * @return
     */
    public String getContextDescription() {
        return "";
    }

    public String toString() {
        if (getName().isEmpty()) {
            return getClass().getSimpleName();
        } else {
            return getName();
        }
    }

    public CaseDefinition getCaseDefinition() {
        return (CaseDefinition) getModelDefinition();
    }

    public ProcessDefinition getProcessDefinition() {
        return (ProcessDefinition) getModelDefinition();
    }

    protected StageDefinition getSurroundingStage() {
        CMMNElementDefinition ancestor = this.getParentElement();
        while (ancestor != null && !(ancestor instanceof StageDefinition)) {
            ancestor = ancestor.getParentElement();
        }
        return (StageDefinition) ancestor;
    }

    public static <T extends CMMNElementDefinition> T fromJSON(String sourceClassName, ValueMap json, Class<T> tClass) {
        String guid = json.readString(Fields.elementId);
        String source = json.readString(Fields.source);
        try {
            DefinitionsDocument def = new DefinitionsDocument(XMLHelper.loadXML(source));
            T element = def.getElement(guid, tClass);
            return element;
        } catch (InvalidDefinitionException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Encountered invalid definition during deserialization; probably content from a newer or older version", e);
            } else {
                logger.warn("Encountered invalid definition during deserialization; probably content from a newer or older version.\nEnable debug logging for full stacktrace. Error messages: " + e.getErrors());
            }
            throw new DeserializationError("Invalid Definition Failure while deserializing an instance of " + sourceClassName, e);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            // TTD we need to come up with a more suitable exception, since this logic is typically also
            //  invoked when recovering from events.
            throw new DeserializationError("Parsing Failure while deserializing an instance of " + sourceClassName, e);
        }
    }

    public ValueMap toJSON() {
        String identifier = this.getId();
        if (identifier == null || identifier.isEmpty()) {
            identifier = this.getName();
        }
        String source = getModelDefinition().getDefinitionsDocument().getSource();
        ValueMap json = new ValueMap(Fields.elementId, identifier, Fields.source, source);
        return json;
    }
}
