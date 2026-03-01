#!/bin/bash
# Security Verification Script for ObsidianBackup
# Tests security hardening implementations

echo "============================================"
echo "ObsidianBackup Security Verification"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0
WARN=0

check_pass() {
    echo -e "${GREEN}✓ PASS:${NC} $1"
    ((PASS++))
}

check_fail() {
    echo -e "${RED}✗ FAIL:${NC} $1"
    ((FAIL++))
}

check_warn() {
    echo -e "${YELLOW}⚠ WARN:${NC} $1"
    ((WARN++))
}

echo "1. Checking Security Files Existence"
echo "-------------------------------------"

# Check security implementation files
if [ -f "app/src/main/java/com/obsidianbackup/security/RootDetectionManager.kt" ]; then
    check_pass "RootDetectionManager.kt exists"
else
    check_fail "RootDetectionManager.kt missing"
fi

if [ -f "app/src/main/java/com/obsidianbackup/security/CertificatePinningManager.kt" ]; then
    check_pass "CertificatePinningManager.kt exists"
else
    check_fail "CertificatePinningManager.kt missing"
fi

if [ -f "app/src/main/java/com/obsidianbackup/security/SecureStorageManager.kt" ]; then
    check_pass "SecureStorageManager.kt exists"
else
    check_fail "SecureStorageManager.kt missing"
fi

if [ -f "app/src/main/java/com/obsidianbackup/security/SecureDatabaseHelper.kt" ]; then
    check_pass "SecureDatabaseHelper.kt exists"
else
    check_fail "SecureDatabaseHelper.kt missing"
fi

if [ -f "app/src/main/java/com/obsidianbackup/security/WebViewSecurityManager.kt" ]; then
    check_pass "WebViewSecurityManager.kt exists"
else
    check_fail "WebViewSecurityManager.kt missing"
fi

echo ""
echo "2. Checking Configuration Files"
echo "--------------------------------"

# Check network security config
if [ -f "app/src/main/res/xml/network_security_config.xml" ]; then
    check_pass "network_security_config.xml exists"
    
    # Verify it has certificate pinning configured
    if grep -q "pin-set" "app/src/main/res/xml/network_security_config.xml"; then
        check_pass "Certificate pinning configured"
    else
        check_warn "Certificate pinning not configured in network_security_config.xml"
    fi
    
    # Verify cleartext traffic is disabled
    if grep -q 'cleartextTrafficPermitted="false"' "app/src/main/res/xml/network_security_config.xml"; then
        check_pass "Cleartext traffic disabled"
    else
        check_fail "Cleartext traffic not explicitly disabled"
    fi
else
    check_fail "network_security_config.xml missing"
fi

# Check ProGuard rules
if [ -f "app/proguard-rules.pro" ]; then
    check_pass "proguard-rules.pro exists"
    
    # Check for security-specific rules
    if grep -q "security" "app/proguard-rules.pro"; then
        check_pass "Security rules in ProGuard configuration"
    else
        check_warn "No specific security rules found in ProGuard"
    fi
    
    # Check for logging removal
    if grep -q "assumenosideeffects.*Log" "app/proguard-rules.pro"; then
        check_pass "Logging removal configured"
    else
        check_warn "Logging removal not configured in ProGuard"
    fi
else
    check_fail "proguard-rules.pro missing"
fi

echo ""
echo "3. Checking Build Configuration"
echo "--------------------------------"

if [ -f "app/build.gradle.kts" ]; then
    check_pass "build.gradle.kts exists"
    
    # Check if minification is enabled
    if grep -q 'isMinifyEnabled = true' "app/build.gradle.kts"; then
        check_pass "Minification enabled in release build"
    else
        check_fail "Minification not enabled"
    fi
    
    # Check if resource shrinking is enabled
    if grep -q 'isShrinkResources = true' "app/build.gradle.kts"; then
        check_pass "Resource shrinking enabled"
    else
        check_warn "Resource shrinking not enabled"
    fi
    
    # Check for security dependencies
    if grep -q "play-services-safetynet" "app/build.gradle.kts"; then
        check_pass "SafetyNet dependency added"
    else
        check_fail "SafetyNet dependency missing"
    fi
    
    if grep -q "android-database-sqlcipher" "app/build.gradle.kts"; then
        check_pass "SQLCipher dependency added"
    else
        check_fail "SQLCipher dependency missing"
    fi
    
    if grep -q "security-crypto" "app/build.gradle.kts"; then
        check_pass "Security Crypto dependency added"
    else
        check_fail "Security Crypto dependency missing"
    fi
