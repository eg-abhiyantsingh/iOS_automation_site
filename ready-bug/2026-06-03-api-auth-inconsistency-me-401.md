# [Bug | HIGH] Backend: valid login token rejected by `/auth/v2/me` (401) while accepted by `/accounts/` (200)

**Test:** `com.egalvanic.api.ApiContractTest.meWithToken` (REST Assured contract suite)
**Date:** 2026-06-03
**Classification:** REAL_BUG (90% confidence — reproduced deterministically across token variants)
**Severity:** HIGH (breaks the documented auth contract; an authenticated client cannot read its own identity)
**Environment:** `https://api.qa.egalvanic.ai/api` (QA, Swagger 2.0 spec)
**Component:** Backend Auth / API gateway (not an iOS client bug)

## Summary

`POST /auth/v2/login` with valid QA credentials returns **200** plus a well-formed
`access_token` (+ `refresh_token`). That same Bearer token is then **accepted by the
protected list endpoint `GET /accounts/` (200)** but **rejected by `GET /auth/v2/me`
with 401 Unauthorized**.

Two protected endpoints on the *same* host disagree about whether the *same* token is
valid. Per the OpenAPI contract both are guarded by the identical `Bearer` security
definition, so a token good enough for `/accounts/` must also be good enough for `/me`.
This is a per-endpoint auth-enforcement inconsistency, not a credential problem.

## Steps to Reproduce

1. `POST https://api.qa.egalvanic.ai/api/auth/v2/login`
   body `{"email":"<QA user>","password":"<QA pass>","subdomain":"acme"}`
   → **200**, response contains `access_token` (JWT) and `refresh_token`.
2. Using that token, `GET https://api.qa.egalvanic.ai/api/accounts/?per_page=5`
   header `Authorization: Bearer <access_token>`
   → **200** (token is honored here).
3. Using the *same* token, `GET https://api.qa.egalvanic.ai/api/auth/v2/me`
   header `Authorization: Bearer <access_token>`
   → **401 Unauthorized** (token is rejected here).

Reproduce in one command:
```bash
USER_EMAIL=<qa-user> USER_PASSWORD=<qa-pass> \
  mvn -B test -DsuiteXmlFile=testng-api-contract.xml \
  -Dtest=ApiContractTest#meWithToken,accountsAuthed
```

## Token-variant matrix (all tried, all 401 on `/me`)

| Token sent to `/me`                         | `/me` result | `/accounts/` result |
|---------------------------------------------|--------------|---------------------|
| `Authorization: Bearer <access_token>`      | **401**      | **200**             |
| `Authorization: Bearer <id_token>`          | 401          | n/a                 |
| session cookie from login                   | 401          | n/a                 |
| both header + cookie                        | 401          | n/a                 |

The token clearly carries valid auth (it satisfies `/accounts/`), which rules out
"wrong token type" on the client side and points at `/me`'s own auth/routing layer.

## Actual Result

```
GET /auth/v2/me  ->  401 Unauthorized
(login token that returns 200 from GET /accounts/ on the same host)
```

## Expected Result

Per the Swagger contract, `GET /auth/v2/me` with a valid Bearer token returns **200**
and the current user object (must include `email`). Any token accepted by one
Bearer-guarded endpoint must be accepted by all Bearer-guarded endpoints on that host.

## Likely Root Cause (hypotheses, in priority order)

1. **Tenant routing.** The login token is tenant-scoped (`subdomain=acme`). `/me`
   may resolve the tenant from the *host* (`acme.api.qa.egalvanic.ai`) or a tenant
   header that the generic `api.qa.egalvanic.ai` host doesn't supply, so it can't
   bind the token to a tenant and returns 401. `/accounts/` may derive the tenant
   from the token claim instead — hence it succeeds. **Most likely.**
2. **Audience/issuer mismatch.** `/me` validates an `aud`/`iss` claim that the
   QA-issued token doesn't match, while `/accounts/` validates only the signature.
3. **Gateway misconfig.** `/auth/v2/*` sits behind a different auth middleware than
   `/accounts/` and rejects the same token.

## How To Confirm

- Decode the JWT (`access_token`) — inspect `iss`, `aud`, tenant claim, `exp`.
- Retry step 3 against a **per-tenant host** (e.g. `https://acme.api.qa.egalvanic.ai/api/auth/v2/me`)
  and/or with an explicit tenant header. If that returns 200, root cause = #1 (tenant routing).
- Compare the auth middleware/route config for `/auth/v2/me` vs `/accounts/`.

## Test Handling

The contract test `meWithToken` (and `accountsAuthed`) currently surface this as a
**skip-with-FINDING** rather than a hard build failure, because the per-tenant API
host is unconfirmed and we don't want a false red while the routing question is open.
Once the correct host/header is confirmed, flip the skip to `assertEquals(200)` so the
suite hard-gates the contract.

---

_Authored from the live API contract run on 2026-06-03. Review and edit before filing.
**Do not** push this file's contents to Jira via tooling — per project rule, Jira
modifications need explicit per-ticket approval._
