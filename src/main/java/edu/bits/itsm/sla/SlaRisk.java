package edu.bits.itsm.sla;


public record SlaRisk(RiskLevel level, int score, String reason) {
}
