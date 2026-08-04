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
import edu.jhuapl.sd.sig.epic.data.HistoryDAO;
import lombok.*;

import javax.persistence.*;
import java.util.Comparator;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "pk")
@Entity
@Table(name = "history")
public class History implements Comparable<History>
{
    @Id
    @Column(name = "pk")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer pk;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp")
    private Date timestamp;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_history_users"))
    @JsonIgnoreProperties("procedureDetails")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_def_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_history_stepDef"))
    @JsonIgnoreProperties({"histories"})
    private StepDef stepDef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_details_pk", referencedColumnName = "pk", foreignKey = @ForeignKey(name = "fk_history_procedureDetails"))
    @JsonIgnoreProperties({"histories"})
    private ProcedureDetails procedureDetails;

    @Override
    public int compareTo(History o)
    {
        return Comparator.comparing(History::getTimestamp)
                .thenComparing(History::getDescription)
                .thenComparing(History::getPk, Comparator.nullsFirst(Integer::compareTo))
                .compare(this, o);
    }

    public History(Date timestamp, String description, Users user, StepDef stepDef, ProcedureDetails procedureDetails)
    {
        this.timestamp = timestamp;
        this.description = description;
        this.user = user;
        this.stepDef = stepDef;
        this.procedureDetails = procedureDetails;
    }

    public History(Object oldObject, Object newObject, Users user)
    {
        this.user = user;
        this.timestamp = new Date();
        if (newObject instanceof ProcedureDetails)
        {
            this.procedureDetails = (ProcedureDetails) newObject;
        }
        else if (newObject instanceof StepDef)
        {
            this.stepDef = (StepDef) newObject;
        }
        this.description = HistoryDAO.generateHistoryDescription(oldObject, newObject);
    }
}
