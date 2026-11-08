package edu.bits.itsm.domain;

public enum Urgency {
    LOW(1), MEDIUM(2), HIGH(3);

    private final int weight;

    Urgency(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}