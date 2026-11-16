import type { QueueSnapshot, TokenResponse, TokenStatus } from "./types";

export function isActive(status: TokenStatus) {
  return status === "WAITING" || status === "CALLED" || status === "SERVING";
}

// Same rule as the backend's WaitTimeEstimator: people ahead are shared between active staff.
export function estimateMinutes(peopleAhead: number, averageServiceMinutes: number, activeStaff: number) {
  if (peopleAhead <= 0) return 0;
  return Math.ceil(peopleAhead / Math.max(1, activeStaff)) * averageServiceMinutes;
}

/**
 * Updates a customer's token from the live board, so the browser does not have to ask the
 * server again on every queue change. Returns null when the token is no longer on the board
 * (it was completed, skipped, cancelled or expired).
 */
export function withLivePosition(token: TokenResponse, snapshot: QueueSnapshot): TokenResponse | null {
  const index = snapshot.tokens.findIndex((entry) => entry.id === token.id);
  if (index === -1) return null;

  const entry = snapshot.tokens[index];
  // The board lists active tokens in join order. Everyone being called or served counts as
  // ahead, plus everyone who joined earlier and is still waiting.
  const peopleAhead = entry.status === "WAITING"
    ? snapshot.tokens.filter((other, otherIndex) => other.status !== "WAITING" || otherIndex < index).length
    : 0;

  return {
    ...token,
    status: entry.status,
    claimedBy: entry.claimedBy,
    updatedAt: entry.updatedAt,
    peopleAhead,
    estimatedWaitMinutes: estimateMinutes(peopleAhead, snapshot.averageServiceMinutes, snapshot.activeStaff),
  };
}
