# Monetization Quick Start Guide

## Quick Implementation Checklist

### 1. **Product IDs Setup in Google Play Console**

Create these subscription products:

| Product ID | Tier | Period | Price |
|------------|------|--------|-------|
| `obsidian_backup_pro_monthly` | Pro | Monthly | $3.99 |
| `obsidian_backup_pro_yearly` | Pro | Yearly | $39.99 |
| `obsidian_backup_team_monthly` | Team | Monthly | $9.99 |
| `obsidian_backup_team_yearly` | Team | Yearly | $99.99 |

Each product needs:
- Base plan with ID: `monthly` or `yearly`
- Free trial offer (14 days) for Pro and Team
- Enabled in all target countries

### 2. **Feature Usage Examples**

#### Check if Feature is Available
```kotlin
@Composable
fun MyFeature(viewModel: MyViewModel = hiltViewModel()) {
    val currentTier by viewModel.subscriptionManager
        .currentTier
        .collectAsState(FeatureTier.FREE)
    
    val featureGateService = viewModel.featureGateService
    val isAvailable by featureGateService
        .isFeatureAvailable(FeatureId.CLOUD_SYNC)
        .collectAsState(false)
    
    if (isAvailable) {
        CloudSyncButton()
    } else {
        UpgradeButton()
    }
}
```

#### Gate a Feature with Dialog
```kotlin
@Composable
fun ProtectedFeature() {
    val currentTier by remember { mutableStateOf(FeatureTier.FREE) }
    val navController = rememberNavController()
    
    ProFeatureGate(
        featureId = FeatureId.ENCRYPTION,
        currentTier = currentTier,
        onUpgradeClick = { 
            navController.navigate("subscriptions") 
        }
    ) {
        // Your premium feature UI
        EncryptionSettings()
    }
}
```

#### Launch Purchase Flow
```kotlin
@Composable
fun SubscribeButton(product: SubscriptionProduct) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: SubscriptionViewModel = hiltViewModel()
    
    Button(onClick = {
        activity?.let { 
            viewModel.purchaseSubscription(it, product) 
        }
    }) {
        Text("Subscribe to ${product.tier.displayName}")
    }
}
```

### 3. **Navigation Setup**

Add subscription screen to your navigation graph:

```kotlin
composable("subscriptions") {
    SubscriptionScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 4. **Initialize in Application**

Billing is automatically initialized via Hilt. Ensure your Application class has:

```kotlin
@HiltAndroidApp
class ObsidianBackupApplication : Application() {
    // Billing automatically initialized via DI
}
```

### 5. **Handle Purchase Results**

```kotlin
val purchaseResult by viewModel.purchaseResult.collectAsState()

LaunchedEffect(purchaseResult) {
    when (val result = purchaseResult) {
        is PurchaseResult.Success -> {
            Toast.makeText(context, "Subscribed!", Toast.LENGTH_SHORT).show()
            viewModel.resetPurchaseState()
        }
        is PurchaseResult.Error -> {
            Toast.makeText(
                context, 
                "Error: ${result.message}", 
                Toast.LENGTH_LONG
            ).show()
            viewModel.resetPurchaseState()
        }
        is PurchaseResult.Cancelled -> {
            viewModel.resetPurchaseState()
        }
        else -> {}
    }
}
```

### 6. **Test with License Testers**

1. Go to Play Console → Setup → License testing
2. Add test email addresses
3. Deploy app to internal testing track
4. Install app from Play Store (internal testing)
5. Make test purchases (automatically refunded for testers)

### 7. **Common Patterns**

#### Show Trial Banner
```kotlin
val isTrialEligible by viewModel.isTrialEligible.collectAsState()

if (isTrialEligible) {
    TrialBanner(
        onStartTrial = { product ->
            activity?.let { viewModel.purchaseSubscription(it, product) }
        }
    )
}
```

#### Show Grace Period Warning
```kotlin
val subscriptionState by viewModel.subscriptionState.collectAsState()

