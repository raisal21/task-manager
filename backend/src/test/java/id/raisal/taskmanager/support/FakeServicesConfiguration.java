package id.raisal.taskmanager.support;

import id.raisal.taskmanager.board.BoardService;
import id.raisal.taskmanager.board.InMemoryBoardRepository;
import id.raisal.taskmanager.task.InMemoryTaskRepository;
import id.raisal.taskmanager.task.TaskService;
import id.raisal.taskmanager.common.error.ErrorWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** The services with fake repositories and a fixed clock, for web slice tests. No database. */
@TestConfiguration
@Import(ErrorWriter.class)
public class FakeServicesConfiguration {

    public static final Instant NOW = Instant.parse("2026-02-03T04:05:06Z");

    @Bean
    InMemoryBoardRepository boardRepository() {
        return new InMemoryBoardRepository();
    }

    @Bean
    InMemoryTaskRepository taskRepository() {
        return new InMemoryTaskRepository();
    }

    @Bean
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Bean
    BoardService boardService(InMemoryBoardRepository repository, Clock clock) {
        return new BoardService(repository, clock);
    }

    @Bean
    TaskService taskService(InMemoryTaskRepository tasks, InMemoryBoardRepository boards, Clock clock) {
        return new TaskService(tasks, boards, clock);
    }
}
