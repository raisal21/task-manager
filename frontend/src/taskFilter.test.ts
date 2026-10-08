import { describe, expect, it } from "vitest";
import { makeTask as task } from "./test/tasks";
import { countByStatus, countForFilter, isOpen, serverStatus, visibleTasks } from "./taskFilter";

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

describe("Status counts", () => {
  const tasks = [
    task(1, "TODO"),
    task(2, "DONE"),
    task(3, "TODO"),
    task(4, "IN_PROGRESS"),
    task(5, "TODO"),
  ];

  it("counts the tasks of each status, and gives 0 to a status without tasks", () => {
    expect(countByStatus(tasks)).toEqual({ TODO: 3, IN_PROGRESS: 1, DONE: 1 });
    expect(countByStatus([task(1, "DONE")])).toEqual({ TODO: 0, IN_PROGRESS: 0, DONE: 1 });
    expect(countByStatus([])).toEqual({ TODO: 0, IN_PROGRESS: 0, DONE: 0 });
  });

  it("gives each filter button its number: All is every task, Open is To Do plus In Progress", () => {
    const counts = countByStatus(tasks);

    expect(countForFilter(counts, "ALL")).toBe(5);
    expect(countForFilter(counts, "OPEN")).toBe(4);
    expect(countForFilter(counts, "TODO")).toBe(3);
    expect(countForFilter(counts, "IN_PROGRESS")).toBe(1);
    expect(countForFilter(counts, "DONE")).toBe(1);
  });
});
