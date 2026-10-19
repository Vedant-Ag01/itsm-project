package edu.bits.itsm.domain;

public class Administrator extends User {

    public Administrator(String id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public String getRoleName() {
        return "ADMINISTRATOR";
    }
}