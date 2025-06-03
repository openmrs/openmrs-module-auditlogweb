package org.openmrs.module.auditlogweb.api.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.openmrs.Allergies;
import org.openmrs.Allergy;
import org.openmrs.BaseCustomizableMetadata;
import org.openmrs.BaseFormRecordableOpenmrsData;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.BaseOpenmrsMetadata;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.BaseReferenceRange;
import org.openmrs.CareSetting;
import org.openmrs.Cohort;
import org.openmrs.CohortMembership;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptAttribute;
import org.openmrs.ConceptAttributeType;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptProposal;
import org.openmrs.ConceptReferenceRange;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptReferenceTermMap;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.ConceptStateConversion;
import org.openmrs.ConceptStopWord;
import org.openmrs.Condition;
import org.openmrs.Diagnosis;
import org.openmrs.DiagnosisAttribute;
import org.openmrs.DiagnosisAttributeType;
import org.openmrs.Drug;
import org.openmrs.DrugIngredient;
import org.openmrs.DrugOrder;
import org.openmrs.DrugReferenceMap;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Field;
import org.openmrs.FieldAnswer;
import org.openmrs.FieldType;
import org.openmrs.Form;
import org.openmrs.FormField;
import org.openmrs.FormResource;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.LocationTag;
import org.openmrs.Obs;
import org.openmrs.ObsReferenceRange;
import org.openmrs.Order;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderFrequency;
import org.openmrs.OrderGroup;
import org.openmrs.OrderGroupAttribute;
import org.openmrs.OrderGroupAttributeType;
import org.openmrs.OrderSetMember;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Program;
import org.openmrs.ProgramAttributeType;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.ProviderAttributeType;
import org.openmrs.ReferralOrder;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.Role;
import org.openmrs.TestOrder;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.VisitType;
import org.openmrs.api.db.ClobDatatypeStorage;
import org.openmrs.api.db.SerializedObject;
import org.openmrs.attribute.BaseAttribute;
import org.openmrs.attribute.BaseAttributeType;
import org.openmrs.hl7.HL7InArchive;
import org.openmrs.hl7.HL7InError;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7QueueItem;
import org.openmrs.hl7.HL7Source;
import org.openmrs.notification.Alert;
import org.openmrs.notification.AlertRecipient;
import org.openmrs.notification.Template;
import org.openmrs.person.PersonMergeLog;
import org.openmrs.scheduler.TaskDefinition;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class UtilClassTest {

    @Test
    public void testAuditedClassesContainsExpected() throws IOException {
        List<String> expectedClassNames = Arrays.asList(
                Allergy.class.getName(), Allergy.class.getName(), BaseCustomizableMetadata.class.getName(), BaseFormRecordableOpenmrsData.class.getName(),
                BaseOpenmrsData.class.getName(), BaseOpenmrsMetadata.class.getName(), BaseOpenmrsObject.class.getName(), BaseReferenceRange.class.getName(),
                CareSetting.class.getName(), Cohort.class.getName(), CohortMembership.class.getName(), Concept.class.getName(), ConceptAnswer.class.getName(),
                ConceptAttribute.class.getName(), ConceptAttribute.class.getName(), ConceptAttributeType.class.getName(), ConceptClass.class.getName(),
                ConceptComplex.class.getName(), ConceptDatatype.class.getName(), ConceptDescription.class.getName(), ConceptMap.class.getName(), ConceptMapType.class.getName(),
                ConceptName.class.getName(), ConceptNameTag.class.getName(), ConceptNumeric.class.getName(), ConceptProposal.class.getName(), ConceptReferenceRange.class.getName(),
                ConceptReferenceTerm.class.getName(), ConceptReferenceTermMap.class.getName(), ConceptSet.class.getName(), ConceptSource.class.getName(), ConceptStateConversion.class.getName(),
                ConceptStopWord.class.getName(), Condition.class.getName(), Diagnosis.class.getName(), DiagnosisAttribute.class.getName(), DiagnosisAttributeType.class.getName(),
                Drug.class.getName(), DrugIngredient.class.getName(), DrugOrder.class.getName(), DrugReferenceMap.class.getName(), Encounter.class.getName(), EncounterType.class.getName(),
                EncounterProvider.class.getName(), EncounterRole.class.getName(), Field.class.getName(), FieldType.class.getName(), FieldAnswer.class.getName(), Form.class.getName(),
                FormField.class.getName(), FormResource.class.getName(), GlobalProperty.class.getName(), Location.class.getName(), LocationAttribute.class.getName(), LocationTag.class.getName(),
                LocationAttributeType.class.getName(), Obs.class.getName(), ObsReferenceRange.class.getName(), Order.class.getName(), OrderAttribute.class.getName(), OrderAttributeType.class.getName(),
                OrderFrequency.class.getName(), OrderGroup.class.getName(), OrderGroupAttribute.class.getName(), OrderGroupAttributeType.class.getName(), OrderSetMember.class.getName(),
                OrderType.class.getName(), Patient.class.getName(), PatientIdentifier.class.getName(), PatientIdentifierType.class.getName(), PatientState.class.getName(), Person.class.getName(),
                PersonAddress.class.getName(), PersonAttribute.class.getName(), PersonName.class.getName(), Privilege.class.getName(), Program.class.getName(), ProgramAttributeType.class.getName(),
                ProgramWorkflow.class.getName(), ProgramWorkflowState.class.getName(), Provider.class.getName(), ProviderAttribute.class.getName(), ProviderAttributeType.class.getName(),
                ReferralOrder.class.getName(), Relationship.class.getName(), RelationshipType.class.getName(), Role.class.getName(), TestOrder.class.getName(), User.class.getName(), Visit.class.getName(),
                VisitAttribute.class.getName(), VisitAttributeType.class.getName(), VisitAttributeType.class.getName(), VisitType.class.getName(), ClobDatatypeStorage.class.getName(),
                SerializedObject.class.getName(), BaseAttribute.class.getName(), BaseAttributeType.class.getName(), HL7InArchive.class.getName(), HL7InQueue.class.getName(), HL7QueueItem.class.getName(),
                HL7Source.class.getName(), HL7InError.class.getName(), Alert.class.getName(), AlertRecipient.class.getName(), Template.class.getName(), PersonMergeLog.class.getName(),
                TaskDefinition.class.getName()
        );

        List<String> actualAuditedClasses = UtilClass.findClassesWithAuditedAnnotation();

        // Assert that all found audited classes are within the correct package
        for (String clazz : actualAuditedClasses) {
            assertTrue(clazz.startsWith("org.openmrs"), "Audited class outside expected package: " + clazz);
        }

        // Assert that expected audited classes are among those found
        for (String expected : expectedClassNames) {
            assertTrue(actualAuditedClasses.contains(expected), "Expected audited class not found: " + expected);
        }
    }
}