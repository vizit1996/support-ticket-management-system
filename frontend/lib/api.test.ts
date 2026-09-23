import { describe, expect, it } from "vitest";
import { TicketStatus, transitions } from "./api";

describe("ticket status controls", () => {
  it.each<[TicketStatus, TicketStatus]>([
    ["OPEN", "IN_PROGRESS"],
    ["OPEN", "CANCELLED"],
    ["IN_PROGRESS", "RESOLVED"],
    ["IN_PROGRESS", "CANCELLED"],
    ["RESOLVED", "CLOSED"],
  ])("allows %s → %s", (from, to) => {
    expect(transitions[from]).toContain(to);
  });

  it.each<TicketStatus>(["CLOSED", "CANCELLED"])("shows no controls for terminal status %s", (status) => {
    expect(transitions[status]).toEqual([]);
  });

  it("does not expose reverse or skip-ahead actions", () => {
    expect(transitions.OPEN).not.toContain("RESOLVED");
    expect(transitions.RESOLVED).not.toContain("OPEN");
    expect(transitions.CLOSED).not.toContain("OPEN");
    expect(transitions.CANCELLED).not.toContain("OPEN");
  });
});
