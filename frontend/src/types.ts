export type TokenStatus =
  | "WAITING"
  | "CALLED"
  | "SERVING"
  | "COMPLETED"
  | "SKIPPED"
  | "CANCELLED";

export interface QueueSummary {
  id: number;
  code: string;
  name: string;
  location: string;
  open: boolean;
  waitingCount: number;
  estimatedWaitMinutes: number;
}

export interface QueueTokenView {
  id: number;
  displayNumber: string;
  status: TokenStatus;
  claimedBy: string | null;
  joinedAt: string;
  updatedAt: string;
}

export interface QueueSnapshot {
  queueId: number;
  queueName: string;
  location: string;
  open: boolean;
  waitingCount: number;
  estimatedWaitMinutes: number;
  generatedAt: string;
  tokens: QueueTokenView[];
}

export interface TokenResponse {
  id: number;
  publicId: string;
  queueId: number;
  queueName: string;
  displayNumber: string;
  customerName: string;
  status: TokenStatus;
  peopleAhead: number;
  estimatedWaitMinutes: number;
  claimedBy: string | null;
  joinedAt: string;
  updatedAt: string;
}

export interface AuthUser {
  username: string;
  displayName: string;
}

export interface HistoryItem {
  id: number;
  displayNumber: string;
  customerName: string;
  status: TokenStatus;
  claimedBy: string | null;
  joinedAt: string;
  calledAt: string | null;
  servingAt: string | null;
  completedAt: string | null;
}

export interface HistoryPage {
  content: HistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
