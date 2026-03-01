# Contributing Guidelines

Thank you for contributing to ObsidianBackup!

## Code of Conduct

Be respectful, inclusive, and professional.

## How to Contribute

### Reporting Bugs

1. Check existing issues
2. Create detailed bug report
3. Include:
   - Device and Android version
   - Steps to reproduce
   - Expected vs actual behavior
   - Logs if available

### Suggesting Features

1. Check existing discussions
2. Create feature request
3. Describe use case
4. Explain expected benefit

### Contributing Code

1. Fork repository
2. Create feature branch
3. Make changes
4. Write tests
5. Update documentation
6. Submit pull request

## Development Setup

```bash
git clone https://github.com/obsidianbackup/ObsidianBackup.git
cd ObsidianBackup
./gradlew build
```

## Coding Standards

### Kotlin Style

Follow [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)

### Code Formatting

```bash
# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### Linting

```bash
./gradlew lint
./gradlew detekt
```

## Commit Messages

Format: `type(scope): message`

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Maintenance

**Example:**
```
feat(backup): add incremental backup support
fix(cloud): resolve upload timeout issue
docs(api): update plugin documentation
```

## Pull Request Process

1. Update documentation
2. Add tests
3. Ensure CI passes
4. Request review
5. Address feedback
6. Merge approved

## Testing Requirements

- Unit tests for new code
- Integration tests for features
- UI tests for user-facing changes
- Maintain 80%+ coverage

## Documentation

- Update user guides
- Add API documentation
- Include code examples
- Update changelog

## License

By contributing, you agree to license your contributions under GPL-3.0.
