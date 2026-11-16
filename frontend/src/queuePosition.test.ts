import { describe, expect, it } from "vitest";
import { estimateMinutes, withLivePosition } from "./queuePosition";
import type { QueueSnapshot, QueueTokenView, TokenResponse, TokenStatus } from "./types";

function entry(id: number, status: TokenStatus): QueueTokenView {
  return { id, displayNumber: `A-00${id}`, status, claimedBy: status === "WAITING" ? null : "Anika", counterLabel: null, joinedAt: "", updatedAt: "" };
}

function snapshot(tokens: QueueTokenView[], activeStaff = 1): QueueSnapshot {
  return {
    queueId: 1, queueName: "Desk", location: "Hall", open: true, waitingCount: 0, estimatedWaitMinutes: 0,
    averageServiceMinutes: 6, activeStaff, generatedAt: "", tokens,
  };
}

const mine = { id: 4, status: "WAITING", peopleAhead: 9, estimatedWaitMinutes: 99 } as TokenResponse;

describe("withLivePosition", () => {
  it("counts people being served plus earlier waiting tokens as ahead", () => {
    const board = snapshot([entry(1, "SERVING"), entry(2, "CALLED"), entry(3, "WAITING"), entry(4, "WAITING"), entry(5, "WAITING")], 2);

    const live = withLivePosition(mine, board);

    expect(live?.peopleAhead).toBe(3);
    expect(live?.estimatedWaitMinutes).toBe(12);
  });

  it("picks up a status change from the board", () => {
    expect(withLivePosition(mine, snapshot([entry(4, "CALLED")]))?.status).toBe("CALLED");
  });

  it("returns null once the token has left the board", () => {
    expect(withLivePosition(mine, snapshot([entry(1, "WAITING")]))).toBeNull();
  });
});

describe("estimateMinutes", () => {
  it("is zero when nobody is ahead and treats zero staff as one", () => {
    expect(estimateMinutes(0, 6, 1)).toBe(0);
    expect(estimateMinutes(2, 5, 0)).toBe(10);
  });
});
