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
    expect(fetchStub).toHaveBeenCalledTimes(1);
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

    fireEvent.click(screen.getByRole("radio", { name: /^Open/ }));

    expect(screen.getByText("Write the spec")).toBeTruthy();
    expect(screen.getByText("Review the pull request")).toBeTruthy();
    expect(screen.queryByText("Ship it")).toBeNull();
    // Open is cut on the client from the list that is already loaded.
    expect(fetchStub).toHaveBeenCalledTimes(1);
  });

  it("reads again with ?status= when you choose one status, because the server filters it (A14)", async () => {
    const fetchStub = stubFetch((input) =>
      Promise.resolve(jsonResponse(String(input).includes("status=DONE") ? [tasks[1]] : tasks)),
    );
    render(<TaskPanel boardId={1} />);
    await screen.findByText("Write the spec");

    fireEvent.click(screen.getByRole("radio", { name: "Done" }));

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
