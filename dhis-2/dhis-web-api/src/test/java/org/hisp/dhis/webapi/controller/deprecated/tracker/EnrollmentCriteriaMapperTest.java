/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentCriteriaMapperTest {

  @Mock private UserService userService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramService programService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityService trackedEntityService;

  @InjectMocks private EnrollmentCriteriaMapper mapper;

  private static final String ORG_UNIT1 = "orgUnit1";

  private static final String PROGRAM_UID = "programUid";

  private static final String ENTITY_TYPE = "entityType";

  private static final String TRACKED_ENTITY = "trackedEntity";

  private static final Set<String> ORG_UNITS = Set.of(ORG_UNIT1);

  private Program program;

  private OrganisationUnit organisationUnit;

  private User user;

  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  @BeforeEach
  void setUp() {
    program = new Program();
    program.setUid(PROGRAM_UID);

    organisationUnit = new OrganisationUnit();
    organisationUnit.setUid(ORG_UNIT1);

    user = new User();
    User user = new User();
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);
    //    when(getCurrentUser()).thenReturn(user);

    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(ENTITY_TYPE);

    trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY);
  }

  @Test
  void shouldMapCorrectlyWhenOrgUnitExistsAndUserInScope() throws IllegalQueryException {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    EnrollmentQueryParams params =
        mapper.getFromUrl(
            ORG_UNITS,
            DESCENDANTS,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertNotEmpty(params.getOrganisationUnits());
    assertEquals(ORG_UNIT1, params.getOrganisationUnits().iterator().next().getUid());
    assertEquals(PROGRAM_UID, params.getProgram().getUid());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitDoesNotExist() {
    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                mapper.getFromUrl(
                    ORG_UNITS,
                    DESCENDANTS,
                    null,
                    "lastUpdated",
                    PROGRAM_UID,
                    ProgramStatus.ACTIVE,
                    null,
                    null,
                    "trackedEntityType",
                    "trackedEntity",
                    false,
                    1,
                    1,
                    false,
                    false,
                    false,
                    null));
    assertEquals("Organisation unit does not exist: " + ORG_UNIT1, exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotInScope() {
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);

    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                mapper.getFromUrl(
                    ORG_UNITS,
                    DESCENDANTS,
                    null,
                    "lastUpdated",
                    PROGRAM_UID,
                    ProgramStatus.ACTIVE,
                    null,
                    null,
                    "trackedEntityType",
                    "trackedEntity",
                    false,
                    1,
                    1,
                    false,
                    false,
                    false,
                    null));
    assertEquals(
        "Organisation unit is not part of the search scope: " + ORG_UNIT1, exception.getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeAccessible() {
    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                mapper.getFromUrl(
                    ORG_UNITS,
                    ACCESSIBLE,
                    null,
                    "lastUpdated",
                    PROGRAM_UID,
                    ProgramStatus.ACTIVE,
                    null,
                    null,
                    ENTITY_TYPE,
                    TRACKED_ENTITY,
                    false,
                    1,
                    1,
                    false,
                    false,
                    false,
                    null));

    assertStartsWith("ouMode ACCESSIBLE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCapture() {
    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                mapper.getFromUrl(
                    ORG_UNITS,
                    CAPTURE,
                    null,
                    "lastUpdated",
                    PROGRAM_UID,
                    ProgramStatus.ACTIVE,
                    null,
                    null,
                    ENTITY_TYPE,
                    TRACKED_ENTITY,
                    false,
                    1,
                    1,
                    false,
                    false,
                    false,
                    null));

    assertStartsWith("ouMode CAPTURE cannot be used with orgUnits.", exception.getMessage());
  }

  @Test
  void shouldMapOrgUnitModeWhenOrgUnitSuppliedAndOrgUnitModeSelected() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    EnrollmentQueryParams enrollmentQueryParams =
        mapper.getFromUrl(
            ORG_UNITS,
            SELECTED,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertEquals(SELECTED, enrollmentQueryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeWhenOrgUnitSuppliedAndOrgUnitModeDescendants() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    EnrollmentQueryParams enrollmentQueryParams =
        mapper.getFromUrl(
            ORG_UNITS,
            DESCENDANTS,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertEquals(DESCENDANTS, enrollmentQueryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeWhenOrgUnitSuppliedAndOrgUnitModeChildren() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    EnrollmentQueryParams enrollmentQueryParams =
        mapper.getFromUrl(
            ORG_UNITS,
            CHILDREN,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertEquals(CHILDREN, enrollmentQueryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldMapParamsWhenOrgUnitModeAllAndUserIsSuperuser() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    User superuser = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Sets.newHashSet("ALL"));
    superuser.setUserRoles(Set.of(userRole));

    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(superuser);

    EnrollmentQueryParams enrollmentQueryParams =
        mapper.getFromUrl(
            ORG_UNITS,
            ALL,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertEquals(ALL, enrollmentQueryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldMapParamsWhenOrgUnitModeAllAndUserIsAuthorized() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(trackedEntityTypeService.getTrackedEntityType(ENTITY_TYPE)).thenReturn(trackedEntityType);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY)).thenReturn(trackedEntity);

    User superuser = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Sets.newHashSet("ALL"));
    superuser.setUserRoles(Set.of(userRole));

    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(superuser);

    EnrollmentQueryParams enrollmentQueryParams =
        mapper.getFromUrl(
            ORG_UNITS,
            ALL,
            null,
            "lastUpdated",
            PROGRAM_UID,
            ProgramStatus.ACTIVE,
            null,
            null,
            ENTITY_TYPE,
            TRACKED_ENTITY,
            false,
            1,
            1,
            false,
            false,
            false,
            null);

    assertEquals(ALL, enrollmentQueryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldFailWhenOrgUnitModeAllAndUserNotAuthorized() {
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT1)).thenReturn(organisationUnit);
    when(organisationUnitService.isInUserHierarchy(
            organisationUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    User superuser = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(
        Sets.newHashSet(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    superuser.setUserRoles(Set.of(userRole));

    IllegalQueryException exception =
        Assertions.assertThrows(
            IllegalQueryException.class,
            () ->
                mapper.getFromUrl(
                    ORG_UNITS,
                    ALL,
                    null,
                    "lastUpdated",
                    PROGRAM_UID,
                    ProgramStatus.ACTIVE,
                    null,
                    null,
                    ENTITY_TYPE,
                    TRACKED_ENTITY,
                    false,
                    1,
                    1,
                    false,
                    false,
                    false,
                    null));

    Assertions.assertEquals(
        "Current user is not authorized to query across all organisation units",
        exception.getMessage());
  }
}
