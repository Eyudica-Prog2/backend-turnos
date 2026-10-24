package ar.edu.um.turnos.appointment.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentSlot;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyScheduleSlotGeneratorTest {

    private static final LocalDate MONDAY = LocalDate.parse("2026-09-14");

    @Test
    void generatesSlotsThatExactlyFitTheScheduleBoundary() {
        List<AppointmentSlot> slots = WeeklyScheduleSlotGenerator.generate(
                MONDAY, List.of(schedule(DayOfWeek.MONDAY, "09:00", "10:00", 30)));

        assertThat(slots).containsExactly(
                slot("09:00", "09:30"),
                slot("09:30", "10:00"));
    }

    @Test
    void doesNotGenerateAPartialSlotPastTheScheduleEnd() {
        List<AppointmentSlot> slots = WeeklyScheduleSlotGenerator.generate(
                MONDAY, List.of(schedule(DayOfWeek.MONDAY, "09:00", "10:20", 30)));

        assertThat(slots).containsExactly(
                slot("09:00", "09:30"),
                slot("09:30", "10:00"));
    }

    @Test
    void returnsEmptyWhenSlotDurationIsLongerThanTheScheduleWithoutWrappingPastMidnight() {
        assertThat(WeeklyScheduleSlotGenerator.generate(
                MONDAY, List.of(schedule(DayOfWeek.MONDAY, "23:00", "23:30", 60))))
                .isEmpty();
    }

    @Test
    void returnsNoSlotsWhenThereIsNoScheduleForThatWeekday() {
        assertThat(WeeklyScheduleSlotGenerator.generate(
                MONDAY, List.of(schedule(DayOfWeek.TUESDAY, "09:00", "10:00", 30))))
                .isEmpty();
    }

    @Test
    void handlesMultipleScheduleRangesInSortedOrder() {
        List<AppointmentSlot> slots = WeeklyScheduleSlotGenerator.generate(MONDAY, List.of(
                schedule(DayOfWeek.MONDAY, "13:00", "14:00", 30),
                schedule(DayOfWeek.MONDAY, "09:00", "10:00", 30)));

        assertThat(slots).containsExactly(
                slot("09:00", "09:30"),
                slot("09:30", "10:00"),
                slot("13:00", "13:30"),
                slot("13:30", "14:00"));
    }

    @Test
    void rejectsNonPositiveSlotDuration() {
        assertThatThrownBy(() -> schedule(DayOfWeek.MONDAY, "09:00", "10:00", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Slot duration must be positive.");
    }

    private static WeeklySchedule schedule(DayOfWeek day, String start, String end, int duration) {
        return new WeeklySchedule(day, LocalTime.parse(start), LocalTime.parse(end), duration);
    }

    private static AppointmentSlot slot(String start, String end) {
        return new AppointmentSlot(MONDAY, LocalTime.parse(start), LocalTime.parse(end));
    }
}
