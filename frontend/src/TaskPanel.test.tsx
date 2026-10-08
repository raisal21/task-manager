import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import TaskPanel from "./TaskPanel";
import { stubFetch } from "./test/stubFetch";
import { jsonResponse, makeTask } from "./test/tasks";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("TaskPanel (R25)", () => {
  it("shows the loading text while the server has not answered", () => {
    // A read that never ends: the panel stays in its first state.
    const fetchStub = stubFetch(() => new Promise<Response>(() => {}));

    render(<TaskPanel boardId={1} />);

    expect(screen.getByText("Loading tasks…")).toBeTruthy();
    expect(String(fetchStub.mock.calls[0]?.[0])).toBe("http://localhost:8080/api/boards/1/tasks");
  });

  it("shows the server error, with a Retry button, when the backend is down", async () => {
    // fetch rejects with a TypeError when no server answers (stopped, wrong URL, or CORS).
    stubFetch(() => Promise.reject(new TypeError("Failed to fetch")));

    render(<TaskPanel boardId={1} />);

    const alert = await screen.findByRole("alert");
    expect(alert.textContent).toContain("Cannot reach the server at http://localhost:8080");
    expect(screen.getByRole("button", { name: "Retry" })).toBeTruthy();
    expect(screen.queryByText("Loading tasks…")).toBeNull();
  });
});

describe("TaskPanel status filter", () => {
  const tasks = [
    makeTask(1, "TODO", "Write the spec"),
    makeTask(2, "DONE", "Ship it"),
    makeTask(3, "IN_PROGRESS", "Review the pull request"),
  ];

  it("shows only the To Do and In Progress tasks after you choose Open, without a new read", async () => {
    const fetchStub = stubFetch(() => Promise.resolve(jsonResponse(tasks)));
    render(<TaskPanel boardId={1} />);
    await screen.findByText("Ship it");

    const readsBefore = fetchStub.mock.calls.length;

    fireEvent.click(screen.getByRole("radio", { name: /^Open/ }));

    expect(screen.getByText("Write the spec")).toBeTruthy();
    expect(screen.getByText("Review the pull request")).toBeTruthy();
    expect(screen.queryByText("Ship it")).toBeNull();
    // Open is cut on the client from the list that is already loaded.
    expect(fetchStub).toHaveBeenCalledTimes(readsBefore);
  });

  it("reads again with ?status= when you choose one status, because the server filters it (A14)", async () => {
    const fetchStub = stubFetch((input) =>
      Promise.resolve(jsonResponse(String(input).includes("status=DONE") ? [tasks[1]] : tasks)),
    );
    render(<TaskPanel boardId={1} />);
    await screen.findByText("Write the spec");

    fireEvent.click(screen.getByRole("radio", { name: /^Done/ }));

    await screen.findByText("Ship it");
    expect(screen.queryByText("Write the spec")).toBeNull();
    expect(String(fetchStub.mock.calls.at(-1)?.[0])).toBe(
      "http://localhost:8080/api/boards/1/tasks?status=DONE",
    );
  });

  it("says that nothing is open when every task is done", async () => {
    stubFetch(() => Promise.resolve(jsonResponse([makeTask(1, "DONE", "Ship it")])));
    render(<TaskPanel boardId={1} />);
    await screen.findByText("Ship it");

    fireEvent.click(screen.getByRole("radio", { name: /^Open/ }));

    expect(screen.getByText("No open tasks (To Do or In Progress).")).toBeTruthy();
  });
});

describe("TaskPanel status counts", () => {
  const tasks = [
    makeTask(1, "TODO", "Write the spec"),
    makeTask(2, "DONE", "Ship it"),
    makeTask(3, "IN_PROGRESS", "Review the pull request"),
  ];

  it("shows the number of tasks on each filter button, from one read of the whole board", async () => {
    const fetchStub = stubFetch(() => Promise.resolve(jsonResponse(tasks)));
    render(<TaskPanel boardId={1} />);

    // The accessible name is the label text and the number.
    expect(await screen.findByRole("radio", { name: "All 3" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "Open (To Do and In Progress) 2" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "To Do 1" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "In Progress 1" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "Done 1" })).toBeTruthy();
    // The list and the counts are two reads of the same URL: one more request for the board.
    expect(fetchStub.mock.calls.map(([input]) => String(input))).toEqual([
      "http://localhost:8080/api/boards/1/tasks",
      "http://localhost:8080/api/boards/1/tasks",
    ]);
  });

  it("keeps the numbers of the whole board when the list shows one status", async () => {
    stubFetch((input) =>
      Promise.resolve(jsonResponse(String(input).includes("status=DONE") ? [tasks[1]] : tasks)),
    );
    render(<TaskPanel boardId={1} />);
    await screen.findByRole("radio", { name: "All 3" });

    fireEvent.click(screen.getByRole("radio", { name: /^Done/ }));

    await screen.findByText("Ship it");
    expect(screen.queryByText("Write the spec")).toBeNull();
    expect(screen.getByRole("radio", { name: "All 3" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "To Do 1" })).toBeTruthy();
  });

  it("reads the numbers again after a status change", async () => {
    const board = tasks.map((task) => ({ ...task }));
    stubFetch((_input, init) => {
      if (init?.method === "PATCH") {
        board[0] = { ...board[0], status: "DONE" };
        return Promise.resolve(jsonResponse(board[0]));
      }
      return Promise.resolve(jsonResponse(board));
    });
    render(<TaskPanel boardId={1} />);
    await screen.findByRole("radio", { name: "To Do 1" });

    fireEvent.change(screen.getByLabelText("Status of Write the spec"), {
      target: { value: "DONE" },
    });

    expect(await screen.findByRole("radio", { name: "To Do 0" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "Done 2" })).toBeTruthy();
    expect(screen.getByRole("radio", { name: "Open (To Do and In Progress) 1" })).toBeTruthy();
  });

  it("still lists the tasks, without numbers, when only the counts cannot be read", async () => {
    // The list read starts first, and the counts read second.
    let reads = 0;
    stubFetch(() => {
      reads += 1;
      return reads === 1
        ? Promise.resolve(jsonResponse(tasks))
        : Promise.reject(new TypeError("Failed to fetch"));
    });
    render(<TaskPanel boardId={1} />);

    expect(await screen.findByText("Ship it")).toBeTruthy();
    expect(screen.getByRole("radio", { name: "Done" })).toBeTruthy();
    expect(screen.queryByRole("alert")).toBeNull();
  });
});
