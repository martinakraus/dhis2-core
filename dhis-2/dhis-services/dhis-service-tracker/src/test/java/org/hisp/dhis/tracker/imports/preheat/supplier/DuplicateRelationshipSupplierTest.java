/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.export.relationship.RelationshipStore;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateRelationshipSupplierTest extends TestBase {

  private static final String REL_A_UID = "RELA";

  private static final String REL_B_UID = "RELB";

  private static final String REL_C_UID = "RELC";

  private static final String TE_A_UID = "TE_A";

  private static final String TE_B_UID = "TE_B";

  private static final String TE_C_UID = "TE_C";

  private static final String KEY_REL_A = "UNIRELTYPE_TE_A_TE_B";

  private static final String KEY_REL_B = "BIRELTYPE_TE_B_TE_C";

  private static final String KEY_REL_C = "UNIRELTYPE_TE_C_TE_A";

  private static final String UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID = "UNIRELTYPE";

  private static final String BIDIRECTIONAL_RELATIONSHIP_TYPE_UID = "BIRELTYPE";

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipA;

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipB;

  private org.hisp.dhis.tracker.imports.domain.Relationship relationshipC;

  private RelationshipType unidirectionalRelationshipType;

  private RelationshipType bidirectionalRelationshipType;

  private TrackedEntity teA, teB, teC;

  private TrackerPreheat preheat;

  @Mock private RelationshipStore relationshipStore;

  @InjectMocks private DuplicateRelationshipSupplier supplier;

  @BeforeEach
  public void setUp() {
    unidirectionalRelationshipType = createRelationshipType('A');
    unidirectionalRelationshipType.setUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID);
    unidirectionalRelationshipType.setBidirectional(false);

    bidirectionalRelationshipType = createRelationshipType('B');
    bidirectionalRelationshipType.setUid(BIDIRECTIONAL_RELATIONSHIP_TYPE_UID);
    bidirectionalRelationshipType.setBidirectional(true);

    OrganisationUnit organisationUnit = createOrganisationUnit('A');

    teA = createTrackedEntity(organisationUnit);
    teA.setUid(TE_A_UID);
    teB = createTrackedEntity(organisationUnit);
    teB.setUid(TE_B_UID);
    teC = createTrackedEntity(organisationUnit);
    teC.setUid(TE_C_UID);

    relationshipA =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_A_UID)
            .relationshipType(MetadataIdentifier.ofUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_A_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_B_UID).build())
            .build();

    relationshipB =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_B_UID)
            .relationshipType(MetadataIdentifier.ofUid(BIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_B_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_C_UID).build())
            .build();

    relationshipC =
        org.hisp.dhis.tracker.imports.domain.Relationship.builder()
            .relationship(REL_C_UID)
            .relationshipType(MetadataIdentifier.ofUid(UNIDIRECTIONAL_RELATIONSHIP_TYPE_UID))
            .from(RelationshipItem.builder().trackedEntity(TE_C_UID).build())
            .to(RelationshipItem.builder().trackedEntity(TE_A_UID).build())
            .build();

    preheat = new TrackerPreheat();
    preheat.put(TrackerIdSchemeParam.UID, unidirectionalRelationshipType);
    preheat.put(TrackerIdSchemeParam.UID, bidirectionalRelationshipType);
  }

  @Test
  void verifySupplier() {
    when(relationshipStore.getUidsByRelationshipKeys(List.of(KEY_REL_A, KEY_REL_B, KEY_REL_C)))
        .thenReturn(List.of(relationshipA(), relationshipB()));

    TrackerObjects trackerObjects =
        TrackerObjects.builder()
            .relationships(List.of(relationshipA, relationshipB, relationshipC))
            .build();

    supplier.preheatAdd(trackerObjects, preheat);

    assertTrue(preheat.isDuplicate(relationshipA));
    assertFalse(preheat.isDuplicate(invertTeToTeRelationship(relationshipA)));
    assertTrue(preheat.isDuplicate(relationshipB));
    assertTrue(preheat.isDuplicate(invertTeToTeRelationship(relationshipB)));
    assertFalse(preheat.isDuplicate(relationshipC));
  }

  private Relationship relationshipA() {
    return createTeToTeRelationship(teA, teB, unidirectionalRelationshipType);
  }

  private Relationship relationshipB() {
    return createTeToTeRelationship(teB, teC, bidirectionalRelationshipType);
  }

  private org.hisp.dhis.tracker.imports.domain.Relationship invertTeToTeRelationship(
      org.hisp.dhis.tracker.imports.domain.Relationship relationship) {
    return org.hisp.dhis.tracker.imports.domain.Relationship.builder()
        .relationship(relationship.getRelationship())
        .relationshipType(relationship.getRelationshipType())
        .from(
            RelationshipItem.builder()
                .trackedEntity(relationship.getTo().getTrackedEntity())
                .build())
        .to(
            RelationshipItem.builder()
                .trackedEntity(relationship.getFrom().getTrackedEntity())
                .build())
        .build();
  }
}
