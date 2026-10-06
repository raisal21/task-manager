import type { TaskStatus } from "./api/types";

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: "To do",
  IN_PROGRESS: "In progress",
  DONE: "Done",
};
