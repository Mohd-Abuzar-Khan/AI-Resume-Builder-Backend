package com.resumade.auth.dto;

public class AuthResponse {
    private String token;
    private Integer userId;
    private String fullName;
    private String email;
    private String role;
    private String plan;
    private Integer credits;
    private String profilePicture;


    public AuthResponse() {}

    public AuthResponse(String token, Integer userId, String fullName, String email, String role, String plan, Integer credits, String profilePicture) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.plan = plan;
        this.credits = credits;
        this.profilePicture = profilePicture;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}
