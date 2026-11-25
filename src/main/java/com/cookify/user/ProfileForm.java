package com.cookify.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    private String nameFirst;
    private String nameLast;

    @Min(value = 1, message = "Age must be between 1 and 120")
    @Max(value = 120, message = "Age must be between 1 and 120")
    private Integer age;

    private String gender;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    private boolean twoFactorEnabled;

    public String getNameFirst() { return nameFirst; }
    public void setNameFirst(String v) { this.nameFirst = v; }
    public String getNameLast() { return nameLast; }
    public void setNameLast(String v) { this.nameLast = v; }
    public Integer getAge() { return age; }
    public void setAge(Integer v) { this.age = v; }
    public String getGender() { return gender; }
    public void setGender(String v) { this.gender = v; }
    public String getBio() { return bio; }
    public void setBio(String v) { this.bio = v; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean v) { this.twoFactorEnabled = v; }
}