if (subscriptionState.isInGracePeriod) {
    GracePeriodWarning(
        daysRemaining = subscriptionState.daysUntilExpiry,
        onUpdatePayment = {
            // Open Play Store subscription management
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://play.google.com/store/account/subscriptions"
                )
            }
            context.startActivity(intent)
        }
    )
}
```

#### Restore Purchases
```kotlin
Button(onClick = { 
    viewModel.restorePurchases() 
}) {
    Icon(Icons.Default.Refresh, null)
    Spacer(Modifier.width(8.dp))
    Text("Restore Purchases")
}
```

### 8. **Testing Checklist**

Before releasing:

- [ ] All product IDs match Play Console exactly
- [ ] Free trial offers are configured correctly
- [ ] Test purchase flow with license tester account
- [ ] Verify features unlock after purchase
- [ ] Test restore purchases on different device
- [ ] Confirm subscription status persists across app restarts
- [ ] Test upgrade from Pro to Team
- [ ] Verify cancellation is detected properly
- [ ] Test grace period handling (requires payment failure)
- [ ] Confirm analytics events are tracked

### 9. **Feature Tier Reference**

| Feature | Free | Pro | Team | Enterprise |
|---------|------|-----|------|------------|
| Basic Backup | ✓ | ✓ | ✓ | ✓ |
| Incremental Backups | ✗ | ✓ | ✓ | ✓ |
| Cloud Sync | ✗ | ✓ | ✓ | ✓ |
| Encryption | ✗ | ✓ | ✓ | ✓ |
| Automation | ✗ | ✓ | ✓ | ✓ |
| Multi-Device | ✗ | ✗ | ✓ | ✓ |
| Shared Backups | ✗ | ✗ | ✓ | ✓ |
| API Access | ✗ | ✗ | ✗ | ✓ |
| Dedicated Support | ✗ | ✗ | ✗ | ✓ |

### 10. **Troubleshooting**

**Problem**: Purchases not detected
- Solution: Call `billingRepository.queryPurchases()` manually
- Check billing connection state

**Problem**: Features not unlocking
- Solution: Verify `FeatureId.minimumTier` matches expected tier
- Check `FeatureFlags.isFeatureAvailable()` logic

**Problem**: Can't test purchases
- Solution: Ensure app is in internal testing track
- Add email to license testers list
- Install from Play Store (not sideload)

**Problem**: Billing connection fails
- Solution: Check `com.android.vending.BILLING` permission in manifest
- Verify Play Store app is installed and updated
- Ensure device has Google account signed in

## File Structure Summary

```
billing/
├── BillingRepository.kt       # Core Play Billing v6 integration
├── SubscriptionManager.kt     # Trial, grace period, lifecycle
├── FeatureGateService.kt      # Check feature availability
├── RevenueAnalytics.kt        # Track revenue events
├── BillingModels.kt           # Data classes
├── BillingManager.kt          # Legacy compatibility facade
├── ProFeatureGate.kt          # Composable feature gates
├── di/BillingModule.kt        # Hilt DI setup
└── ui/
    ├── SubscriptionScreen.kt  # Full subscription UI
    ├── SubscriptionViewModel.kt
    └── UpgradePrompts.kt      # Dialogs and banners

model/FeatureTier.kt           # Tier and feature definitions
```

## Key Classes

- **BillingRepository**: Main interface to Play Billing Library
- **SubscriptionManager**: Handles trials, grace periods, state
- **FeatureGateService**: Check if features are available
- **RevenueAnalytics**: Track all revenue events
- **SubscriptionScreen**: Full-featured subscription management UI

## Next Steps

1. Set up products in Play Console
2. Add subscription navigation to your app
3. Implement feature gates where needed
4. Test with internal testing track
5. Monitor analytics and adjust pricing
6. Collect user feedback on value proposition

For full documentation, see [MONETIZATION.md](MONETIZATION.md)
