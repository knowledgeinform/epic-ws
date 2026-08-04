/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.javers;

import edu.jhuapl.sd.sig.epic.model.ChangeLogsHolder;
import org.javers.common.collections.Sets;
import org.javers.common.string.ToStringBuilder;
import org.javers.core.metamodel.annotation.*;
import java.time.ZonedDateTime;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author bartosz walacik
 */
@TypeName("Employee")
public class Employee extends ChangeLogsHolder
{

    private int pk;
    @Id
    private String name;
    private int salary;

    private Employee boss;

    private List<Employee> subordinates = new ArrayList<>();

    private Set<String> skills;

    private Map<Integer, String> performance;

    private ZonedDateTime lastPromotionDate;

    public Employee()
    {}

    public Employee(String name)
    {
        this(name, 10000);
    }

    public Employee(String name, int salary)
    {
        checkNotNull(name);
        this.name = name;
        this.salary = salary;
    }

    public Employee(String name, int salary, String position)
    {
        checkNotNull(name);
        checkNotNull(position);
        this.name = name;
        this.salary = salary;
    }

    @Override
    public String getId()
    {
        return name;
    }

    public Employee addSubordinate(Employee employee)
    {
        checkNotNull(employee);
        employee.boss = this;
        subordinates.add(employee);
        return this;
    }

    public Employee addSubordinates(Employee... employees)
    {
        checkNotNull(employees);
        for (Employee e : employees)
        {
            addSubordinate(e);
        }
        return this;
    }

    public ZonedDateTime getLastPromotionDate()
    {
        return lastPromotionDate;
    }

    public String getName()
    {
        return name;
    }

    public Employee getBoss()
    {
        return boss;
    }

    public int getSalary()
    {
        return salary;
    }

    public List<Employee> getSubordinates()
    {
        return Collections.unmodifiableList(subordinates);
    }

    public Set<String> getSkills()
    {
        return Collections.unmodifiableSet(this.skills);
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSalary(int salary)
    {
        this.salary = salary;
    }

    public void setBoss(Employee boss)
    {
        this.boss = boss;
    }

    public void setSubordinates(List<Employee> subordinates)
    {
        this.subordinates = subordinates;
    }

    public void setSkills(String... skills)
    {
        this.skills = Sets.asSet(skills);
    }

    void setLastPromotionDate(ZonedDateTime lastPromotionDate)
    {
        this.lastPromotionDate = lastPromotionDate;
    }

    public int getPk()
    {
        return pk;
    }

    public void setPk(int pk)
    {
        this.pk = pk;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Employee))
        {
            return false;
        }
        Employee that = (Employee) obj;

        return this.name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.toString(this,
                "pk", String.valueOf(pk),
                "name", name,
                "salary", salary,
                "boss", boss != null ? boss.name : "",
                "subordinates", subordinates.size());
    }
}
