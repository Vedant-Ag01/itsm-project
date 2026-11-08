package edu.bits.itsm.domain;

// The employee's feedback after an incident is resolved.
public class Feedback {

    private final int rating;
    private final String comment;

    public Feedback(int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new InvalidIncidentException("Feedback rating must be between 1 and 5");
        }
        this.rating = rating;
        this.comment = (comment == null) ? "" : comment;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}