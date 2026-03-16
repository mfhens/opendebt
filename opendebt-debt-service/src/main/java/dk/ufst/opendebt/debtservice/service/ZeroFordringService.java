package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.debtservice.entity.DebtEntity;

/**
 * Validates 0-fordring (zero-principal claim) patterns for renter on indfriet hovedfordring.
 *
 * <p>A 0-fordring is a hovedfordring (HF) with principalAmount == 0 that serves as a reference
 * anchor for underfordringer (UF) such as renter (interest claims) after the original hovedfordring
 * has been fully paid (indfriet).
 */
public interface ZeroFordringService {

  /**
   * Validate that a 0-saldo fordring has all required stamdata.
   *
   * @param entity the debt entity to validate
   */
  void validateZeroFordring(DebtEntity entity);

  /**
   * Validate that a UF (underfordring/rente) correctly references an existing hovedfordring via
   * hovedfordringsId.
   *
   * @param underfordring the underfordring entity to validate
   */
  void validateUnderfordringReference(DebtEntity underfordring);
}
