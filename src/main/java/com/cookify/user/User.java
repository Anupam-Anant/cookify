package com.cookify.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "name_first") private String nameFirst;
    @Column(name = "name_last")  private String nameLast;
    private Integer age;
    @Column(name = "preference_id") private Long preferenceId;
    private String gender;

    @Column(name = "pass", nullable = false)
    private String passwordHash;

    @Column(unique = true) private String phone;
    private String bio;
    @Column(name = "profile_image_path") private String profileImagePath;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "warning_count", nullable = false)
    private int warningCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- getters / setters (generate all) ---
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNameFirst() { return nameFirst; }
    public void setNameFirst(String v) { this.nameFirst = v; }
    public String getNameLast() { return nameLast; }
    public void setNameLast(String v) { this.nameLast = v; }
    public Integer getAge() { return age; }
    public void setAge(Integer v) { this.age = v; }
    public Long getPreferenceId() { return preferenceId; }
    public void setPreferenceId(Long v) { this.preferenceId = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public String getProfileImagePath() { return profileImagePath; }
    public void setProfileImagePath(String v) { this.profileImagePath = v; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean v) { this.twoFactorEnabled = v; }
    public Role getRole() { return role; }
    public void setRole(Role v) { this.role = v; }
    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus v) { this.accountStatus = v; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int v) { this.warningCount = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
