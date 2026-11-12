import { assertEquals, assertThrows } from "jsr:@std/assert@1";
import { emptyUsage, estimateCostUsd, parsePrice } from "./cost.ts";

// Prices here are arbitrary round test numbers, NOT real provider prices.
const prices = {
  sttUsdPerMinute: 0.01,
  llmInputUsdPerMTok: 1,
  llmOutputUsdPerMTok: 5,
  embeddingUsdPerMTok: 0.02,
};

Deno.test("sums every component", () => {
  const usage = {
    sttSeconds: 60,
    llmInputTokens: 1_000_000,
    llmOutputTokens: 100_000,
    llmCalls: 1,
    embeddingTokens: 1_000_000,
  };
  // 0.01 + 1 + 0.5 + 0.02
  assertEquals(estimateCostUsd(usage, prices), 1.53);
});

Deno.test("missing any price yields null, never a silent zero", () => {
  assertEquals(estimateCostUsd(emptyUsage(), { ...prices, sttUsdPerMinute: null }), null);
  assertEquals(estimateCostUsd(emptyUsage(), { ...prices, embeddingUsdPerMTok: null }), null);
});

Deno.test("rounds to numeric(10,6) precision", () => {
  const usage = { ...emptyUsage(), llmInputTokens: 1 };
  assertEquals(estimateCostUsd(usage, prices), 0.000001);
});

Deno.test("parsePrice", () => {
  assertEquals(parsePrice("X", undefined), null);
  assertEquals(parsePrice("X", "  "), null);
  assertEquals(parsePrice("X", "0.25"), 0.25);
  assertThrows(() => parsePrice("X", "abc"));
  assertThrows(() => parsePrice("X", "-1"));
});
