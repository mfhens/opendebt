package dk.ufst.opendebt.common.soap;

import java.time.Instant;

public record Oces3AuthContext(
    String fordringshaverId, String cn, String issuer, Instant validTo, String serialNumber) {}
