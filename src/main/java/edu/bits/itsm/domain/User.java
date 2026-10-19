package edu.bits.itsm.domain;

public abstract class User {

    private final String id;
    private String name;
    private String email;

    protected User(String id, String name, String email) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("User id cannot be empty");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public abstract String getRoleName();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}