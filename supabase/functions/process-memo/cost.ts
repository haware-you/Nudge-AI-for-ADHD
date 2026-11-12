// Per-memo cost estimate (PRD §11 Week 1: "per-memo cost measured"; §7 COGS ≤ $0.15/user/month).
//
// Unit prices are deliberately NOT hard-coded: provider pricing changes, and a stale number in
// code would make the COGS KPI look verified when it isn't. They come from PRICE_* env vars
// (see supabase/.env.example). If any price is missing, the estimate is null — "unknown",
// never silently $0 — while the raw usage units are still stored so cost can be recomputed.

export interface MemoUsage {
  sttSeconds: number;
  llmInputTokens: number;
  llmOutputTokens: number;
  llmCalls: number;
  embeddingTokens: number;
}

export interface UnitPrices {
  sttUsdPerMinute: number | null;
  llmInputUsdPerMTok: number | null;
  llmOutputUsdPerMTok: number | null;
  embeddingUsdPerMTok: number | null;
}

export function emptyUsage(): MemoUsage {
  return { sttSeconds: 0, llmInputTokens: 0, llmOutputTokens: 0, llmCalls: 0, embeddingTokens: 0 };
}

export function estimateCostUsd(usage: MemoUsage, prices: UnitPrices): number | null {
  const {
    sttUsdPerMinute,
    llmInputUsdPerMTok,
    llmOutputUsdPerMTok,
    embeddingUsdPerMTok,
  } = prices;
  if (
    sttUsdPerMinute === null || llmInputUsdPerMTok === null ||
    llmOutputUsdPerMTok === null || embeddingUsdPerMTok === null
  ) {
    return null;
  }
  const cost = (usage.sttSeconds / 60) * sttUsdPerMinute +
    (usage.llmInputTokens / 1e6) * llmInputUsdPerMTok +
    (usage.llmOutputTokens / 1e6) * llmOutputUsdPerMTok +
    (usage.embeddingTokens / 1e6) * embeddingUsdPerMTok;
  // numeric(10, 6) in Postgres
  return Math.round(cost * 1e6) / 1e6;
}

/** Parses an optional non-negative price env var; null if unset/blank, throws if garbage. */
export function parsePrice(name: string, raw: string | undefined): number | null {
  if (raw === undefined || raw.trim() === "") return null;
  const value = Number(raw);
  if (!Number.isFinite(value) || value < 0) {
    throw new Error(`Invalid price env var ${name}=${raw}: expected a non-negative number`);
  }
  return value;
}
