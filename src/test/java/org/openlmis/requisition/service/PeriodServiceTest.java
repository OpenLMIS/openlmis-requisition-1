package org.openlmis.requisition.service;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.domain.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.RequisitionStatus.INITIATED;
import static org.openlmis.requisition.domain.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.domain.RequisitionStatus.SKIPPED;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProcessingScheduleDto;
import org.openlmis.requisition.exception.InvalidPeriodException;
import org.openlmis.requisition.exception.InvalidRequisitionStatusException;
import org.openlmis.requisition.exception.RequisitionException;
import org.openlmis.requisition.exception.RequisitionInitializationException;
import org.openlmis.requisition.exception.RequisitionTemplateColumnException;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.service.referencedata.PeriodReferenceDataService;
import org.openlmis.requisition.service.referencedata.ScheduleReferenceDataService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class PeriodServiceTest {

  @Mock
  private Requisition initiatedRequsition;

  @Mock
  private Requisition submittedRequsition;

  @Mock
  private Requisition authorizedRequsition;

  @Mock
  private Requisition approvedRequsition;

  @Mock
  private PeriodReferenceDataService periodReferenceDataService;

  @Mock
  private RequisitionRepository requisitionRepository;

  @Mock
  private ScheduleReferenceDataService scheduleReferenceDataService;

  @InjectMocks
  private PeriodService periodService;

  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();

  private ProcessingPeriodDto period1;
  private ProcessingPeriodDto period2;
  private ProcessingPeriodDto period3;
  private ProcessingPeriodDto period4;
  private ProcessingPeriodDto period5;

  @Before
  public void setUp() throws Exception {
    period1 = createPeriod(0);
    period2 = createPeriod(1);
    period3 = createPeriod(2);
    period4 = createPeriod(3);
    period5 = createPeriod(4);

    doReturn(INITIATED).when(initiatedRequsition).getStatus();
    doReturn(SUBMITTED).when(submittedRequsition).getStatus();
    doReturn(AUTHORIZED).when(authorizedRequsition).getStatus();
    doReturn(APPROVED).when(approvedRequsition).getStatus();

    doReturn(Collections.emptyList())
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period1.getId(), false);
    doReturn(Collections.singletonList(initiatedRequsition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period2.getId(), false);
    doReturn(Collections.singletonList(submittedRequsition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period3.getId(), false);
    doReturn(Collections.singletonList(authorizedRequsition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period4.getId(), false);
    doReturn(Collections.singletonList(approvedRequsition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period5.getId(), false);
  }

  @Test
  public void shouldReturnCurrentPeriod() throws Exception {
    Requisition requisition = new Requisition();
    requisition.setStatus(SUBMITTED);

    doReturn(Collections.singletonList(period1))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(Collections.singletonList(requisition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period1.getId(), false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(1));
    assertThat(currentPeriods.iterator().next().getId(), is(equalTo(period1.getId())));
  }

  @Test
  public void shouldReturnCurrentPeriodsIfThereIsMoreThanOne() throws Exception {
    Requisition requisition = new Requisition();
    requisition.setStatus(SUBMITTED);

    ProcessingPeriodDto period6 = new ProcessingPeriodDto();
    period6.setId(UUID.randomUUID());
    period6.setStartDate(period1.getStartDate());
    period6.setEndDate(period1.getEndDate());

    doReturn(Arrays.asList(period1, period6))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(Collections.singletonList(requisition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period1.getId(), false);
    doReturn(Collections.singletonList(requisition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period6.getId(), false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    List<UUID> ids = currentPeriods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertThat(ids, hasSize(2));
    assertThat(ids, contains(period1.getId(), period6.getId()));
  }

  @Test
  public void shouldNotReturnCurrentPeriodIfThereIsNoRequisition() throws Exception {
    doReturn(Collections.singletonList(period1))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(Collections.emptyList())
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period1.getId(), false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(0));
  }

  @Test
  public void shouldNotReturnCurrentPeriodIfItDoesNotExist() throws Exception {
    doReturn(Collections.singletonList(period2))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(0));
  }

  @Test
  public void shouldNotReturnCurrentPeriodIfThereIsNonSubmittedRequisition() throws Exception {
    Requisition requisition = new Requisition();
    requisition.setStatus(INITIATED);

    doReturn(Collections.singletonList(period1))
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    doReturn(Collections.singletonList(requisition))
        .when(requisitionRepository)
        .searchByProcessingPeriodAndType(period1.getId(), false);

    List<ProcessingPeriodDto> currentPeriods =
        periodService.getCurrentPeriods(programId, facilityId);

    assertThat(currentPeriods, hasSize(0));
  }

  @Test
  public void shouldReturnOnlyValidPeriodsForRequisitionInitiate() {
    List<ProcessingPeriodDto> list = Lists.newArrayList(
        period1, period2, period3, period4, period5
    );

    doReturn(list)
        .when(periodReferenceDataService)
        .searchByProgramAndFacility(programId, facilityId);

    Collection<ProcessingPeriodDto> periods =
        periodService.getPeriods(programId, facilityId, false);

    verify(requisitionRepository, times(5)).searchByProcessingPeriodAndType(any(UUID.class), any());

    assertNotNull(periods);
    assertEquals(3, periods.size());

    List<UUID> periodUuids = periods
        .stream()
        .map(ProcessingPeriodDto::getId)
        .collect(Collectors.toList());

    assertTrue(periodUuids.contains(period1.getId()));
    assertTrue(periodUuids.contains(period2.getId()));
    assertTrue(periodUuids.contains(period3.getId()));
  }

  @Test
  public void shouldReturnPreviousPeriod() throws Exception {
    doReturn(period5)
        .when(periodReferenceDataService)
        .findOne(period5.getId());
    doReturn(Arrays.asList(period1, period2, period3, period4, period5))
        .when(periodReferenceDataService)
        .search(any(), any());

    ProcessingPeriodDto previous = periodService.findPreviousPeriod(period5.getId());

    assertThat(previous.getId(), is(equalTo(period4.getId())));
  }

  private ProcessingPeriodDto createPeriod(int plusMonth) {
    ProcessingPeriodDto dto = new ProcessingPeriodDto();
    dto.setId(UUID.randomUUID());
    dto.setProcessingSchedule(mock(ProcessingScheduleDto.class));
    dto.setStartDate(LocalDate.now().plusMonths(plusMonth).with(firstDayOfMonth()));
    dto.setEndDate(LocalDate.now().plusMonths(plusMonth).with(lastDayOfMonth()));

    return dto;
  }

  @Test(expected = RequisitionInitializationException.class)
  public void shouldThrowExceptionIfScheduleDoesNotExist()
      throws RequisitionException, RequisitionTemplateColumnException {
    when(periodReferenceDataService.searchByProgramAndFacility(programId,facilityId))
        .thenReturn(Lists.newArrayList(period1));
    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(null);

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test(expected = InvalidPeriodException.class)
  public void shouldThrowExceptionIfPeriodIsNotTheOldest()
      throws RequisitionException, RequisitionTemplateColumnException {
    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());

    period1.setProcessingSchedule(processingScheduleDto);
    period1.setId(UUID.randomUUID());

    when(periodReferenceDataService.searchByProgramAndFacility(programId,facilityId))
        .thenReturn(Lists.newArrayList(period1));

    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(Collections.singletonList(processingScheduleDto));

    periodService.findPeriod(programId, facilityId, UUID.randomUUID(), false);
  }

  @Test(expected = InvalidPeriodException.class)
  public void shouldThrowExceptionWhenInitiatingReqPeriodDoesNotBelongToTheSameScheduleAsProgram()
      throws RequisitionException, RequisitionTemplateColumnException {

    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    processingScheduleDto.setId(UUID.randomUUID());
    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
        .thenReturn(Collections.singletonList(processingScheduleDto));

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test(expected = InvalidRequisitionStatusException.class)
  public void shouldThrowExceptionWhenPreviousReqHasInitiatedStatus()
          throws RequisitionException, RequisitionTemplateColumnException {

    Requisition requisition = getRequsition(INITIATED);
    when(requisitionRepository.getLastRegularRequisition(facilityId, programId))
            .thenReturn(requisition);

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test(expected = InvalidRequisitionStatusException.class)
  public void shouldThrowExceptionWhenPreviousReqHasSubmittedStatus()
          throws RequisitionException, RequisitionTemplateColumnException {

    Requisition requisition = getRequsition(SUBMITTED);
    when(requisitionRepository.getLastRegularRequisition(facilityId, programId))
            .thenReturn(requisition);

    periodService.findPeriod(programId, facilityId, null, false);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasAuthorizedStatus()
          throws RequisitionException, RequisitionTemplateColumnException {

    Requisition requisition = getRequsition(AUTHORIZED);
    setMockForFindPeriod(requisition);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasApprovedStatus()
          throws RequisitionException, RequisitionTemplateColumnException {

    Requisition requisition = getRequsition(APPROVED);
    setMockForFindPeriod(requisition);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  @Test
  public void shouldSucceedWhenPreviousReqHasSkippedStatus()
          throws RequisitionException, RequisitionTemplateColumnException {

    Requisition requisition = getRequsition(SKIPPED);
    setMockForFindPeriod(requisition);

    ProcessingPeriodDto period = periodService.findPeriod(programId, facilityId, null, false);
    assertEquals(period1, period);
  }

  private Requisition getRequsition(RequisitionStatus status) {
    Requisition requisition = new Requisition();
    requisition.setStatus(status);
    return requisition;
  }

  private void setMockForFindPeriod(Requisition requisition) {
    when(requisitionRepository.getLastRegularRequisition(facilityId, programId))
            .thenReturn(requisition);
    when(periodReferenceDataService.searchByProgramAndFacility(programId, facilityId))
            .thenReturn(Collections.singletonList(period1));

    ProcessingScheduleDto processingScheduleDto = new ProcessingScheduleDto();
    UUID id = UUID.randomUUID();
    processingScheduleDto.setId(id);
    when(scheduleReferenceDataService.searchByProgramAndFacility(programId, facilityId))
            .thenReturn(Collections.singletonList(processingScheduleDto));
    when(period1.getProcessingSchedule().getId()).thenReturn(id);
  }

}