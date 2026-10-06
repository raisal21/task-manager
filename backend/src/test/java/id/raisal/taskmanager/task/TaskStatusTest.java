package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import id.raisal.taskmanager.common.error.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskStatusTest {

    @ParameterizedTest
    @CsvSource({"TODO,TODO", "IN_PROGRESS,IN_PROGRESS", "DONE,DONE"})
    void parsesEachKnownValue(String text, TaskStatus expected) {
        assertThat(TaskStatus.parse(text)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"BLOCKED", "todo", "Done", " DONE", "DONE ", "IN-PROGRESS"})
    void rejectsUnknownValue(String text) {
        assertThatThrownBy(() -> TaskStatus.parse(text))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.getField()).isEqualTo("status");
                    assertThat(exception.getMessage()).isEqualTo("Status must be TODO, IN_PROGRESS, or DONE.");
                });
    }

    @Test
    void hasExactlyTheThreeStatusesOfTheSchema() {
        assertThat(TaskStatus.values()).containsExactly(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);
    }
}
