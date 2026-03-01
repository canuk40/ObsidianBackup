# 001. Layered Architecture

Date: 2024-01-15
Status: Accepted

## Context

ObsidianBackup needs a maintainable, testable, and scalable architecture. The application is complex with multiple concerns: UI, business logic, data access, and platform-specific implementations.

## Decision

Adopt a layered architecture with clear separation of concerns:

1. **Presentation Layer**: UI and ViewModels
2. **Domain Layer**: Business logic and use cases
3. **Data Layer**: Repositories and data sources
4. **Infrastructure Layer**: Platform-specific implementations

## Alternatives Considered

### MVC (Model-View-Controller)
- Simpler for small apps
- Poor separation for complex apps
- Tight coupling between layers

### VIPER (View-Interactor-Presenter-Entity-Router)
- Too complex for our needs
- Excessive boilerplate
- Steep learning curve

## Consequences

### Positive
- Clear separation of concerns
- Easy to test each layer independently
- Scalable architecture
- Industry-standard approach
- Good documentation and community support
- Easy onboarding for new developers

### Negative
- More initial setup required
- More files and classes
- Learning curve for junior developers
- Potential over-engineering for simple features

## Implementation

```
Presentation → Domain → Data → Infrastructure
```

Each layer only depends on layers below it, never above.

## References

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
