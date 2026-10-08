import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import TaskPanel from "./TaskPanel";
import { stubFetch } from "./test/stubFetch";

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
