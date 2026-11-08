package edu.bits.itsm.domain;

public enum Impact {
    LOW(1), MEDIUM(2), HIGH(3);

    private final int weight;

    Impact(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}