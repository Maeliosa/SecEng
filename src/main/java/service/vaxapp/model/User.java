package service.vaxapp.model;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "PPS number cannot be null")
    @Size(min = 4, message = "PPS number must be 8 characters long")
    @Column(name = "user_pps", unique = true)
    private String PPS;

    @NotNull(message = "Full name cannot be null")
    @Size(min = 2, max = 50, message = "Full name must be between 2 and 50 characters")
    @Column(name = "full_name")
    private String fullName;

    @Column
    private String address;
    @Column(name = "phone_number")
    private String phoneNumber;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    @Column
    private String email;

    @Column(name = "date_of_birth")
    private String dateOfBirth;
    @Column
    private String nationality;
    @Column
    private String gender;
    @Column(nullable = false)
    @Type(type = "org.hibernate.type.NumericBooleanType")
    private Boolean admin = false;
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;
    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(nullable = false)
    private String role;



    // Bidirectional one-to-many relationship (One user may get multiple vaccines)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<Vaccine> vaccines;

    // Bidirectional one-to-many relationship (One user may ask multiple forum
    // questions)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<ForumQuestion> questions;

    // Bidirectional one-to-many relationship (One user may be assigned multiple
    // appointments)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    private List<Appointment> appointments;

    public User() {
    }

    public User(String PPS, String fullName, String address, String phoneNumber, String email, String dateOfBirth,
            String nationality, String gender, Boolean admin) {
        this.PPS = PPS;
        this.fullName = fullName;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.gender = gender;
        this.admin = admin;
        this.role = admin ? "ADMIN" : "USER";  // Automatically set the role based on admin status
    }

    public Integer getId() {
        return id;
    }

    public String getPPS() {
        return PPS;
    }

    public void setPPS(String PPS) {
        this.PPS = PPS;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getAge() {
        LocalDate birthday = LocalDate.parse(this.dateOfBirth, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public Boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
