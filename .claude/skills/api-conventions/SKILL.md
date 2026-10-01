---
name: api-conventions
description: REST API conventions for the AgroTrends backend - URL rules, the HttpResponse envelope, status codes, error bodies, pagination, DTO and validation rules, OpenAPI annotations, date formats, the lang parameter. Use whenever adding or changing a controller, a request or response DTO, or an exception, so the contract stays stable for the frontend (AGROTRENDS- FrontEnd).
---

# API conventions (AgroTrends)

The React frontend builds against this contract. Do not break a field name, path or status code
without recording it in `docs/DEV_LOG.md` under "Breaking API changes" in the same PR.

## URLs

- Base path `/api`, plural nouns, kebab-case: `/api/blogs`, `/api/questions`, `/api/auth/refresh-token`.
- Identity in the path, filters in the query string. Actions that aren't CRUD are a POST sub-resource:
  `POST /api/auth/forgot-password`, `POST /api/user/change-password`.
- Existing routes keep their shape (`/api/blogs/id/{blogId}`, `/api/blogs/all`, `.../delete`); new
  routes follow the rule above.
- The caller's id never appears in a URL, query or body - "my things" is `/me`-style or implied by the token.

## Envelope

Every endpoint returns `HttpResponse`:

```json
{ "status": "OK", "message": "Data loaded successfully.", "payload": {}, "success": true }
```

- `message` is safe to display. Never put exception detail in it.
- `payload` is `null` on failure, except a 500 which carries `{ "errorId": "<uuid>" }` so a bug report
  maps to a log line.
- One envelope shape. Do not invent another.

## Status codes

| Code | Use |
|---|---|
| 200 / 201 | read-update / created |
| 400 | malformed request, validation failure, wrong current password |
| 401 | missing, invalid, expired or signed-out token; wrong credentials (always the same message) |
| 403 | authenticated but not allowed (not the owner, missing privilege, password change pending) |
| 404 | not found - and for **private** data that isn't yours (see security skill) |
| 409 | duplicate or illegal state (duplicate e-mail, already published) |
| 429 | rate limit / lockout (sign-in, forgot-password, change-password) |
| 500 | unexpected - generic message + `errorId`, never detail |

## Pagination - every list endpoint

- Params `pageNo` (0-based), `pageSize` (default 20, **clamp to max 100**), `sortBy`, `ascOrDesc`
  (the existing names - keep them).
- Return Spring's `Page` shape in `payload`.
- **Real database pagination** (`Page<X> findAllByY(Y y, Pageable p)`). Never load a full list and wrap
  it in `PageImpl` (`BlogService.getAllByCategory/Author` do this today - Phase 2 fixes it).
- `sortBy` is validated against a per-entity allowlist; an unknown field is a 400, not a 500.
  The default sort field is `creationDate`.

## DTOs and validation

- Request DTOs: Lombok `@Getter @Setter`, Bean Validation on every field, `@Valid` on every `@RequestBody`.
- Messages come from `ErrorCode` constants: `@NotBlank(message = ErrorCode.ERROR_TITLE_IS_REQUIRED)`.
- Bind enums as the enum type, not `String`.
- Response DTOs are records; they never contain `password`, hashes, tokens or another user's e-mail/phone.

## Formats

- Timestamps: ISO-8601 UTC (`Instant`). The `AuditModel` pattern `dd-MM-yyyy hh:mm:ss` is 12-hour with no
  AM/PM and not machine-parseable - replaced in Phase 4 housekeeping; new DTOs use `Instant`.
- Enums: uppercase strings. IDs: numeric for now (`Long`); public slugs arrive with Phase 2.
- `null` over omitted fields.

## Language

Most endpoints accept `?lang=en|bn` (default `en`); it selects the `ErrorCode` translation. Pass it through
to `ApplicationException`. Blog/answer content is not translated.

## OpenAPI

- `@Operation(summary=...)` on every endpoint, written for someone who doesn't know the domain.
- `@SecurityRequirement(name = "jwtToken")` on every authenticated endpoint - its absence must mean
  the endpoint really is public.
- `@Tag` per controller; `@CommonApiResponses` for the shared error responses.
- `/api/auth/**` is permit-all at the filter level, so any endpoint under it that needs a caller must
  say so and resolve the user from the security context itself (sign-out does).
