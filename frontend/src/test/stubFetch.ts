import { vi } from "vitest";

type FetchHandler = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;

/**
 * Replaces `fetch` for one test. The API client is the only code that calls `fetch`, so a test that
 * renders a component sees the answer of this handler and never reaches a real backend.
 * Call `vi.unstubAllGlobals()` after the test.
 */
export function stubFetch(handler: FetchHandler) {
  const stub = vi.fn(handler);
  vi.stubGlobal("fetch", stub);
  return stub;
}
