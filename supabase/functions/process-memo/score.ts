import { Effort } from "./types.ts";

// Deterministic heuristic only — NOT an LLM call, per PRD §7 ("keeps the widget's top pick
// cheap, fast, and explainable") and the COGS guardrail in CLAUDE.md. Every constant here is
// named so the ranking is explainable and tunable without touching the pipeline shape.

const EFFORT_WEIGHT: Record<Effort, number> = {
  // Lower effort scores higher: the product's paralysis hook is handing back a *small* next
  // step, not necessarily the most important one.
  low: 1.0,
  medium: 0.6,
  high: 0.3,
};

const RECENCY_HALF_LIFE_HOURS = 72;
const SKIP_PENALTY_PER_SKIP = 0.15;
const SKIP_PENALTY_CAP = 0.6; // a skipped task is deprioritized, never effectively buried forever

const EFFORT_COMPONENT_WEIGHT = 0.5;
const RECENCY_COMPONENT_WEIGHT = 0.3;
const SKIP_COMPONENT_WEIGHT = 0.2;

export interface ScorableTask {
  effort: Effort;
  updatedAt: Date;
  skipCount: number;
}

export function computePriorityScore(task: ScorableTask, now: Date = new Date()): number {
  const ageHours = Math.max(0, (now.getTime() - task.updatedAt.getTime()) / 36e5);
  const recencyWeight = Math.exp(-ageHours / RECENCY_HALF_LIFE_HOURS);
  const skipPenalty = Math.min(task.skipCount * SKIP_PENALTY_PER_SKIP, SKIP_PENALTY_CAP);

  return (
    EFFORT_COMPONENT_WEIGHT * EFFORT_WEIGHT[task.effort] +
    RECENCY_COMPONENT_WEIGHT * recencyWeight -
    SKIP_COMPONENT_WEIGHT * skipPenalty
  );
}