else
    check_fail "build.gradle.kts missing"
fi

echo ""
echo "4. Checking AndroidManifest Configuration"
echo "------------------------------------------"

if [ -f "app/src/main/AndroidManifest.xml" ]; then
    check_pass "AndroidManifest.xml exists"
    
    # Check for network security config reference
    if grep -q "networkSecurityConfig" "app/src/main/AndroidManifest.xml"; then
        check_pass "Network security config referenced in manifest"
    else
        check_fail "Network security config not referenced in manifest"
    fi
    
    # Check if cleartext traffic is disabled
    if grep -q 'usesCleartextTraffic="false"' "app/src/main/AndroidManifest.xml"; then
        check_pass "Cleartext traffic disabled in manifest"
    else
        check_warn "Cleartext traffic not explicitly disabled in manifest"
    fi
    
    # Check for exported components
    exported_count=$(grep -c 'android:exported="true"' "app/src/main/AndroidManifest.xml" || true)
    if [ $exported_count -gt 0 ]; then
        check_warn "Found $exported_count exported components - ensure they are properly secured"
    else
        check_pass "No exported components (or all properly secured)"
    fi
else
    check_fail "AndroidManifest.xml missing"
fi

echo ""
echo "5. Checking Code Quality"
echo "------------------------"

# Check for hardcoded secrets (basic check)
if grep -r "api[_-]key\|password\|secret" --include="*.kt" --include="*.java" app/src/main/java/ 2>/dev/null | grep -v "//.*api[_-]key" | grep -v "TODO" | grep -v "YOUR_" | grep -v "REPLACE" > /dev/null; then
    check_warn "Potential hardcoded secrets found - review manually"
else
    check_pass "No obvious hardcoded secrets detected"
fi

# Check for SQL injection vulnerabilities (basic check)
if grep -r "execSQL\|rawQuery" --include="*.kt" --include="*.java" app/src/main/java/ 2>/dev/null | grep -v "?" > /dev/null; then
    check_warn "Potential SQL injection vulnerability - review queries"
else
    check_pass "No obvious SQL injection vulnerabilities detected"
fi

# Check for insecure WebView usage
if grep -r "setJavaScriptEnabled(true)" --include="*.kt" --include="*.java" app/src/main/java/ 2>/dev/null; then
    check_warn "JavaScript enabled in WebView - ensure proper security measures"
else
    check_pass "No insecure WebView JavaScript usage detected"
fi

echo ""
echo "6. Security Documentation"
echo "-------------------------"

if [ -f "SECURITY_HARDENING.md" ]; then
    check_pass "SECURITY_HARDENING.md exists"
    
    # Check for OWASP compliance documentation
    if grep -q "OWASP Mobile Top 10" "SECURITY_HARDENING.md"; then
        check_pass "OWASP Mobile Top 10 documented"
    else
        check_fail "OWASP Mobile Top 10 not documented"
    fi
    
    # Check for penetration testing checklist
    if grep -q "Penetration Testing Checklist" "SECURITY_HARDENING.md"; then
        check_pass "Penetration testing checklist included"
    else
        check_warn "Penetration testing checklist not found"
    fi
else
    check_fail "SECURITY_HARDENING.md missing"
fi

echo ""
echo "============================================"
echo "Security Verification Summary"
echo "============================================"
echo -e "${GREEN}Passed:${NC} $PASS"
echo -e "${YELLOW}Warnings:${NC} $WARN"
echo -e "${RED}Failed:${NC} $FAIL"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ All critical security checks passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some security checks failed. Please review and fix.${NC}"
    exit 1
fi
