import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { TokenResponse } from "../types";
import { TicketCard } from "./TicketCard";

function token(status: TokenResponse["status"]): TokenResponse {
  return {
    id: 1,
    publicId: "4f1c2a8e-0000-4000-8000-000000000001",
    queueId: 1,
    queueName: "Citizen Service Desk",
    displayNumber: "A-007",
    customerName: "Asha",
    status,
    peopleAhead: 2,
    estimatedWaitMinutes: 12,
    claimedBy: null,
    joinedAt: "2026-08-07T08:00:00Z",
    updatedAt: "2026-08-07T08:00:00Z",
  };
}

describe("TicketCard", () => {
  it("lets a waiting customer leave the queue", () => {
    const onCancel = vi.fn();
    render(<TicketCard token={token("WAITING")} onCancel={onCancel} cancelling={false} />);

    expect(screen.getByText("A-007")).toBeInTheDocument();
    expect(screen.getByText("12 minutes · 2 ahead")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Leave this queue" }));
    expect(onCancel).toHaveBeenCalledOnce();
  });

  it("hides the cancel button once the token has been called", () => {
    render(<TicketCard token={token("CALLED")} onCancel={() => undefined} cancelling={false} />);

    expect(screen.queryByRole("button", { name: "Leave this queue" })).not.toBeInTheDocument();
  });
});
