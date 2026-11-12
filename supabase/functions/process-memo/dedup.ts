import { config } from "../_shared/config.ts";

export async function embedText(
  text: string,
): Promise<{ embedding: number[]; tokens: number }> {
  const response = await fetch("https://api.voyageai.com/v1/embeddings", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      Authorization: `Bearer ${config.embeddingsApiKey}`,
    },
    body: JSON.stringify({ input: [text], model: config.embeddingsModel }),
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(`Embeddings request failed (${response.status}): ${body}`);
  }

  const data = await response.json();
  return {
    embedding: data.data[0].embedding as number[],
    // Voyage reports usage.total_tokens; treated as 0 if a provider omits it.
    tokens: Number(data.usage?.total_tokens ?? 0),
  };
}

function cosineSimilarity(a: number[], b: number[]): number {
  let dot = 0;
  let normA = 0;
  let normB = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    normA += a[i] * a[i];
    normB += b[i] * b[i];
  }
  if (normA === 0 || normB === 0) return 0;
  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

export interface OpenTaskEmbedding {
  id: string;
  embedding: number[];
}

/**
 * Threshold-merge against the user's open tasks (PRD §7 dedup step). Returns the best match
 * above the configured similarity threshold, or null if this is a genuinely new task.
 */
export function findDuplicate(
  candidateEmbedding: number[],
  openTasks: OpenTaskEmbedding[],
): { taskId: string; similarity: number } | null {
  let best: { taskId: string; similarity: number } | null = null;
  for (const task of openTasks) {
    const similarity = cosineSimilarity(candidateEmbedding, task.embedding);
    if (similarity >= config.dedupSimilarityThreshold && (!best || similarity > best.similarity)) {
      best = { taskId: task.id, similarity };
    }
  }
  return best;
}
