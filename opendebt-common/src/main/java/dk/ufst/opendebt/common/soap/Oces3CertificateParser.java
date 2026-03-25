package dk.ufst.opendebt.common.soap;

import java.security.cert.X509Certificate;
import java.time.Instant;

import javax.security.auth.x500.X500Principal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Oces3CertificateParser {

  @Value("${opendebt.soap.oces3.fordringshaver-dn-field:CN}")
  private String fordringsshaverDnField;

  public Oces3AuthContext parse(X509Certificate certificate) {
    X500Principal subject = certificate.getSubjectX500Principal();
    String dn = subject.getName(X500Principal.RFC2253);
    String fordringshaverId = extractDnField(dn, fordringsshaverDnField);
    String cn = extractDnField(dn, "CN");
    String issuer = certificate.getIssuerX500Principal().getName(X500Principal.RFC2253);
    Instant validTo = certificate.getNotAfter().toInstant();
    String serialNumber = certificate.getSerialNumber().toString(16);
    return new Oces3AuthContext(fordringshaverId, cn, issuer, validTo, serialNumber);
  }

  private String extractDnField(String dn, String field) {
    String[] parts = dn.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    String prefix = field + "=";
    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.startsWith(prefix)) {
        return trimmed.substring(prefix.length()).trim();
      }
    }
    return "";
  }
}
