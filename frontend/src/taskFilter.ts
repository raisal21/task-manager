import type { Task, TaskStatus } from "./api/types";

/**
 * "OPEN" is a view of the client: the tasks that are not done (To Do and In Progress together).
 * The API has no such status and filters one status at a time (R10, A14).
 */
export type StatusFilterValue = "ALL" | "OPEN" | TaskStatus;

const OPEN_STATUSES: readonly TaskStatus[] = ["TODO", "IN_PROGRESS"];

/** The predicate of the Open filter. */
export function isOpen(task: Pick<Task, "status">): boolean {
  return OPEN_STATUSES.includes(task.status);
}

/**
 * The `?status=` value of the read for a filter. null reads all tasks of the board, which is also what
 * the Open filter needs. Only a single status is filtered by the server.
 */
export function serverStatus(filter: StatusFilterValue): TaskStatus | null {
  return filter === "ALL" || filter === "OPEN" ? null : filter;
}

/** The tasks that a list shows. The server already applied a single status, so only Open is cut here. */
export function visibleTasks(tasks: Task[], filter: StatusFilterValue): Task[] {
  return filter === "OPEN" ? tasks.filter(isOpen) : tasks;
}
