package com.medid.entity;

import com.medid.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // This will store the BCRYPT HASH

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "doctor_ref_code", unique = true)
    private String doctorRefCode; // Human-facing doctor code e.g. MEDID-12

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "weekday_shift")
    private String weekdayShift; // MORNING | EVENING | NIGHT

    @Column(name = "weekend_shift")
    private String weekendShift; // MORNING | EVENING | NIGHT

    @Column(name = "enabled")
    private Boolean enabled = true;

    @PrePersist
    public void applyDefaultRole() {
        if (this.role == null) {
            this.role = Role.RECEPTIONIST;
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }

    @PreUpdate
    public void applyDefaultEnabledOnUpdate() {
        if (this.enabled == null) {
            this.enabled = true;
        }
    }

    // Spring Security Methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled == null || enabled; }
}
