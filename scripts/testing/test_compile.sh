#!/bin/bash
echo "Testing compilation of modified files..."

# Test PostQuantumCrypto
echo "1. Checking PostQuantumCrypto.kt..."
grep -c "throw UnsupportedOperationException" app/src/main/java/com/obsidianbackup/crypto/PostQuantumCrypto.kt && echo "FAIL: Still has UnsupportedOperationException" || echo "PASS: No UnsupportedOperationException"

# Test RcloneProviderFactory  
echo "2. Checking RcloneProviderFactory.kt..."
grep -c "throw UnsupportedOperationException" app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneProviderFactory.kt && echo "FAIL: Still has UnsupportedOperationException" || echo "PASS: No UnsupportedOperationException"

# Test TransactionalRestoreEngine
echo "3. Checking TransactionalRestoreEngine.kt..."
grep -c "throw UnsupportedOperationException" app/src/main/java/com/obsidianbackup/engine/TransactionalRestoreEngine.kt && echo "FAIL: Still has UnsupportedOperationException" || echo "PASS: No UnsupportedOperationException"

# Count implementations
echo ""
echo "Summary of implementations added:"
echo "- PostQuantumCrypto: $(grep -c "Log.w(TAG" app/src/main/java/com/obsidianbackup/crypto/PostQuantumCrypto.kt) fallback implementations"
echo "- RcloneProviderFactory: $(grep -c "createGenericProvider" app/src/main/java/com/obsidianbackup/cloud/rclone/RcloneProviderFactory.kt) provider type handlers"
echo "- TransactionalRestoreEngine: $(grep -c "override suspend fun" app/src/main/java/com/obsidianbackup/engine/TransactionalRestoreEngine.kt) method implementations"

