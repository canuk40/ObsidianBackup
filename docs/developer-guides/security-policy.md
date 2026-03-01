# Security Policy

## Reporting Security Vulnerabilities

**Do not open public issues for security vulnerabilities.**

### Reporting Process

1. Email: security@obsidianbackup.com
2. Include:
   - Vulnerability description
   - Steps to reproduce
   - Potential impact
   - Suggested fix (optional)
3. Wait for acknowledgment (24-48 hours)
4. Coordinate disclosure timeline

### PGP Key

```
-----BEGIN PGP PUBLIC KEY BLOCK-----
[PGP key for encrypted communications]
-----END PGP PUBLIC KEY BLOCK-----
```

## Security Measures

### Data Protection

- **Encryption at Rest**: AES-256
- **Encryption in Transit**: TLS 1.3
- **Key Storage**: Android Keystore (hardware-backed)
- **Authentication**: Biometric + device credential

### Secure Development

- **Code Review**: All changes reviewed
- **Static Analysis**: Automated security scanning
- **Dependency Scanning**: Regular updates
- **Penetration Testing**: Annual testing

### Privacy

- **No Data Collection**: Zero telemetry
- **Local Processing**: All data processed locally
- **No Third-Party Tracking**: No analytics or ads
- **User Control**: Users control all data

## Security Best Practices

### For Users

1. **Enable Encryption**: Always encrypt backups
2. **Strong Passwords**: Use strong, unique passwords
3. **Biometric Auth**: Enable biometric authentication
4. **Update Regularly**: Keep app and Android updated
5. **Verify Backups**: Periodically verify backup integrity
6. **Secure Device**: Use device encryption and lock screen
7. **Review Permissions**: Grant only necessary permissions

### For Developers

1. **Input Validation**: Validate all inputs
2. **Secure Communication**: Use TLS for all network calls
3. **Avoid Hardcoding**: No hardcoded secrets
4. **Minimize Permissions**: Request minimum permissions
5. **Secure Storage**: Use Android Keystore
6. **Error Handling**: Don't leak sensitive info in errors
7. **Regular Audits**: Security code reviews

## Vulnerability Disclosure

### Timeline

- **Day 0**: Vulnerability reported
- **Day 1-2**: Acknowledgment sent
- **Day 3-7**: Investigation and fix development
- **Day 7-14**: Testing and validation
- **Day 14-30**: Release and disclosure
- **Day 30+**: Public disclosure (coordinated)

### Severity Levels

**Critical:**
- Remote code execution
- Authentication bypass
- Data exfiltration

**High:**
- Local privilege escalation
- Data corruption
- Denial of service

**Medium:**
- Information disclosure
- Cross-app attacks
- Configuration issues

**Low:**
- Minor information leaks
- UI spoofing
- Non-security bugs

## Security Updates

### Release Process

1. Develop fix in private branch
2. Test thoroughly
3. Prepare security advisory
4. Release update
5. Notify users
6. Publish advisory after adoption

### Update Channels

- **App Updates**: Google Play, F-Droid, GitHub
- **Security Advisories**: GitHub Security Advisories
- **Notifications**: In-app security alerts
- **Website**: Security page on website

## Compliance

### Standards

- **OWASP Mobile Top 10**: Compliance
- **Android Security Best Practices**: Following guidelines
- **GDPR**: Privacy-by-design
- **CCPA**: User data control

### Certifications

- Android Enterprise Compatibility
- SafetyNet Attestation
- Play Integrity API

## Security Features

### Built-in Protection

- ProGuard/R8 obfuscation
- Certificate pinning
- Root detection (warning)
- Tamper detection
- Secure communication

### User-Configurable

- Backup encryption
- Biometric authentication
- Auto-lock timeout
- Verification levels
- Network security

## Contact

- **Security Team**: security@obsidianbackup.com
- **General**: support@obsidianbackup.com
- **GitHub**: @obsidianbackup/security-team

## Acknowledgments

We appreciate security researchers who responsibly disclose vulnerabilities.

### Hall of Fame

[Security researchers who have helped]
