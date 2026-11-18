package edu.bits.itsm.domain;

// What kind of note this is. A request for information is a normal note with a special
// type, not a new lifecycle state, so the incident keeps moving through its normal states.
public enum NoteType {
    NOTE, INFO_REQUEST, INFO_RESPONSE
}
