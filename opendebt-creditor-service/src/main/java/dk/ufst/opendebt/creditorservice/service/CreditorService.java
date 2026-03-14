package dk.ufst.opendebt.creditorservice.service;

import java.util.*;

import dk.ufst.opendebt.creditorservice.dto.*;

public interface CreditorService {

  CreditorDto getByCreditorOrgId(UUID creditorOrgId);

  CreditorDto getByExternalCreditorId(String externalCreditorId);

  List<CreditorDto> getChildrenByParentId(UUID parentCreditorId);

  ValidateActionResponse validateAction(UUID creditorOrgId, ValidateActionRequest request);

  Optional<CreditorDto> findCreditorByCreditorOrgId(UUID creditorOrgId);
}
