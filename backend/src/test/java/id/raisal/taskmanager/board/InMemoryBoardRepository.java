package id.raisal.taskmanager.board;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A fake for unit tests. It keeps boards in memory and obeys the contract of the repository interface. */
public class InMemoryBoardRepository implements BoardRepository {

    private final List<Board> boards = new ArrayList<>();
    private long lastId = 0;

    public void add(Board board) {
        boards.add(board);
        lastId = Math.max(lastId, board.getId());
    }

    public List<Board> all() {
        return List.copyOf(boards);
    }

    public void clear() {
        boards.clear();
        lastId = 0;
    }

    @Override
    public List<Board> findAllByOrderByCreatedAtAscIdAsc() {
        return boards.stream()
                .sorted(Comparator.comparing(Board::getCreatedAt).thenComparing(Board::getId))
                .toList();
    }

    @Override
    public Board save(Board board) {
        Board saved = new Board(++lastId, board.getName(), board.getCreatedAt());
        boards.add(saved);
        return saved;
    }

    @Override
    public Optional<Board> findById(Long id) {
        return boards.stream().filter(board -> board.getId().equals(id)).findFirst();
    }

    @Override
    public int deleteBoardById(Long id) {
        return boards.removeIf(board -> board.getId().equals(id)) ? 1 : 0;
    }
}
