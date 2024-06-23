package service.vaxapp.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.vaxapp.model.AppointmentSlot;
import service.vaxapp.model.VaccineCentre;
import service.vaxapp.repository.AppointmentSlotRepository;
import service.vaxapp.repository.VaccineCentreRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentSlotGenerating {

    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;

    @Autowired
    private VaccineCentreRepository vaccineCentreRepository;

    /**
     * This class creates appointment slots for a given vaccine center for the next 7 days
     * with time slot from 9:00 to 17:30 with 15 mins interval. It iterates through each day within
     * the date range and each time interval within the day's start and end times to create slots.
     *
     * @param centreId The ID of the vaccine centre
     * @param startDate The start date for the slots
     * @param endDate The end date for the slots
     * @param startTime The start time for each day's slots
     * @param endTime The end time for each day's slots
     * @param intervalMinutes The interval in minutes between slots
     */


    public void generateSlots(int centreId, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, int intervalMinutes) {
        Optional<VaccineCentre> vaccineCentreOpt = vaccineCentreRepository.findById(centreId);
        if (!vaccineCentreOpt.isPresent()) {
            // Throw an exception if the vaccine centre does not exist
            throw new IllegalArgumentException("Vaccine centre not found");
        }
        // Get the vaccine centre entity
        VaccineCentre vaccineCentre = vaccineCentreOpt.get();

        List<AppointmentSlot> slots = new ArrayList<>();
        LocalDate date = startDate;


        // Loop through each date from startDate to endDate
        while (!date.isAfter(endDate)) {
            LocalTime time = startTime;
            // Loop through each time slot from startTime to endTime
            while (!time.isAfter(endTime)) {
                AppointmentSlot slot = new AppointmentSlot();
                slot.setVaccineCentre(vaccineCentre);
                slot.setDate(date);
                slot.setStartTime(time);
                slots.add(slot);
                time = time.plusMinutes(intervalMinutes);
            }
            // Move to the next date for appointment booking
            date = date.plusDays(1);
        }

        appointmentSlotRepository.saveAll(slots);
    }



    /**
     * Generates appointment slots if they do not already exist for the next specified number of days.
     *
     * @param centreId The ID of the vaccine centre
     * @param daysAhead The number of days ahead to generate slots for
     * @param startTime The start time for each day's slots
     * @param endTime The end time for each day's slots
     * @param intervalMinutes The interval in minutes between slots
     */

    public void generateSlotsIfNotExist(int centreId, int daysAhead, LocalTime startTime, LocalTime endTime, int intervalMinutes) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);

        // Loop through each date from today to endDate
        for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<AppointmentSlot> existingSlots = appointmentSlotRepository.findByDateAndCentreId(date, centreId);
            // If no slots exist for the date and centre, generate new slots
            if (existingSlots.isEmpty()) {
                generateSlots(centreId, date, date, startTime, endTime, intervalMinutes);
            }
        }
    }
}

