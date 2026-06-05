# AGENTS.md

## Required Git Workflow

These rules are mandatory for every coding agent working in this repository.

Before every commit:

1. Run formatting for the touched stack.
2. Run Ruff.
3. Run relevant tests.
4. Check `git status`.
5. Check `git diff --staged`.
6. Confirm no secrets, credentials, `.env` files, local databases, caches, or generated junk files are staged.
7. Confirm the commit is atomic and represents one logical change.

Every commit message must use this exact structure:

```text
<gitmoji> <imperative summary>

Intent:
Explain why this change exists.

Changes:
- List the important changes.

Validation:
- List commands actually run.
```

Examples:

```text
✨ Add Subaru MPI draft workflow

Intent:
Create the first usable technician-facing form flow for the WP0 demo.

Changes:
- Add API routes for creating and updating MPI drafts.
- Add static frontend controls for the Subaru inspection form.
- Add tests for draft creation and submission validation.

Validation:
- ./.venv/bin/ruff check .
- ./.venv/bin/pytest
```

Do not create a commit that skips the gitmoji/body format unless the user explicitly says to ignore this rule.

If validation cannot be run, say that clearly in the commit message `Validation:` section and in the final handoff.

---

## Project goals

This repo is for Hank MPI: a production-style FastAPI backend for an AI-assisted dealership multi-point inspection workflow.

Prioritize:

- correctness
- readability
- clear architecture
- beginner-friendly intent comments
- tests
- small atomic changes
- production habits

The system should remain understandable to humans. Coding agents may write code quickly, but the architecture must stay simple, explicit, and easy to review.

The product goal is:

```text
Technician captures inspection evidence
  → backend stores and processes media
  → AI suggests structured MPI findings
  → technician approves facts
  → advisor edits customer-facing story
  → customer receives a clear report/video/page
```

Do not optimize only for a flashy demo. Build the smallest reliable version of the workflow.

---

## Stack assumptions

Primary backend stack:

- Python
- FastAPI
- Ruff
- Pytest
- PostgreSQL
- SQLAlchemy / SQLModel if present
- Alembic if migrations are configured
- Background workers if media/AI processing is involved

Use existing project tooling whenever available.

If `uv.lock` exists, use `uv`.

Examples:

```bash
uv sync
uv run pytest
uv run ruff check .
uv run ruff format .
```

If no `uv.lock` exists, use the existing project setup: `pip`, `poetry`, `hatch`, or Makefile targets.

Prefer Makefile commands when present.

---

## Core engineering principles

Make the smallest correct change.

Follow existing project patterns before introducing new ones.

Avoid speculative abstractions.

Do not add new frameworks, libraries, queues, ORMs, or infrastructure tools unless clearly necessary.

Keep these concerns separate when practical:

```text
API routes
request/response schemas
business logic
database models
database queries
background jobs
external provider integrations
tests
```

Do not hide important behavior inside clever helpers.

Readable and boring code is preferred.

---

## Documentation and planning

Markdown implementation plans are temporary working documents.

Use plan documents while a feature branch is active to coordinate
scope, sequencing, open questions, and validation.

Once the feature has been fully implemented and merged back into
`main`, remove the feature plan document from the repository.

Before removing a completed plan, record the durable conclusions in
`docs/decision-log.md`:

- decisions that shaped the implementation
- meaningful deviations from the original plan
- validation that was actually run
- unresolved follow-up work
- context a future contributor needs to understand why the feature
  works the way it does

Do not leave stale implementation plans in `docs/` as if they are
current product or architecture direction. If a plan has become
durable architecture documentation, rename and rewrite it as an
architecture note instead of keeping it as a plan.

---

## Commenting expectations

Comments are important in this repo.

Write comments that explain **intent**, not obvious syntax.

Good comments explain:

- why this function exists
- what business rule it protects
- what assumption is being made
- what could go wrong
- why a choice is intentionally simple
- how a non-technical founder should think about the concept

Avoid comments that only restate the code.

Bad:

```python
# Set status to processed
asset.status = AssetStatus.PROCESSED
```

Good:

```python
# Mark the asset as processed only after all derived files are available.
# The customer page and AI pipeline should never use a half-processed upload.
asset.status = AssetStatus.PROCESSED
```

