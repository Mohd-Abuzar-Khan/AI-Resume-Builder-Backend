# Resume Database Storage (Brief)

This document summarizes how resume data is stored and managed in the database by the resume-service.

## Where data lives

- Database: MySQL
- Service: resume-service (port 9093)
- Tables:
  - `resumes`
  - `resume_sections`

Schema is managed by Hibernate with `ddl-auto: update`.

## Core tables and fields

### `resumes`

Stores resume metadata and ownership.

- `resume_id` (PK)
- `user_id` (owner)
- `title`, `target_job_title`
- `template_id` (references template-service template)
- `ats_score`
- `status` (DRAFT, COMPLETE, PUBLISHED)
- `language` (default "en")
- `is_public`, `view_count`
- `owner_name`, `owner_avatar` (public gallery metadata)
- `created_at`, `updated_at`

### `resume_sections`

Stores the content blocks that make up a resume.

- `section_id` (PK)
- `resume_id` (FK to `resumes`)
- `section_type` (PERSONAL_INFO, SUMMARY, EXPERIENCE, etc.)
- `title`
- `content` (JSON string; schema depends on section type)
- `display_order` (render order)
- `is_visible`
- `ai_generated`
- `created_at`

## Relationships

- One resume has many sections.
- Sections are ordered by `display_order` and loaded in that order.
- Deleting a resume cascades to its sections (orphan removal).

## Write flow (create/update)

1. Client requests `POST /api/v1/resumes`.
2. Service enforces plan quota (FREE users max 3 resumes).
3. A `resumes` row is created with metadata and template reference.
4. Sections are added separately via section endpoints, each creating a `resume_sections` row.

Updates:
- Resume metadata updates overwrite fields on the `resumes` row.
- Section updates overwrite `title` and `content` on the `resume_sections` row.
- Reordering updates `display_order` for each section.
- Visibility toggles update `is_visible`.

## Read flow (ownership and visibility)

- Owners can read all of their resumes.
- Public resumes can be read by other users.
- Viewing a public resume increments `view_count` (non-atomic).
- Gallery queries filter by `is_public` and can search by title/owner/target job title.

## Duplicate flow

- A resume can be duplicated.
- The copy creates a new `resumes` row and clones all `resume_sections` rows.
- Public resumes can be duplicated by other users; private resumes cannot.

## Notes and constraints

- Section `content` is stored as raw JSON string without server-side schema validation.
- View count increments are not atomic under high concurrency.
- Template link is stored as `template_id` (no FK to template-service DB).
