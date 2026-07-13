export type Effort = "low" | "medium" | "high";

export interface ExtractedTask {
  text: string;
  effort: Effort;
  confidence: number; // 0..1 — below LOW_CONFIDENCE_THRESHOLD surfaces for one-tap review confirmation
  source_span: string; // verbatim snippet of the transcript this task came from (M5 traceability)
}

export interface ExtractionResult {
  tasks: ExtractedTask[];
  notes: string[];
}

export const LOW_CONFIDENCE_THRESHOLD = 0.6;
