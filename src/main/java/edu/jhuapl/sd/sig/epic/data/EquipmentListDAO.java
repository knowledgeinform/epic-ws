/*
 * COPYRIGHT NOTICE
 * (C) 2026 The Johns Hopkins University Applied Physics Laboratory LLC.
 * All Rights Reserved.
 * This material may only be used, modified, or reproduced by or for the U.S. Government
 * pursuant to the license rights granted under FAR clause 52.227-14 or DFARS clauses
 * 252.227-7013/7014.
 * For any other permission, please contact the Legal Office at JHU/APL.
 */
package edu.jhuapl.sd.sig.epic.data;

import edu.jhuapl.sd.sig.epic.data.util.JPAUtils;
import edu.jhuapl.sd.sig.epic.model.*;
import edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.WebApplicationException;
import java.util.*;
import java.util.stream.Collectors;

public class EquipmentListDAO
{

    private static final Logger LOGGER = LogManager.getLogger();

    public static EquipmentList getEquipmentListForRun(EntityManager em, Integer runId)
    {
        EquipmentList el = null;
        String qString = "SELECT e FROM EquipmentList e WHERE e.run.pk = :runId";
        TypedQuery<EquipmentList> q = em.createQuery(qString, EquipmentList.class);
        List<EquipmentList> els = q.getResultList();
        if (els != null && els.size() > 0)
        {
            el = els.get(0);
        }
        return el;
    }

