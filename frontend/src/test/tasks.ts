import type { Task, TaskStatus } from "../api/types";

export function makeTask(id: number, status: TaskStatus, title = `Task ${id}`): Task {
  return {
    id,
    boardId: 1,
    title,
    description: null,
    status,
    createdAt: "2026-10-08T00:00:00Z",
    updatedAt: "2026-10-08T00:00:00Z",
  };
}

/** A 200 response with a JSON body, as the backend sends it. */
export function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
