import { useCallback, useEffect, useRef, useState } from "react";
import { toApiError } from "../api/client";
import type { ApiError } from "../api/types";

export type RequestState<T> =
  | { status: "loading" }
  | { status: "success"; data: T }
  | { status: "error"; error: ApiError };

/**
 * The state of one read, and the read guard (P1) for it. Both lists use it.
 *
 * - The read starts when the component mounts. Until the first response, the state is "loading".
 * - `reload` starts a new read. The state keeps its last value until the new response comes.
 * - Only the latest read can change the state. Each new read stops the previous read, and the response of a
 *   read that is not the latest is ignored, for a success and for an error.
 * - After the component unmounts, no read is current, and `reload` does nothing. A late result of a form
 *   of a removed component cannot start a read.
 *
 * `read` must keep its identity between renders (a function outside the component, or `useCallback`).
 * When it changes, a new read starts. A component that reads for another input (for example another board)
 * must get a `key`, so that it does not show the data of the previous input.
 */
export function useRequest<T>(read: (signal: AbortSignal) => Promise<T>): {
  state: RequestState<T>;
  reload: () => void;
} {
  const [state, setState] = useState<RequestState<T>>({ status: "loading" });
  const latestRead = useRef<AbortController | null>(null);
  const active = useRef(false);

  const reload = useCallback(() => {
    if (!active.current) {
      return;
    }
    latestRead.current?.abort();
    const controller = new AbortController();
    latestRead.current = controller;

    read(controller.signal).then(
      (data) => {
        if (latestRead.current === controller) {
          setState({ status: "success", data });
        }
      },
      (error: unknown) => {
        if (latestRead.current === controller) {
          setState({ status: "error", error: toApiError(error) });
        }
      },
    );
  }, [read]);

  useEffect(() => {
    active.current = true;
    reload();
    return () => {
      active.current = false;
      latestRead.current?.abort();
      latestRead.current = null;
    };
  }, [reload]);

  return { state, reload };
}
