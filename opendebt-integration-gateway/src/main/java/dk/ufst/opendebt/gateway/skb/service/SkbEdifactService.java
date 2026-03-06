package dk.ufst.opendebt.gateway.skb.service;

import java.io.InputStream;
import java.util.List;

import dk.ufst.opendebt.gateway.skb.model.CreditAdvice;
import dk.ufst.opendebt.gateway.skb.model.DebitAdvice;
import dk.ufst.opendebt.gateway.skb.model.SkbMessage;

/** Service for parsing and generating CREMUL/DEBMUL EDIFACT messages for SKB integration. */
public interface SkbEdifactService {

  /** Parses a CREMUL EDIFACT message and returns structured credit advices. */
  SkbMessage parseCremul(InputStream cremulStream);

  /** Generates a DEBMUL EDIFACT message from debit advices. */
  byte[] generateDebmul(List<DebitAdvice> debitAdvices);
}
