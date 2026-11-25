package com.cookify.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class RegistrationForm {
    @NotBlank private String username;
    @NotBlank @Email private String email;
    @NotBlank private String phone;
    @NotBlank private String password;
    @NotBlank private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String v) { this.confirmPassword = v; }
}
