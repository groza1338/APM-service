package dev.sc.apm.entity;

import dev.sc.apm.util.converter.DurationConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "client")
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 64)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 64)
    private String lastName;

    @Column(name = "middle_name", length = 64)
    private String middleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 32)
    private MaritalStatus maritalStatus;

    @Column(name = "passport", nullable = false, unique = true, length = 10)
    private String passport;

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Column(name = "address", length = 128)
    private String address;

    @Column(name = "organization_name", nullable = false, length = 96)
    private String organizationName;

    @Column(name = "position", nullable = false, length = 64)
    private String position;

    @Column(name = "employment_period", length = 16)
    @Convert(converter = DurationConverter.class)
    private Duration employmentPeriod;

    @OneToMany(mappedBy = "client")
    @ToString.Exclude
    private List<CreditApplication> creditApplications;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Client client = (Client) o;
        return getId() != null && Objects.equals(getId(), client.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
