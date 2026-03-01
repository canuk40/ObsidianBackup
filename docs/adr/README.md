# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for ObsidianBackup.

## What is an ADR?

An ADR documents an important architectural decision made along with its context and consequences.

## Format

Each ADR follows this format:

```markdown
# [Number]. [Title]

Date: YYYY-MM-DD
Status: [Proposed | Accepted | Deprecated | Superseded]

## Context

What is the issue we're facing?

## Decision

What decision did we make?

## Consequences

What becomes easier or more difficult?
```

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [001](001-architecture-layered.md) | Layered Architecture | Accepted |
| [002](002-plugin-system.md) | Plugin-Based Automation | Accepted |
| [003](003-merkle-tree-verification.md) | Merkle Tree for Verification | Accepted |
| [004](004-jetpack-compose-ui.md) | Jetpack Compose for UI | Accepted |
| [005](005-room-database.md) | Room for Local Storage | Accepted |

## Creating a New ADR

1. Copy template
2. Increment number
3. Fill in sections
4. Submit for review
5. Update index

## Template

```markdown
# [Number]. [Title]

Date: YYYY-MM-DD
Status: Proposed

## Context

[Describe the context and problem]

## Decision

[Describe the decision]

## Alternatives Considered

[List alternatives]

## Consequences

### Positive
- [Benefit 1]
- [Benefit 2]

### Negative
- [Drawback 1]
- [Drawback 2]

## References

- [Link 1]
- [Link 2]
```
