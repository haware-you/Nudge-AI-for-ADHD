import { z } from "npm:zod@3";
import { config } from "../_shared/config.ts";
import { ExtractionResult } from "./types.ts";

// Versioned per PRD §12 ("prompt versions tagged in repo alongside golden-set eval results").
// Bump this whenever the prompt text changes and re-run the golden-set eval (docs/prd.md §9/§11).
export const EXTRACTION_PROMPT_VERSION = "v1";

const SYSTEM_PROMPT = `You extract actionable tasks from a spoken voice memo transcript for an ADHD \
task-capture app. The transcript may mix English and Bahasa Indonesia in the same memo.

Rules, in priority order:
1. Never invent or infer a task the speaker did not actually say. If the memo is venting, \
journaling, or has no concrete action, put it in "notes" instead of "tasks" — an empty \
"tasks" array is a correct and common output.
2. If an item is ambiguous (e.g. "deal with the Sarah thing"), keep it VERBATIM as a task text \
— do not rewrite, clarify, or expand it — and set confidence low (below 0.6).
3. Every task must include "source_span": the verbatim substring of the transcript it came from.
4. Tag each task's effort as "low" (a two-minute action), "medium" (a focused single sitting), \
or "high" (multi-step or requires a block of time).
5. Split compound rambles into separate discrete tasks rather than one run-on task.
6. Output only via the record_extraction tool call. Do not add commentary.`;

const extractedTaskSchema = z.object({
  text: z.string().min(1),
  effort: z.enum(["low", "medium", "high"]),
  confidence: z.number().min(0).max(1),
  source_span: z.string().min(1),
});

const extractionResultSchema = z.object({
  tasks: z.array(extractedTaskSchema),
  notes: z.array(z.string()),
});

const EXTRACTION_TOOL = {
  name: "record_extraction",
  description: "Record the tasks and notes extracted from the memo transcript.",
  input_schema: {
    type: "object",
    properties: {
      tasks: {
        type: "array",
        items: {
          type: "object",
          properties: {
            text: { type: "string" },
            effort: { type: "string", enum: ["low", "medium", "high"] },
            confidence: { type: "number" },
            source_span: { type: "string" },
          },
          required: ["text", "effort", "confidence", "source_span"],
        },
      },
      notes: { type: "array", items: { type: "string" } },
    },
    required: ["tasks", "notes"],
  },
};

async function callClaude(transcript: string, correctionNote?: string): Promise<unknown> {
  const userContent = correctionNote
    ? `${correctionNote}\n\nTranscript:\n${transcript}`
    : `Transcript:\n${transcript}`;

  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": config.anthropicApiKey,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify({
      model: config.extractionModel,
      max_tokens: 1024,
      system: SYSTEM_PROMPT,
      tools: [EXTRACTION_TOOL],
      tool_choice: { type: "tool", name: EXTRACTION_TOOL.name },
      messages: [{ role: "user", content: userContent }],
    }),
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(`Claude extraction request failed (${response.status}): ${body}`);
  }

  const data = await response.json();
  const toolUse = data.content?.find((block: { type: string }) => block.type === "tool_use");
  return toolUse?.input;
}

/**
 * Returns null (never throws for malformed model output) after one retry, so the caller can
 * fail safe to the raw transcript rather than fabricate or crash — per CLAUDE.md: "malformed
 * output -> one retry -> fail safe to raw transcript."
 */
export async function extractTasks(transcript: string): Promise<ExtractionResult | null> {
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const raw = await callClaude(
        transcript,
        attempt === 0
          ? undefined
          : "Your previous response did not match the required schema exactly. " +
              "Call record_extraction again with valid tasks/notes fields only.",
      );
      const parsed = extractionResultSchema.safeParse(raw);
      if (parsed.success) {
        return parsed.data;
      }
    } catch (_err) {
      // fall through to retry / fail-safe
    }
  }
  return null;
}
