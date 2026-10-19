package edu.bits.itsm.domain;

public class Employee extends User {

    private String department;

    public Employee(String id, String name, String email, String department) {
        super(id, name, email);
        this.department = department;
    }

    @Override
    public String getRoleName() {
        return "EMPLOYEE";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}