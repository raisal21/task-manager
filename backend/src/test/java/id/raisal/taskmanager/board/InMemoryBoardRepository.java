package id.raisal.taskmanager.board;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A fake for unit tests. It keeps boards in memory and obeys the ordering of the repository interface. */
class InMemoryBoardRepository implements BoardRepository {

    private final List<Board> boards = new ArrayList<>();

    void add(Board board) {
        boards.add(board);
    }

    @Override
    public List<Board> findAllByOrderByCreatedAtAscIdAsc() {
        return boards.stream()
                .sorted(Comparator.comparing(Board::getCreatedAt).thenComparing(Board::getId))
                .toList();
    }
}