Use beginner-friendly explanations around domain concepts.

Examples of concepts that deserve comments:

- tenant isolation
- public share links
- presigned uploads
- background job retries
- AI suggestions vs. approved findings
- technician approval
- advisor approval
- media processing state
- idempotency
- why raw video is not the source of truth

Do not over-comment simple Python syntax.

The goal is not noisy code. The goal is code that teaches the system’s intent.

---

## Python style

Use type hints for new public functions.

Prefer explicit return types.

Prefer simple functions.

Use Pydantic models for API schemas when appropriate.

Avoid mutable default arguments.

Avoid unnecessary classes.

Use classes when they clearly model state, behavior, or framework structure.

Keep I/O separate from business logic when practical.

Prefer explicit error handling over hidden magic.

Do not silently swallow exceptions.

Use structured logging where relevant.

Avoid broad `except Exception` unless there is a clear reason and the exception is logged or re-raised appropriately.

---

## FastAPI expectations

Keep route handlers thin.

Route handlers should usually:

```text
validate request
check auth/permissions
call service/use-case function
return response schema
```

Business logic should not live entirely inside route files.

Prefer clear module boundaries.

Example module structure:

```text
app/
  modules/
    inspections/
      models.py
      schemas.py
      routes.py
      service.py
      repository.py
      tests/
```

Only introduce repositories/services if they make the code clearer. Do not create empty abstraction layers.

---

## Domain rules

AI output is never the source of truth.

AI output should be treated as a suggestion until reviewed by a human.

Customer-visible data should come only from approved fields.

Technician approval controls factual inspection claims.

Advisor approval controls customer-facing language.

Raw video is evidence, not the final report.

The structured inspection claim is the backbone of the system.

Important rule:

```text
Do not let AI-generated content silently overwrite human-approved fields.
```

Important rule:

```text
Never allow one dealership to access another dealership’s data.
```

Important rule:

```text
Public customer links must expose only approved customer-facing data.
```

---

## Security

Do not commit secrets, credentials, tokens, API keys, `.env` files, or private certificates.

Do not disable auth, permissions, validation, TLS, or security checks to make tests pass.

Do not log sensitive customer data.

Be careful with:

- customer names
- phone numbers
- emails
- VINs
- license plates
- repair order numbers
- public share tokens
- raw media URLs
- AI provider keys

Public share tokens should be high entropy.

Store token hashes if the project uses that pattern.

Media files should not be exposed publicly by default.

Use signed URLs or controlled access patterns when implemented.

---

## Testing expectations

Add tests for new behavior.

Add regression tests for bug fixes.

At minimum, test:

- successful path
- important failure path
- permission/tenant isolation when relevant

Important tests for this product include:

- users cannot access another dealership’s inspections
- users cannot access another dealership’s media
- expired public links do not work
- customer links expose only approved data
- AI suggestions do not overwrite human-approved fields
- failed jobs can be marked failed or retried
- uploaded assets are attached to the correct inspection

Never say tests passed unless they were actually run.

If tests were not run, say exactly that and explain why.

---

## Validation before finishing changes

Before finishing a task, run the relevant checks.

Preferred commands when available:

```bash
uv run ruff format .
uv run ruff check .
uv run pytest
```

If using plain Python tooling:

```bash
ruff format .
ruff check .
pytest
```

If the repo has Makefile targets, prefer them:

```bash
make format
make lint
make test
```

Ruff and tests should pass before committing.

Do not commit code with known lint or test failures unless explicitly instructed, and clearly document the failure.

---

## Database and migrations

If a schema/model changes, include the corresponding migration when the project uses migrations.

Do not edit old migrations unless the project is still pre-release and the team has explicitly chosen migration rewriting.

Migration names should be descriptive.

Examples:

```text
add_inspection_claims
add_evidence_assets
add_public_share_links
```

Be careful with destructive migrations.

Do not drop columns, delete data, or change identifiers without explicit confirmation.

---

## Background jobs

Use background jobs for slow or unreliable work.

Examples:

- video transcoding
- thumbnail generation
- audio extraction
- AI extraction
- translation
- PDF generation
- video rendering

Do not perform heavy media or AI work inside a normal API request.

Jobs should expose clear statuses.

Example statuses:

