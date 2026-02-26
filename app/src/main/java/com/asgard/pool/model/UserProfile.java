package com.asgard.pool.model;

import java.io.Serializable;

public class UserProfile implements Serializable {
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_MANAGER = "MANAGER";

    private String role; // STUDENT or MANAGER

    public UserProfile() {}

    public UserProfile(String role) {
        this.role = role;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
