import { useCallback, useEffect, useRef, useState } from "react";
import { toApiError } from "../api/client";
import type { ApiError } from "../api/types";

export type RequestState<T> =
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: ApiError };

type ReadFunction<T> = (signal: AbortSignal) => Promise<T>;

/**
 * The state of one read, and the read guard for it. Both lists use it.
 *
 * - The read starts when the component mounts. Until the first response, the state is "loading".
 * - When `read` changes (for example another filter), the state is "loading" at once, and a new read starts.
 *   The data of a previous `read` is never shown for the new one.
 * - `reload` starts the current read again. The state keeps its last value until the new response comes.
 *   It always uses the current `read`, also if it was made in an earlier render (for example by a write that
 *   ends after a filter change). So no write can start a read for a previous filter.
 * - Only the latest read can change the state. Each new read stops the previous read, and the response of a
 *   read that is not the latest is ignored, for a success and for an error.
 * - After the component unmounts, no read is current, and `reload` does nothing. A late result of a form
 *   of a removed component cannot start a read.
 *
 * `read` must keep its identity between renders (a function outside the component, or `useCallback`).
 * A component that reads for another board must get a `key`, so that its forms and rows start new.
 */
export function useRequest<T>(read: ReadFunction<T>): {
  state: RequestState<T>;
  reload: () => void;
} {
  // The result carries the read that made it. A result of another read is not the state of this read.
  const [result, setResult] = useState<{
    read: ReadFunction<T>;
    state: Exclude<RequestState<T>, { status: "loading" }>;
  } | null>(null);
  const latestRead = useRef<AbortController | null>(null);
  const currentRead = useRef(read);
  const active = useRef(false);

  const start = useCallback((readToStart: ReadFunction<T>) => {
    latestRead.current?.abort();
    const controller = new AbortController();
    latestRead.current = controller;

    readToStart(controller.signal).then(
      (data) => {
        if (latestRead.current === controller) {
          setResult({ read: readToStart, state: { status: "success", data } });
        }
      },
      (error: unknown) => {
        if (latestRead.current === controller) {
          setResult({ read: readToStart, state: { status: "error", error: toApiError(error) } });
        }
      },
    );
  }, []);

  useEffect(() => {
    active.current = true;
    currentRead.current = read;
    start(read);
    return () => {
      active.current = false;
      latestRead.current?.abort();
      latestRead.current = null;
    };
  }, [read, start]);

  const reload = useCallback(() => {
    if (active.current) {
      start(currentRead.current);
    }
  }, [start]);

  const state: RequestState<T> =
    result !== null && result.read === read ? result.state : { status: "loading" };
  return { state, reload };
}