```text
pending
processing
completed
failed
retrying
```

Where possible, jobs should be idempotent.

A retried job should not duplicate records or corrupt state.

---

## AI integration rules

AI providers should be wrapped behind clear provider/service interfaces.

Do not scatter raw AI API calls throughout the codebase.

Store enough metadata to debug AI behavior:

- provider
- model
- prompt version
- input IDs
- output
- status
- error message if failed

AI output should be validated before being saved.

Prefer structured JSON outputs.

Do not trust AI output blindly.

If AI output is malformed, fail safely and make the failure visible.

---

## Media handling rules

Large videos should not be uploaded through normal JSON requests.

Use object storage / presigned upload flow when implemented.

Every media file should have a database record.

Every media file should belong to an inspection or another clear parent object.

Track upload and processing status.

Do not assume uploaded files are valid.

Do not assume video processing will succeed.

Do not expose raw storage URLs publicly unless the project explicitly uses public assets.

---

## Git workflow

Use atomic commits.

One commit should represent one logical change.

Do not mix unrelated changes.

Examples of good atomic commits:

```text
✨ Add EvidenceAsset model and migration
✅ Add tenant isolation tests for inspections
🐛 Fix public link expiry check
♻️ Refactor media processing status transitions
📝 Document AI suggestion approval rules
```

Examples of bad commits:

```text
misc changes
fix stuff
update backend
big refactor and add upload and fix tests
```

Before committing:

1. Format code.
2. Run Ruff.
3. Run relevant tests.
4. Check the diff.
5. Make sure no secrets are included.
6. Make sure the commit is atomic.

---

## Commit message format

Every commit message must start with the correct gitmoji.

Use gitmoji meanings from gitmoji.dev.

Format:

```text
<gitmoji> <imperative summary>

Intent:
Explain why this change exists.

Changes:
- List the important changes.

Validation:
- List commands actually run.
```

Example:

```text
✨ Add evidence asset upload model

Intent:
Create the backend source of truth for uploaded inspection media before adding
processing or AI analysis.

Changes:
- Add EvidenceAsset SQLAlchemy model.
- Add upload and processing status enums.
- Add Alembic migration.
- Add basic asset creation tests.

Validation:
- uv run ruff format .
- uv run ruff check .
- uv run pytest
```

Use concise but specific commit subjects.

Use imperative mood:

```text
Add evidence asset model
Fix tenant isolation check
Document public link lifecycle
```

Not:

```text
Added evidence asset model
Fixes tenant isolation
Documentation updates
```

---

## Common gitmoji choices

Use the most accurate gitmoji for the change.

Common examples:

```text
✨ New feature
🐛 Bug fix
✅ Add or update tests
📝 Documentation
♻️ Refactor
🚨 Fix lint/compiler warnings
🔒️ Security fix
⚡️ Performance improvement
🔥 Remove code or files
💄 UI/style changes
🚚 Move or rename files
🔧 Configuration changes
👷 CI/build workflow changes
🗃️ Database-related changes
🚑️ Critical hotfix
🚀 Deployment-related change
```

If unsure, check gitmoji.dev and choose the closest match.

Do not use random emojis.

---

## Pull request / handoff summary

When finishing work, summarize:

```text
What changed
Why it changed
Files/modules touched
Tests run
Any skipped tests
Known risks
Follow-up work
```

Be honest.

Do not claim something is production-ready unless it has been tested in production-like conditions.

Do not claim a workflow is complete if parts are mocked.

Use these labels when helpful:

```text
Mocked
Functional
Pilot-ready
```

---

## Communication norms

Be explicit about assumptions.

Call out uncertainty.

Call out tradeoffs.

Prefer clear explanations over cleverness.

If a task reveals a larger architectural issue, mention it before expanding scope.

Do not silently rewrite unrelated parts of the system.

Do not introduce major new patterns without explaining the intent.

---

## Definition of done

A task is done when:

- the requested behavior is implemented
- code is readable
- intent comments are included where useful
- relevant tests are added or updated
- Ruff passes
- tests pass
- no secrets are committed
- commit is atomic
- commit message starts with the correct gitmoji
- skipped validation is clearly disclosed

Final rule:

```text
Move fast, but keep the system understandable.
```
