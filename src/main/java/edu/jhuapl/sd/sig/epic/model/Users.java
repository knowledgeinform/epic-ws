/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.javers.core.metamodel.annotation.DiffIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "userId")
@Entity
@Table(name = "users")
public class Users implements Serializable
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "is_admin")
    private Boolean isAdmin;

    @Column(name = "last_login")
    private Long lastLogin;

    @Column(name = "pin")
    //	@GeneratorType(type = PinDefaultGenerator.class)
    //	@ColumnDefault("AppConfigurationDAO.getConfigForKey(ConfigKey.SYSTEM_DEFAULT_PIN")
    @ColumnDefault("000000") // FIXME: Replace this with the configuration key value
    private String pin;

    @Column(name = "email")
    private String email;

    @Fetch(FetchMode.SUBSELECT)
    @ManyToMany(cascade = {CascadeType.ALL})
    @JoinTable(
            name = "user_procedure_detail",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "procedure_detail_pk")})
    @DiffIgnore
    @JsonIgnoreProperties({"user"})
    Set<ProcedureDetails> procedureDetails = new HashSet<>();

    //	public static final class PinDefaultGenerator implements
    //			ValueGenerator<String> {
    //		@Override
    //		public String generateValue(Session session, Object owner) {
    //			return AppConfigurationDAO.getConfigForKey(ConfigKey.SYSTEM_DEFAULT_PIN);
    //		}
    //	}
}
