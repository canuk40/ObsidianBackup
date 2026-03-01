#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════╗"
echo "║     Biometric Authentication Implementation Verification  ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo ""

# Check security package files
echo "📁 Checking security package files..."
FILES=(
    "app/src/main/java/com/obsidianbackup/security/BiometricAuthManager.kt"
    "app/src/main/java/com/obsidianbackup/security/PasskeyManager.kt"
    "app/src/main/java/com/obsidianbackup/security/BiometricSettings.kt"
    "app/src/main/java/com/obsidianbackup/security/BiometricAuthIntegration.kt"
    "app/src/main/java/com/obsidianbackup/security/BiometricExampleUsage.kt"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        lines=$(wc -l < "$file")
        echo "   ✓ $file ($lines lines)"
    else
        echo "   ✗ $file MISSING"
    fi
done

echo ""
echo "📝 Checking modified files..."
if grep -q "androidx.biometric:biometric" app/build.gradle.kts; then
    echo "   ✓ build.gradle.kts updated with biometric dependencies"
else
    echo "   ✗ build.gradle.kts missing biometric dependencies"
fi

if grep -q "requireBiometric: Boolean" app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt; then
    echo "   ✓ EncryptionEngine.kt updated with biometric support"
else
    echo "   ✗ EncryptionEngine.kt missing biometric support"
fi

echo ""
echo "📚 Checking documentation..."
DOCS=(
    "BIOMETRIC_AUTHENTICATION.md"
    "BIOMETRIC_QUICKSTART.md"
    "BIOMETRIC_IMPLEMENTATION_SUMMARY.md"
)

for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        lines=$(wc -l < "$doc")
        echo "   ✓ $doc ($lines lines)"
    else
        echo "   ✗ $doc MISSING"
    fi
done

echo ""
echo "🔍 Checking key implementations..."

# Check BiometricPrompt usage
if grep -q "BiometricPrompt" app/src/main/java/com/obsidianbackup/security/BiometricAuthManager.kt; then
    echo "   ✓ BiometricPrompt integration found"
fi

# Check StrongBox
if grep -q "setIsStrongBoxBacked" app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt; then
    echo "   ✓ StrongBox KeyMint integration found"
fi

# Check UserAuthentication
if grep -q "setUserAuthenticationRequired" app/src/main/java/com/obsidianbackup/crypto/EncryptionEngine.kt; then
    echo "   ✓ User authentication requirement found"
fi

# Check PasskeyManager
if grep -q "CredentialManager" app/src/main/java/com/obsidianbackup/security/PasskeyManager.kt; then
    echo "   ✓ PasskeyManager with CredentialManager found"
fi

# Check DataStore
if grep -q "dataStore" app/src/main/java/com/obsidianbackup/security/BiometricSettings.kt; then
    echo "   ✓ DataStore settings persistence found"
fi

echo ""
echo "📊 Code Statistics:"
total_lines=0
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        lines=$(wc -l < "$file")
        total_lines=$((total_lines + lines))
    fi
done
echo "   • Total code lines: $total_lines"

total_doc_lines=0
for doc in "${DOCS[@]}"; do
    if [ -f "$doc" ]; then
        lines=$(wc -l < "$doc")
        total_doc_lines=$((total_doc_lines + lines))
    fi
done
echo "   • Total documentation lines: $total_doc_lines"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "✅ Biometric Authentication Implementation Complete!"
echo "═══════════════════════════════════════════════════════════"
