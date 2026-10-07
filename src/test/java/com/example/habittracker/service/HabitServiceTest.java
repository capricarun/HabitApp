package com.example.habittracker.service;

import com.example.habittracker.dto.HabitRequest;
import com.example.habittracker.dto.HabitStats;
import com.example.habittracker.model.Habit;
import com.example.habittracker.repository.HabitRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HabitServiceTest {

    private HabitService service() {
        return new HabitService(new HabitRepository());
    }

    private HabitRequest request(String name, String description) {
        HabitRequest request = new HabitRequest();
        request.setName(name);
        request.setDescription(description);
        request.setFrequency(Habit.Frequency.DAILY);
        return request;
    }

    @Test
    void createAssignsIdAndDefaults() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read 10 pages")
        );

        assertNotNull(habit.getId());
        assertEquals("Read", habit.getName());
        assertEquals("Read 10 pages", habit.getDescription());
        assertTrue(habit.isActive());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        HabitService service = service();

        assertThrows(
                HabitNotFoundException.class,
                () -> service.getById(999999L)
        );
    }

    @Test
    void updateChangesNameAndDescription() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Old description")
        );

        Habit updated = service.update(
                habit.getId(),
                request("Read More", "New description")
        );

        assertEquals("Read More", updated.getName());
        assertEquals("New description", updated.getDescription());
    }

    @Test
    void deleteRemovesHabit() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read daily")
        );

        service.delete(habit.getId());

        assertThrows(
                HabitNotFoundException.class,
                () -> service.getById(habit.getId())
        );
    }

    @Test
    void setActiveTogglesFlag() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read daily")
        );

        assertTrue(habit.isActive());

        Habit updated = service.setActive(habit.getId(), false);

        assertFalse(updated.isActive());
    }

    @Test
    void streaksAreCalculatedForConsecutiveDays() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read daily")
        );

        service.markComplete(
                habit.getId(),
                LocalDate.now().minusDays(2)
        );
        service.markComplete(
                habit.getId(),
                LocalDate.now().minusDays(1)
        );
        service.markComplete(
                habit.getId(),
                LocalDate.now()
        );

        Habit updated = service.getById(habit.getId());

        assertEquals(3, updated.getCurrentStreak());
    }

    @Test
    void unmarkCompleteResetsStreak() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read daily")
        );

        service.markComplete(
                habit.getId(),
                LocalDate.now().minusDays(1)
        );
        service.markComplete(
                habit.getId(),
                LocalDate.now()
        );

        service.unmarkComplete(
                habit.getId(),
                LocalDate.now().minusDays(1)
        );
        service.unmarkComplete(
                habit.getId(),
                LocalDate.now()
        );

        Habit updated = service.getById(habit.getId());

        assertEquals(0, updated.getCurrentStreak());
    }

    @Test
    void statsReportLastSevenDaysRate() {
        HabitService service = service();

        Habit habit = service.create(
                request("Read", "Read daily")
        );

        service.markComplete(
                habit.getId(),
                LocalDate.now()
        );
        service.markComplete(
                habit.getId(),
                LocalDate.now().minusDays(1)
        );

        HabitStats stats = service.getStats(habit.getId());

        assertEquals(28.6, stats.getLast7DaysCompletionRate(), 0.1);
    }
}