    /**
     * This method deletes an equipment item from the run as a whole, including any steps to which
     * the equipment might be linked. User is passed in as a parameter for the purposes of generating a
     * history message documenting the change.
     * 
     * @param em
     * @param pk
     * @param user
     * @return
     */
    public static Run deleteEquipmentListItem(EntityManager em, int pk, Users user)
    {
        EquipmentList eq = JPAUtils.getRecordById(em, EquipmentList.class, pk);
        Run run = JPAUtils.getRecordById(em, Run.class, eq.getRun().getPk());

        try
        {
            em.getTransaction().begin();

            // Remove equipment from collections
            run.getEquipmentList().remove(eq);

            // generate history for the run
            History runHistory = new History(eq, null, user);
            runHistory.setProcedureDetails(run.getProcedureDetails());
            em.persist(runHistory);
            run.getProcedureDetails().getHistories().add(runHistory);

            // add run step history to any steps associated with this equipment
            Set<StepDef> steps = eq.getSteps();
            if (steps != null && !steps.isEmpty())
            {
                for (StepDef step : steps)
                {
                    // generate a run step history comment
                    History history = new History(eq, null, user);
                    history.setStepDef(step);
                    em.persist(history);
                    step.getEquipment().remove(eq);
                    step.getHistories().add(history);
                    em.merge(step);
                }
            }
            em.remove(eq);
            em.merge(run);
            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String message = "Error removing Equipment List Item with pk: " + pk + " from database";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
        return run;
    }

    /**
     * `stepPk` should be null if this equipment list item does not pertain to a specific step. In that case, `runPk` should still be set.
     * This method adds an equipment list item to a step. User is passed in as a parameter for the purposes of creating a history message.
     * 
     * @param em
     * @param runPk
     * @param stepPk
     * @param data
     * @param user
     * @return
     */
    public static EquipmentList setEquipmentListItemForRun(EntityManager em, Integer runPk, Integer stepPk, EquipmentList data, Users user)
    {
        EquipmentList eq;
        Run run = RunDAO.getRunByPk(em, runPk);
        StepDef newStepDef = null;
        try
        {
            em.getTransaction().begin();

            if (data.getPk() != null)
            {
                eq = JPAUtils.getRecordById(em, EquipmentList.class, data.getPk());
            }
            else
            {
                eq = new EquipmentList();
            }

            eq.setName(data.getName());
            eq.setSerialNumber(data.getSerialNumber());
            eq.setPropertyNumber(data.getPropertyNumber());
            eq.setCalibrationDate(data.getCalibrationDate());
            eq.setCalibrationDueDate(data.getCalibrationDueDate());

            if (eq.getRun() == null)
            {
                eq.setRun(run);

                // generate a history for the run
                History runHistory = new History(null, data, user);
                runHistory.setProcedureDetails(run.getProcedureDetails());
                em.persist(runHistory);
                run.getProcedureDetails().getHistories().add(runHistory);
            }
            if (stepPk != null)
            {
                newStepDef = JPAUtils.getRecordById(em, StepDef.class, stepPk);
                StepDef finalNewStepDef = newStepDef;
                if (data.getSteps() == null || data.getSteps().stream().noneMatch(stepDef -> stepDef.getPk().equals(finalNewStepDef.getPk())))
                {
                    // if here, this step pk is not currently associated with the equipment, which means it needs
                    // a run step history added to the step
                    History history = new History(null, data, user);
                    history.setStepDef(newStepDef);
                    em.persist(history);
                    newStepDef.getHistories().add(history);
                }

                if (eq.getSteps() == null)
                {
                    eq.setSteps(new HashSet<>());
                }

                eq.getSteps().add(newStepDef);
            }
            eq = em.merge(eq);

            run.getEquipmentList().add(eq);
            em.merge(run);

            if (newStepDef != null)
            {
                newStepDef.getEquipment().add(eq);
                em.merge(newStepDef);
            }

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error updating EquipmentList with PK: " + data.getPk() + " for run with PK: " + runPk;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }
        return eq;
    }

    public static StepDef removeItemFromStep(EntityManager em, Integer equipmentPk, Integer stepPk, Users user)
    {
        StepDef step = null;

        try
        {
            em.getTransaction().begin();

            // Get the equipment
            EquipmentList equipment = JPAUtils.getRecordById(em, EquipmentList.class, equipmentPk);

            // Get the step
            step = em.find(StepDef.class, stepPk);

            // Remove the step
            equipment.getSteps().remove(step);

            // Commit changes to the equipment
            em.merge(equipment);

            // generate history for the step
            History history = new History(equipment, null, user);
            history.setStepDef(step);
            em.persist(history);

            em.getTransaction().commit();
        }
        catch (Exception e)
        {
            if (em.getTransaction().isActive())
            {
                em.getTransaction().rollback();
            }
            String msg = "Error removing EquipmentList with PK: " + equipmentPk + " from step with PK: " + stepPk;
            LOGGER.error(msg, e);
            throw new WebApplicationException(msg, e);
        }

        em.refresh(step);
        return step;
    }

    /**
     * Returns list of equipment used for populating the equipment report based on given parameters.
     * Groups the results based on property and serial numbers and concatenates the names
     * 
     * @param em
     * @param programPk
     * @param subsystemPk
     * @param testingPhasePk
     * @return
     */
    public static List<EquipmentList> getAllEquipmentForReport(EntityManager em, Integer programPk, Integer subsystemPk, Integer testingPhasePk)
    {
        List<EquipmentList> allEquipment = null;
        try
        {
            // first write query to return relevant entries from equipment list given the global filters.
            // base query - if there are no global filters, just this gets run.
            String query = "SELECT e from EquipmentList e";

            // adds to the query based on the global filters.
            if (programPk != null || subsystemPk != null || testingPhasePk != null)
            {
                query += " JOIN e.run r JOIN r.procedureDetails pt JOIN pt.procedureDef pd WHERE";
            }
            if (programPk != null)
            {
                query += " pd.program.pk = :programPk";
            }
            if (subsystemPk != null)
            {
                if (programPk != null)
                {
                    query += " AND";
                }
                query += " pd.subsystem.pk = :subsystemPk";
            }
            if (testingPhasePk != null)
            {
                if (programPk != null || subsystemPk != null)
                {
                    query += " AND";
                }
                query += " r.testingPhase.pk = :testingPhasePk";
            }

            // turn it into a TypedQuery and set parameters based on global filters.
            TypedQuery<EquipmentList> q = em.createQuery(query, EquipmentList.class);
            if (programPk != null)
            {
                q.setParameter("programPk", programPk);
            }
            if (subsystemPk != null)
            {
                q.setParameter("subsystemPk", subsystemPk);
            }
            if (testingPhasePk != null)
            {
                q.setParameter("testingPhasePk", testingPhasePk);
            }

            // result of query is a list of equipment.
            List<EquipmentList> equipment = q.getResultList();

            // in the following block, we are first grouping the equipment by property number;
            // this results in a list of lists of equipment where each list contains equipment with different names
            // and possibly serial numbers but same property. We then consolidate each of these lists to a single
            // EquipmentList object that has one property number and (comma separated) strings for serial number and name.
            // We then examine the consolidated objects for matching serial numbers. If serial numbers match for two
            // given objects those are further consolidated into one object. We ultimately return the list of consolidated objects.
            allEquipment = equipment.stream()
                    .collect(Collectors.groupingBy(e -> e.getPropertyNumber()))
                    .values()
                    .stream()
                    .reduce(new ArrayList<EquipmentList>(), (partialList, list) ->
                    {
                        if (list.get(0).getPropertyNumber().isEmpty())
                        {
                            List<EquipmentList> noPropNumber = list.stream().collect(Collectors.groupingBy(e -> e.getSerialNumber()))
                                    .values()
                                    .stream()
                                    .reduce(new ArrayList<EquipmentList>(), (blankPropertyNumberList, sameSerials) ->
                                    {
                                        // if here, property number is blank for these equipment, and they are now grouped on serial number
                                        EquipmentList consolidatedEquipment = sameSerials.stream()
                                                .reduce(new EquipmentList(), (newEquipment, equip) ->
                                                {
                                                    if (newEquipment.getPropertyNumber() == null)
                                                    {
                                                        newEquipment.setSerialNumber(equip.getSerialNumber());
                                                        newEquipment.setName(equip.getName());
                                                        newEquipment.setPropertyNumber(equip.getPropertyNumber()); // this is an empty string
                                                    }
                                                    if (!equip.getName().equalsIgnoreCase(newEquipment.getName()))
                                                    {
                                                        newEquipment.setName(newEquipment.getName() + ", " + equip.getName());
                                                    }
                                                    return newEquipment;
                                                });
                                        blankPropertyNumberList.add(consolidatedEquipment);
                                        return blankPropertyNumberList;
                                    });
                            partialList.addAll(noPropNumber);
                        }
                        else
                        {
                            EquipmentList consolidatedEquipment = list.stream()
                                    .reduce(new EquipmentList(), (newEquipment, equip) ->
                                    {
                                        // all the elements in this list have the same property number
                                        // if newEquipment has null fields, is the first element, populate it with the first element's values.
                                        if (newEquipment.getPropertyNumber() == null)
                                        {
                                            newEquipment.setSerialNumber(equip.getSerialNumber());
                                            newEquipment.setName(equip.getName());
                                            newEquipment.setPropertyNumber(equip.getPropertyNumber());
                                        }
                                        if (!equip.getSerialNumber().equalsIgnoreCase(newEquipment.getSerialNumber()))
                                        {
                                            newEquipment.setSerialNumber(newEquipment.getSerialNumber().isEmpty() ? equip.getSerialNumber() : (equip.getSerialNumber().isEmpty() ? newEquipment
                                                    .getSerialNumber() : newEquipment.getSerialNumber() + ", " + equip.getSerialNumber()));
                                        }
                                        if (!equip.getName().equalsIgnoreCase(newEquipment.getName()))
                                        {
                                            newEquipment.setName(newEquipment.getName() + ", " + equip.getName());
                                        }
                                        return newEquipment;
                                    });

                            // we've now reduced the list down to one consolidated equipment. We need to check if this equipment
                            // matches on serial number to any of the other consolidated equipment.
                            boolean found = false;
                            outerloop: for (EquipmentList e : partialList)
                            {
                                // during the reduce action above, consolidatedEquipment could potentially have a string for
                                // serialNumber that is multiple comma-separated values.
                                List<String> serialNumbersToMatch = new ArrayList<String>(Arrays.asList(consolidatedEquipment.getSerialNumber().split(", ")));
                                serialNumbersToMatch
                                        .stream()
                                        .forEach(s ->
                                        {
                                            s.trim();
                                        });
                                for (String serial : serialNumbersToMatch)
                                {
                                    if (e.getSerialNumber().equalsIgnoreCase(serial) && !e.getSerialNumber().isEmpty())
                                    {
                                        e.setPropertyNumber(e.getPropertyNumber().isEmpty() ? consolidatedEquipment.getPropertyNumber() : (consolidatedEquipment.getPropertyNumber().isEmpty() ? e
                                                .getPropertyNumber() : e.getPropertyNumber() + ", " + consolidatedEquipment.getPropertyNumber()));
                                        if (!e.getName().equalsIgnoreCase(consolidatedEquipment.getName()))
                                        {
                                            e.setName(e.getName() + ", " + consolidatedEquipment.getName());
                                        }
                                        found = true;
                                        break outerloop;
                                    }
                                }
                            }
                            if (!found)
                            {
                                partialList.add(consolidatedEquipment);
                            }
                        }
                        return partialList;
                    });

            // return the list of equipment
            return allEquipment;
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of all equipment for given filters";
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
    }

    public static Set<RunListDTO> getAllRunsAssociatedWithEquipment(EntityManager em, String equipmentPropertyNumber,
            String equipmentSerialNumber)
    {
        boolean hasPropertyNumber = equipmentPropertyNumber != null && !equipmentPropertyNumber.isEmpty();
        boolean hasSerialNumber = equipmentSerialNumber != null && !equipmentSerialNumber.isEmpty();
        Set<RunListDTO> allRunsForEquipment = null;
        try
        {
            String query = "SELECT new edu.jhuapl.sd.sig.epic.model.display.dto.RunListDTO(pDet, pDef.name, pDef.program, pDef.subsystem, r.createdDate, r.user) " +
                    "from ProcedureDetails pDet JOIN pDet.procedureDef pDef JOIN pDet.run r JOIN r.equipmentList eq " +
                    "WHERE pDet.editType != :editType";

            if (hasPropertyNumber && hasSerialNumber)
            {
                query += " AND (UPPER(eq.propertyNumber) = UPPER(:equipmentPropertyNumber) OR UPPER(eq.serialNumber) = UPPER(:equipmentSerialNumber))";
            }
            else if (hasPropertyNumber)
            {
                query += " AND UPPER(eq.propertyNumber) = UPPER(:equipmentPropertyNumber)";
            }
            else if (hasSerialNumber)
            {
                query += " AND UPPER(eq.serialNumber) = UPPER(:equipmentSerialNumber)";
            }

            TypedQuery<RunListDTO> q = em.createQuery(query, RunListDTO.class);
            q.setParameter("editType", EditType.ORIGINAL);
            if (hasPropertyNumber)
            {
                q.setParameter("equipmentPropertyNumber", equipmentPropertyNumber);
            }
            if (hasSerialNumber)
            {
                q.setParameter("equipmentSerialNumber", equipmentSerialNumber);
            }

            allRunsForEquipment = new HashSet<>(q.getResultList());
            return allRunsForEquipment;
        }
        catch (Exception e)
        {
            String message = "Could not retrieve list of all runs for equipment with property number " + equipmentPropertyNumber +
                    " and/or serial number " + equipmentSerialNumber;
            LOGGER.error(message, e);
            throw new WebApplicationException(message, e);
        }
    }
}
