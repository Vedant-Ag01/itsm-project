package edu.bits.itsm.domain;

public enum BusinessCriticality {
    LOW(1), MEDIUM(2), HIGH(3);

    private final int weight;

    BusinessCriticality(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}