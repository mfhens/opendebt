package dk.ufst.opendebt.payment.bookkeeping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dk.ufst.bookkeeping.engine.BookkeepingEngine;
import dk.ufst.bookkeeping.engine.BookkeepingEngineImpl;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.InterestAccrualService;
import dk.ufst.bookkeeping.service.RetroactiveCorrectionService;
import dk.ufst.bookkeeping.service.TimelineReplayService;
import dk.ufst.bookkeeping.service.impl.InterestAccrualServiceImpl;
import dk.ufst.bookkeeping.service.impl.RetroactiveCorrectionServiceImpl;
import dk.ufst.bookkeeping.service.impl.TimelineReplayServiceImpl;
import dk.ufst.bookkeeping.spi.Kontoplan;

@Configuration
public class BookkeepingConfiguration {

  @Bean
  InterestAccrualService interestAccrualService(FinancialEventStore financialEventStore) {
    return new InterestAccrualServiceImpl(financialEventStore);
  }

  @Bean
  RetroactiveCorrectionService retroactiveCorrectionService(
      LedgerEntryStore ledgerEntryStore,
      FinancialEventStore financialEventStore,
      InterestAccrualService interestAccrualService,
      Kontoplan kontoplan) {
    return new RetroactiveCorrectionServiceImpl(
        ledgerEntryStore, financialEventStore, interestAccrualService, kontoplan);
  }

  @Bean
  TimelineReplayService timelineReplayService(
      FinancialEventStore financialEventStore,
      LedgerEntryStore ledgerEntryStore,
      CoveragePriorityPort coveragePriorityPort,
      Kontoplan kontoplan) {
    return new TimelineReplayServiceImpl(
        financialEventStore, ledgerEntryStore, coveragePriorityPort, kontoplan);
  }

  @Bean
  BookkeepingEngine bookkeepingEngine(
      LedgerEntryStore ledgerEntryStore, FinancialEventStore financialEventStore) {
    return new BookkeepingEngineImpl(ledgerEntryStore, financialEventStore);
  }
}
