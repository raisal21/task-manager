export interface Board {
  id: number;
  name: string;
  /** ISO-8601 UTC time from the API. */
  createdAt: string;
}

export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

export interface Task {
  id: number;
  boardId: number;
  title: string;
  /** null when the task has no description. */
  description: string | null;
  status: TaskStatus;
  /** ISO-8601 UTC times from the API. */
  createdAt: string;
  updatedAt: string;
}

export interface NewTask {
  title: string;
  /** An empty text is allowed. The backend stores it as null (A10). */
  description: string;
}

/**
 * network: no response at all (backend stopped, wrong URL, or CORS). fetch gives a TypeError.
 * http: the backend sent an error response.
 * unexpected: all other faults, for example a response that is not JSON.
 */
export type ApiErrorKind = "network" | "http" | "unexpected";

export interface ApiErrorDetails {
  status?: number;
  code?: string;
  detail?: string;
  field?: string;
}

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  readonly code?: string;
  readonly detail?: string;
  readonly field?: string;

  constructor(kind: ApiErrorKind, message: string, details: ApiErrorDetails = {}) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = details.status;
    this.code = details.code;
    this.detail = details.detail;
    this.field = details.field;
  }
}
