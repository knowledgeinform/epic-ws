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
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "equipment_list")
public class EquipmentList
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pk")
    private Integer pk;

    @Column(name = "name")
    private String name;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "property_number")
    private String propertyNumber;

    @Column(name = "calibration_date")
    private Date calibrationDate;

    @Column(name = "calibration_due_date")
    private Date calibrationDueDate;

    @ManyToOne
    @JoinColumn(name = "run_pk", referencedColumnName = "pk", nullable = false, foreignKey = @ForeignKey(name = "fk_equipmentList_run"))
    @JsonIgnoreProperties({"equipmentList", "step"})
    private Run run;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "step_equipment_list",
            joinColumns = @JoinColumn(
                    name = "equipment_pk",
                    referencedColumnName = "pk"),
            inverseJoinColumns = @JoinColumn(
                    name = "step_pk",
                    referencedColumnName = "pk"))
    @JsonIgnoreProperties({"equipment", "run"})
    private Set<StepDef> steps;

    @Override
    public String toString()
    {
        return "equipment: [name: " + this.name + ", propertyNumber: " + this.propertyNumber +
                ", serialNumber: " + this.serialNumber + "]";
    }
}
