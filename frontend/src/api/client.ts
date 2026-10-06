import { API_BASE_URL } from "./config";
import { ApiError, type Board, type NewTask, type Task, type TaskStatus } from "./types";

// The only file that uses fetch. Components call the functions at the end of this file.

type JsonRecord = Record<string, unknown>;

function isRecord(value: unknown): value is JsonRecord {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" && value !== "" ? value : undefined;
}

/** All failures that leave this file are ApiError values. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  if (error instanceof TypeError) {
    return new ApiError("network", `No response from ${API_BASE_URL}`);
  }
  const message = error instanceof Error ? error.message : String(error);
  return new ApiError("unexpected", message);
}

async function readHttpError(response: Response): Promise<ApiError> {
  let body: unknown;
  try {
    body = await response.json();
  } catch {
    body = undefined;
  }
  const fields = isRecord(body) ? body : {};
  // The RFC 9457 body has detail, code, and field. The default Spring Boot body (until tkt_06) has error.
  const detail =
    asString(fields.detail) ??
    asString(fields.message) ??
    asString(fields.error) ??
    asString(fields.title) ??
    asString(response.statusText);
  return new ApiError("http", detail ?? `HTTP ${response.status}`, {
    status: response.status,
    code: asString(fields.code),
    detail,
    field: asString(fields.field),
  });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: { Accept: "application/json", ...init.headers },
    });
    if (!response.ok) {
      throw await readHttpError(response);
    }
    return (await response.json()) as T;
  } catch (error) {
    throw toApiError(error);
  }
}

export function getBoards(signal?: AbortSignal): Promise<Board[]> {
  return request<Board[]>("/api/boards", { signal });
}

export function createBoard(name: string): Promise<Board> {
  return request<Board>("/api/boards", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name }),
  });
}

export function getTasks(boardId: number, signal?: AbortSignal): Promise<Task[]> {
  return request<Task[]>(`/api/boards/${boardId}/tasks`, { signal });
}

export function createTask(boardId: number, task: NewTask): Promise<Task> {
  return request<Task>(`/api/boards/${boardId}/tasks`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  });
}

export function changeTaskStatus(taskId: number, status: TaskStatus): Promise<Task> {
  return request<Task>(`/api/tasks/${taskId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
}
