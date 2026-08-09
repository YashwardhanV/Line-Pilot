import { useEffect, useState } from "react";
import { apiRequest, eventUrl } from "../api";
import type { QueueSnapshot } from "../types";

export function useQueueEvents(queueId: number | null) {
  const [snapshot, setSnapshot] = useState<QueueSnapshot | null>(null);
  const [connectionState, setConnectionState] = useState<"idle" | "live" | "reconnecting">("idle");

  useEffect(() => {
    if (queueId === null) {
      setSnapshot(null);
      setConnectionState("idle");
      return;
    }

    let active = true;
    apiRequest<QueueSnapshot>(`/queues/${queueId}`)
      .then((data) => {
        if (active) setSnapshot(data);
      })
      .catch(() => {
        if (active) setConnectionState("reconnecting");
      });

    const source = new EventSource(eventUrl(queueId));
    const handleUpdate = (event: MessageEvent<string>) => {
      if (!active) return;
      setSnapshot(JSON.parse(event.data) as QueueSnapshot);
      setConnectionState("live");
    };
    source.addEventListener("queue.updated", handleUpdate as EventListener);
    source.onopen = () => setConnectionState("live");
    source.onerror = () => setConnectionState("reconnecting");

    return () => {
      active = false;
      source.removeEventListener("queue.updated", handleUpdate as EventListener);
      source.close();
    };
  }, [queueId]);

  return { snapshot, connectionState };
}
