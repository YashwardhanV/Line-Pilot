import { useEffect, useRef } from "react";
import type { TokenResponse } from "../types";

const BASE_TITLE = "LinePilot";

/** Ask once, while the customer is clicking "Get my token" (browsers only allow it on a click). */
export function requestTurnAlertPermission() {
  if ("Notification" in window && Notification.permission === "default") {
    void Notification.requestPermission();
  }
}

/**
 * Buzzes, shows a system notification and changes the tab title when the customer's token is
 * called, in case they are looking at another tab or app.
 */
export function useTurnAlert(token: TokenResponse | null) {
  const previousStatus = useRef(token?.status);
  const status = token?.status;
  const displayNumber = token?.displayNumber;
  const counter = token?.counterLabel ?? "the desk";

  useEffect(() => {
    const justCalled = status === "CALLED" && previousStatus.current !== "CALLED";
    previousStatus.current = status;
    if (!justCalled) {
      if (status !== "CALLED") document.title = BASE_TITLE;
      return;
    }

    const message = `Token ${displayNumber}: please go to ${counter} now.`;
    document.title = `Your turn! ${displayNumber}`;
    navigator.vibrate?.([300, 150, 300]);
    if ("Notification" in window && Notification.permission === "granted") {
      new Notification("It's your turn", { body: message });
    }
  }, [status, displayNumber, counter]);
}
