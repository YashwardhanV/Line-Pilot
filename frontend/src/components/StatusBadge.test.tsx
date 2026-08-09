import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBadge } from "./StatusBadge";

describe("StatusBadge", () => {
  it("renders the explicit queue status", () => {
    render(<StatusBadge status="SERVING" />);
    expect(screen.getByText("SERVING")).toBeInTheDocument();
  });
});
