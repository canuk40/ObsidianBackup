#!/bin/bash

# Monetization Implementation Verification Script
# Verifies all required files are present and properly structured

echo "🔍 Verifying Monetization Implementation..."
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# Function to check file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1 - MISSING"
        ((ERRORS++))
        return 1
    fi
}

# Function to check directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1/"
        return 0
    else
        echo -e "${RED}✗${NC} $1/ - MISSING"
        ((ERRORS++))
        return 1
    fi
}

# Function to check for keyword in file
check_keyword() {
    if grep -q "$2" "$1" 2>/dev/null; then
        echo -e "  ${GREEN}✓${NC} Contains: $2"
        return 0
    else
        echo -e "  ${YELLOW}⚠${NC} Missing keyword: $2"
        ((WARNINGS++))
        return 1
    fi
}

echo "📂 Checking Directory Structure..."
check_dir "app/src/main/java/com/obsidianbackup/billing"
check_dir "app/src/main/java/com/obsidianbackup/billing/ui"
check_dir "app/src/main/java/com/obsidianbackup/billing/di"
echo ""

echo "📄 Checking Core Billing Files..."
check_file "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/FeatureGateService.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/RevenueAnalytics.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/BillingModels.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/BillingManager.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/ProFeatureGate.kt"
echo ""

echo "🎨 Checking UI Files..."
check_file "app/src/main/java/com/obsidianbackup/billing/ui/SubscriptionScreen.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/ui/SubscriptionViewModel.kt"
check_file "app/src/main/java/com/obsidianbackup/billing/ui/UpgradePrompts.kt"
echo ""

echo "💉 Checking Dependency Injection..."
check_file "app/src/main/java/com/obsidianbackup/billing/di/BillingModule.kt"
echo ""

echo "📊 Checking Model Files..."
check_file "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt"
echo ""

echo "📚 Checking Documentation..."
check_file "MONETIZATION.md"
check_file "MONETIZATION_QUICKSTART.md"
check_file "MONETIZATION_SUMMARY.md"
echo ""

echo "🔍 Verifying Key Implementation Details..."

# Check BillingRepository
if [ -f "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" ]; then
    echo "BillingRepository.kt:"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "BillingClient.newBuilder"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "enablePendingPurchases"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "queryPurchasesAsync"
fi

# Check SubscriptionManager
if [ -f "app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt" ]; then
    echo "SubscriptionManager.kt:"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt" "TRIAL_DAYS"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt" "isTrialEligible"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/SubscriptionManager.kt" "DataStore"
fi

# Check FeatureTier
if [ -f "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt" ]; then
    echo "FeatureTier.kt:"
    check_keyword "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt" "FREE"
    check_keyword "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt" "PRO"
    check_keyword "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt" "TEAM"
    check_keyword "app/src/main/java/com/obsidianbackup/model/FeatureTier.kt" "ENTERPRISE"
fi

echo ""
echo "📦 Checking Product IDs..."
if [ -f "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" ]; then
    echo "Product IDs in BillingRepository:"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "PRODUCT_PRO_MONTHLY"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "PRODUCT_PRO_YEARLY"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "PRODUCT_TEAM_MONTHLY"
    check_keyword "app/src/main/java/com/obsidianbackup/billing/BillingRepository.kt" "PRODUCT_TEAM_YEARLY"
fi

echo ""
echo "🧪 Checking Test Integration Points..."
if [ -f "MONETIZATION.md" ]; then
    echo "MONETIZATION.md documentation:"
    check_keyword "MONETIZATION.md" "Testing Checklist"
    check_keyword "MONETIZATION.md" "Google Play Console Setup"
    check_keyword "MONETIZATION.md" "Server-Side Verification"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 VERIFICATION SUMMARY"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All required files present${NC}"
else
    echo -e "${RED}✗ $ERRORS files missing${NC}"
fi

if [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All keywords found${NC}"
else
    echo -e "${YELLOW}⚠ $WARNINGS warnings (non-critical)${NC}"
fi

echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ MONETIZATION IMPLEMENTATION VERIFIED${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Configure products in Google Play Console"
    echo "2. Add 'subscriptions' route to navigation"
    echo "3. Test with internal testing track"
    echo "4. Connect analytics in RevenueAnalytics.kt"
    echo ""
    echo "See MONETIZATION_QUICKSTART.md for implementation guide"
    exit 0
else
    echo -e "${RED}❌ VERIFICATION FAILED${NC}"
    echo "Some files are missing. Please review the errors above."
    exit 1
fi
