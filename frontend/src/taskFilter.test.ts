import { describe, expect, it } from "vitest";
import { makeTask as task } from "./test/tasks";
import { isOpen, serverStatus, visibleTasks } from "./taskFilter";

describe("Open filter", () => {
  it("counts To Do and In Progress as open, and Done as not open", () => {
    expect(isOpen(task(1, "TODO"))).toBe(true);
    expect(isOpen(task(2, "IN_PROGRESS"))).toBe(true);
    expect(isOpen(task(3, "DONE"))).toBe(false);
  });

  it("keeps only the open tasks, in their order", () => {
    const tasks = [task(1, "DONE"), task(2, "IN_PROGRESS"), task(3, "TODO"), task(4, "DONE")];

    expect(visibleTasks(tasks, "OPEN").map((t) => t.id)).toEqual([2, 3]);
  });

  it("does not cut the list for All or for one status, because the server did that (A14)", () => {
    const tasks = [task(1, "DONE"), task(2, "TODO")];

    expect(visibleTasks(tasks, "ALL")).toBe(tasks);
    expect(visibleTasks(tasks, "DONE")).toBe(tasks);
  });

  it("reads without ?status= for All and Open, and with it for one status", () => {
    expect(serverStatus("ALL")).toBeNull();
    expect(serverStatus("OPEN")).toBeNull();
    expect(serverStatus("IN_PROGRESS")).toBe("IN_PROGRESS");
  });
});
