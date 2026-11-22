import "dotenv/config";
import express from "express";
import cors from "cors";
import OpenAI from "openai";

const app = express();
app.use(cors());
app.use(express.json({ limit: "1mb" }));

app.get("/health", (_req, res) => res.json({ ok: true, service: "jash-ai-screen-assistant" }));

app.post("/api/practice/solve", async (req, res) => {
  const question = String(req.body?.question ?? "").trim();
  const choices = Array.isArray(req.body?.choices) ? req.body.choices.slice(0, 8) : [];
  if (!question || question.length > 12000) return res.status(400).json({ error: "Provide a question under 12,000 characters." });
  if (!process.env.OPENAI_API_KEY) return res.status(503).json({ error: "Backend is not configured with an AI key." });

  try {
    const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
    const response = await client.responses.create({
      model: process.env.OPENAI_MODEL || "gpt-4.1-mini",
      instructions: "You are a careful tutor for authorized practice and mock questions. Return JSON only with answer, confidence (0 to 1), and verification_note. If information is insufficient, set answer to 'UNCERTAIN'. Never claim certainty without support. Do not assist with active graded examinations.",
      input: JSON.stringify({ question, choices })
    });
    const raw = response.output_text || "";
    let parsed;
    try { parsed = JSON.parse(raw); } catch { parsed = { answer: "UNCERTAIN", confidence: 0, verification_note: "Could not parse a structured answer." }; }
    res.json(parsed);
  } catch (error) {
    console.error("solve error", error?.message || error);
    res.status(502).json({ error: "AI provider request failed." });
  }
});

const port = Number(process.env.PORT || 8787);
app.listen(port, "0.0.0.0", () => console.log(`Jash AI backend listening on ${port}`));
