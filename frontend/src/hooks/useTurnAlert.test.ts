import { renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { TokenResponse } from "../types";
import { useTurnAlert } from "./useTurnAlert";

function token(status: TokenResponse["status"]) {
  return { displayNumber: "A-007", counterLabel: "Counter 2", status } as TokenResponse;
}

describe("useTurnAlert", () => {
  afterEach(() => vi.restoreAllMocks());

  it("alerts once when the token changes from waiting to called", () => {
    const vibrate = vi.fn();
    Object.defineProperty(navigator, "vibrate", { value: vibrate, configurable: true });

    const { rerender } = renderHook(({ current }) => useTurnAlert(current), {
      initialProps: { current: token("WAITING") },
    });
    expect(vibrate).not.toHaveBeenCalled();

    rerender({ current: token("CALLED") });
    rerender({ current: token("CALLED") });

    expect(vibrate).toHaveBeenCalledOnce();
    expect(document.title).toBe("Your turn! A-007");
  });
});
