# xx memory Skill

A standalone, self-contained AI skill that lets any AI agent generate flashcard files importable by the **xx memory (通用智能记忆助手)** Android app.

This directory is **not** part of the Android app itself — it is a portable skill definition that an AI reads to learn how to produce correctly-formatted memory card files.

## Files

- `xx-memory-skill.json` — the complete skill definition. Read this to learn the supported card types, card fields, output formats, quality guidelines, and examples.
- `README.md` — this file.

## What it enables

An AI that reads `xx-memory-skill.json` can generate flashcard files in four formats that the xx memory app can import:

| Format | Extension | Best for |
|--------|-----------|----------|
| JSON   | `.json`   | Default, lossless, easiest to validate |
| CSV    | `.csv`    | Spreadsheet workflows |
| TXT    | `.txt`    | Human-friendly, diff-friendly |
| Markdown | `.md`   | Readable notes with rich formatting |

Supported card types: `qa` (问答), `fill_blank` (填空), `code` (代码), `image` (图片), `audio` (音频).

Card fields: `question`, `answer`, `subject`, `detail`, `cardType`, `tags`, `imageUrl`, `audioUrl`, `isFavorite`.

## How to use

1. Point an AI agent at `xx-memory-skill.json` (paste the file, attach it, or reference its path).
2. Ask the AI to create flashcards from your notes / textbook / code / media — optionally specifying a target format.
3. The AI will produce a file in one of the supported formats following the rules and quality guidelines defined in the skill.
4. Import the generated file into the xx memory app via its import flow.

If no format is specified, the AI defaults to **JSON**.

## Validation

Before delivering a file, the AI should run through the `validationChecklist` in the skill JSON to ensure every card has the required fields, the correct `cardType`, valid `imageUrl` / `audioUrl` where required, no duplicates, and UTF-8 encoding without BOM.

## Notes

- This skill is format-only. It does not interact with the xx memory app's database or scheduling engine.
- When in doubt about field semantics, prefer the JSON format — it is lossless and unambiguous.
- To convert between formats, parse using the source format's rules and re-render using the target format's rules without altering card content.
