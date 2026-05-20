package hr.tvz.experimate.experimate.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDate;

@MappedSuperclass
public abstract class Person {

    private static final int MINIMUM_AGE = 18;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String idNumber;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private boolean emailVerified = false;

    //For Hibernate
    protected Person() {
    }

    /**
     * @param email normalized to lowercase before persistence; must be non-blank
     */
    protected Person(String firstName,
                     String lastName,
                     LocalDate dateOfBirth,
                     String idNumber,
                     String email) {
        this.firstName = validateName(firstName);
        this.lastName = validateName(lastName);
        this.dateOfBirth = validateDateOfBirth(dateOfBirth);
        this.idNumber = validateIdNumber(idNumber);
        this.email = validateEmail(email);
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    private String validateName(String name){
        if(name==null || name.isEmpty())
            throw new IllegalArgumentException("Name cannot be empty/null");

        return name;
    }

    private LocalDate validateDateOfBirth(LocalDate dateOfBirth) {
        LocalDate ageLimitDate = LocalDate.now().minusYears(MINIMUM_AGE);

        if (dateOfBirth == null)
            throw new IllegalArgumentException(
                    "Date of birth cannot be null"
            );
        if (dateOfBirth.isAfter(ageLimitDate))
            throw new IllegalArgumentException(
                    "User must be at least 18 years of age. Date of birth cannot be later than " +  ageLimitDate
            );

        return dateOfBirth;
    }

    private String validateIdNumber(String number){
        if(number==null || number.isEmpty())
            throw new IllegalArgumentException("Identification number cannot be empty/null");
        return number;
    }

    private String validateEmail(String email) {
        if (email == null || email.isEmpty())
            throw new IllegalArgumentException("Email cannot be empty/null");
        return email.toLowerCase();
    }

    //TODO validacija za idNumber
}
