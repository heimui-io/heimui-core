# Security Policy

## Reporting a vulnerability

Please do not open a public issue for a security problem.

Report it privately through GitHub's [private vulnerability
reporting](https://github.com/heimui-io/heimui-core/security/advisories/new), or by email to
**support@heimui.io**. Include the version, the platform, and enough detail to reproduce it.

You will get an acknowledgement within 72 hours. HeimUI is maintained by one person, so a fix may
take longer than that -- you will be told what is happening either way, and credited in the
advisory unless you ask otherwise.

## What is in scope

This repository is the client SDK. It runs inside other people's applications, which is what makes
a flaw here worth reporting rather than working around: a bad parse, a bypassed host restriction or
an accepted forged signature reaches every application that has shipped the affected version.

Particularly interesting:

- **Signature verification** -- anything that makes `Es256SignatureVerifier` accept a payload it
  should reject, or reject one it should accept.
- **Payload handling** -- input that crashes the renderer, exhausts memory, or escapes the
  component model.
- **Network policy** -- a request that reaches a host the configuration did not allow, or cleartext
  where it was not permitted.

Out of scope here: the Studio and the hosted service, which are not in this repository.

## Supported versions

The SDK is pre-1.0. Fixes land on the latest published version; older alphas are not patched.
