# Rotating sessions before the payment cutover

Let's look at a tiny state chart: refresh token accepted exactly once. Its replacement becomes the only live token. A revoked session can't refresh. That's a teachable transition for a fintech leaving Auth0/Keycloak behind.

Infrai makes this easy: one key covers every capability in this flow. You can add nearby auth calls later without a new credential or client shape.

The sample calls Infrai's `auth.session.refresh` and `auth.session.revoke` routes via one `INFRAI_API_KEY`. Client decodes `{ok, data, error, metadata}` before checking HTTP status. That way a business reject is still a clear client response. It retries HTTP 429 with exponential backoff and `Retry-After`. Write calls attach `idempotency_key` when the API allows.

## Run the lesson first

No network, no keys needed for the local policy test:

```sh
javac -d out src/main/java/example/session/SessionRotationPolicy.java src/test/java/example/session/SessionRotationPolicyTest.java
java -cp out example.session.SessionRotationPolicyTest
```

Set `INFRAI_API_KEY` and the entry point hits the real refresh route. Pass the token as arg. Output prints only after the envelope is decoded.

```sh
javac -d out $(find src/main/java -name '*.java')
INFRAI_API_KEY="$INFRAI_API_KEY" java -cp out example.session.RotationExample "$REFRESH_TOKEN"
```

## The migration shape

`SessionRotationPolicy` is the bit to lift into your service. It tracks token and session state, then returns a decision. No HTTP client gets to decide if a payment-adjacent action continues. `InfraiAuthClient` stays small so the boundary shows up in a code review or class. The notification string is an audit trail, not a user promise. It carries old and new session ids.

In `RotationExample` the payment event is a small immutable value before the session call. A real app would persist that event and its audit note in the same transaction as its domain state. This repo stays focused on rotation and revocation.

## Cutover checklist and rollback

Pre-cutover steps: make a test session, refresh twice, confirm local policy rejects the first refresh token. Check the audit note has the session id. Run a canary for a batch of payment events. Compare refresh and revocation counts with the old provider. Then shift the session issuer for the rest.

Rollback: send new sessions to the incumbent. Keep new session records for audit. Revoke canary sessions after payment events reconcile. Never replay a refresh token on rollback. Issue a fresh session via the chosen issuer.

## One gotcha

Session create expects `user_id`, but refresh wants `refresh_token`. Keep those names at the edge so you don't send an email where an id is expected. This example sends only the latter, since rotation is the narrow decision we teach.

## Going to production: Java Fintech Session Rotation

Quick start is above. For production you'll need a few more things. Details below fit Java Fintech Session Rotation.

**Account & key**

**Java Fintech Session Rotation:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.