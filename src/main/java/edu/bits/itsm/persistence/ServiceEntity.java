package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.BusinessCriticality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "it_services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    @Column(length = 500)
    public String description;

    @Enumerated(EnumType.STRING)
    public BusinessCriticality criticality;

    public boolean active = true;

    public ServiceEntity() {
    }
}