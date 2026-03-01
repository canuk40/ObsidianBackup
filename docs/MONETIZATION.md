# ObsidianBackup Monetization Infrastructure

## Overview

ObsidianBackup implements a comprehensive subscription-based monetization system using Google Play Billing Library v6. The system supports multiple subscription tiers, free trials, promo codes, and advanced revenue analytics.

## Architecture

### Components

```
billing/
├── BillingRepository.kt          # Core billing operations with Play Billing v6
├── SubscriptionManager.kt        # Subscription lifecycle management
├── FeatureGateService.kt         # Feature access control
├── RevenueAnalytics.kt           # Revenue tracking and analytics
├── BillingModels.kt              # Data models
├── BillingManager.kt             # Legacy facade (backward compatibility)
├── ProFeatureGate.kt             # Composable feature gates
├── di/
│   └── BillingModule.kt          # Dependency injection
└── ui/
    ├── SubscriptionScreen.kt     # Main subscription UI
    ├── SubscriptionViewModel.kt  # UI state management
    └── UpgradePrompts.kt         # Upgrade dialogs and prompts

model/
└── FeatureTier.kt                # Tier and feature definitions
```

## Subscription Tiers

### Free Tier
- **Price**: Free
- **Features**:
  - Basic backup and restore
  - Limited to 3 apps
  - Local storage only
  - Basic logs

### Pro Tier ($3.99/month or $39.99/year)
- **Features**:
  - Unlimited backups
  - Incremental backups
  - Cloud sync (Google Drive, WebDAV, Rclone)
  - AES-256 encryption
  - Restore simulation
  - Automation (scheduled/event-based)
  - Parallel operations
  - Exportable logs
  - 14-day free trial

### Team Tier ($9.99/month or $99.99/year)
- **All Pro features plus**:
  - Multi-device management
  - Device-to-device migration
  - Shared backups
  - Priority restore queues
  - Team collaboration
  - 14-day free trial

### Enterprise Tier (Custom Pricing)
- **All Team features plus**:
  - Custom retention policies
  - REST API access
  - White-label options
  - Dedicated support (24/7)
  - SLA guarantees
  - On-premise deployment options
  - Contact sales for pricing

## Implementation Guide

### 1. Initialize Billing

The billing system is automatically initialized via Hilt dependency injection:

```kotlin
@Inject
lateinit var billingRepository: BillingRepository

@Inject
lateinit var subscriptionManager: SubscriptionManager
```

### 2. Check Subscription Status

```kotlin
// In your composable
val subscriptionState by subscriptionManager
    .currentTier
    .collectAsState(initial = FeatureTier.FREE)

// Check if user has premium access
if (subscriptionState != FeatureTier.FREE) {
    // Show premium features
}
```

### 3. Feature Gating

Use the feature gate composable to protect premium features:

```kotlin
@Composable
fun MyPremiumFeature(
    viewModel: MyViewModel = hiltViewModel()
) {
    val currentTier by viewModel.currentTier.collectAsState()

    ProFeatureGate(
        featureId = FeatureId.CLOUD_SYNC,
        currentTier = currentTier,
        onUpgradeClick = { 
            navController.navigate("subscriptions")
        }
    ) {
        // Premium feature UI
        CloudSyncSettings()
    }
}
```

### 4. Programmatic Feature Checks

```kotlin
@Inject
lateinit var featureGateService: FeatureGateService

// Check if feature is available
val isAvailable = featureGateService
    .isFeatureAvailable(FeatureId.ENCRYPTION)
    .first()

if (isAvailable) {
    // Enable encryption
} else {
    // Show upgrade prompt
}
```

### 5. Launch Purchase Flow

```kotlin
// In your ViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    fun purchaseSubscription(
        activity: Activity,
        product: SubscriptionProduct
    ) {
        billingRepository.launchBillingFlow(activity, product)
    }
}

// In your composable
val activity = LocalContext.current as Activity
Button(onClick = { 
    viewModel.purchaseSubscription(activity, selectedProduct)
}) {
    Text("Subscribe")
}
```

### 6. Handle Purchase Results

```kotlin
val purchaseResult by viewModel.purchaseResult.collectAsState()

LaunchedEffect(purchaseResult) {
    when (purchaseResult) {
        is PurchaseResult.Success -> {
            // Show success message
            showSnackbar("Subscription activated!")
        }
        is PurchaseResult.Error -> {
            // Show error
            showSnackbar("Purchase failed: ${purchaseResult.message}")
        }
        is PurchaseResult.Cancelled -> {
            // User cancelled
        }
        else -> {}
    }
}
```

## Free Trial Management

### Trial Eligibility

Users are eligible for a 14-day free trial if they haven't used a trial before:

```kotlin
val isTrialEligible by subscriptionManager
    .isTrialEligible()
    .collectAsState(initial = false)

if (isTrialEligible) {
    // Show trial offer
}
```

### Trial Period Tracking

```kotlin
val trialDaysRemaining by subscriptionManager
    .trialDaysRemaining
    .collectAsState(initial = null)

if (trialDaysRemaining != null) {
    // Show trial countdown
    Text("Trial: $trialDaysRemaining days remaining")
}
```

### Trial Conversion

The system automatically tracks trial conversions and reports them to analytics:

```kotlin
// Handled automatically in SubscriptionManager
// Tracks when user converts from trial to paid subscription
```

## Promo Codes

### Applying Promo Codes

```kotlin
fun purchaseWithPromo(
    activity: Activity,
    product: SubscriptionProduct,
    promoCode: String
) {
    billingRepository.launchBillingFlowWithPromo(
        activity, 
        product, 
        promoCode
    )
}
```

### Promo Code Validation

Promo codes are validated server-side by Google Play. The client-side `PromoCode` model is for display purposes:

```kotlin
data class PromoCode(
    val code: String,
    val discount: Int,
    val validUntil: Long,
    val applicableTiers: List<FeatureTier>
)
```

## Grace Period Handling

When a subscription payment fails, Google Play provides a grace period:

```kotlin
val gracePeriodInfo by billingRepository
    .gracePeriodInfo
    .collectAsState()

if (gracePeriodInfo.isInGracePeriod) {
    // Show warning to update payment method
    GracePeriodWarning(
        daysRemaining = gracePeriodInfo.daysRemaining,
        onUpdatePayment = { /* Open Play Store */ }
    )
}
```

## Subscription Cancellation

Users cancel subscriptions through Google Play. The app detects cancellation status:

```kotlin
val subscriptionState by billingRepository
    .subscriptionState
    .collectAsState()

if (!subscriptionState.isAutoRenewing) {
    // Subscription is cancelled, will expire on expiryTime
    Text("Subscription expires in ${subscriptionState.daysUntilExpiry} days")
}
```

To guide users to cancel:

```kotlin
fun openSubscriptionManagement(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://play.google.com/store/account/subscriptions")
    }
    context.startActivity(intent)
}
```

## Restore Purchases

Users can restore purchases on new devices:

```kotlin
suspend fun restorePurchases(): Boolean {
    return billingRepository.restorePurchases()
}

// In UI
Button(onClick = { 
    viewModel.restorePurchases() 
}) {
    Icon(Icons.Default.Refresh, "Restore")
    Text("Restore Purchases")
}
```

## Revenue Analytics

### Tracking Events

The system automatically tracks all revenue events:

```kotlin
// Events tracked automatically:
- SUBSCRIPTION_STARTED
- SUBSCRIPTION_RENEWED
- SUBSCRIPTION_UPGRADED
- SUBSCRIPTION_DOWNGRADED
- SUBSCRIPTION_CANCELLED
- SUBSCRIPTION_EXPIRED
- TRIAL_STARTED
- TRIAL_CONVERTED
- TRIAL_EXPIRED
- PURCHASE_RESTORED
```

### Custom Analytics Integration

Integrate with your analytics platform:

```kotlin
class RevenueAnalytics @Inject constructor() {
    
    suspend fun trackEvent(event: RevenueEvent) {
        // Firebase Analytics
        firebaseAnalytics.logEvent("revenue_event", bundleOf(
            "type" to event.eventType.name,
            "tier" to event.tier.name,
            "price" to event.price,
            "currency" to event.currency
        ))
        
        // Amplitude
        amplitude.logEvent("Revenue Event", mapOf(
            "event_type" to event.eventType.name,
            "tier" to event.tier.name,
            "revenue" to event.price
        ))
    }
}
```

### Revenue Metrics

```kotlin
val metrics = revenueAnalytics.getRevenueMetrics(
    startTime = monthStart,
    endTime = monthEnd
)

// Returns:
// - totalRevenue
// - newSubscriptions
// - renewals
// - upgrades/downgrades
// - cancellations
// - trialConversions
// - ARPU
// - MRR
```

## Google Play Console Setup

### 1. Create Subscription Products

1. Go to Google Play Console → Your App → Monetization → Subscriptions
2. Create four subscription products:
   - `obsidian_backup_pro_monthly` - Pro Monthly ($3.99)
   - `obsidian_backup_pro_yearly` - Pro Yearly ($39.99)
   - `obsidian_backup_team_monthly` - Team Monthly ($9.99)
   - `obsidian_backup_team_yearly` - Team Yearly ($99.99)

### 2. Configure Base Plans

For each subscription product:
1. Create base plan with ID `monthly` or `yearly`
2. Set billing period (1 month or 12 months)
3. Set price in all applicable countries

### 3. Add Free Trial Offer

For Pro and Team subscriptions:
1. Add offer to base plan
2. Offer type: Free trial
3. Duration: 14 days
4. Eligibility: New subscribers only

### 4. Configure Promo Codes

1. Go to Order Management → Promo codes
2. Create promo code campaigns
3. Set discount percentage and duration
4. Limit to specific products/countries

### 5. Test with License Testers

1. Add test accounts in Play Console
2. Build and deploy app to internal testing track
3. Test purchase flows with test accounts
4. Verify subscriptions appear in test account

## Testing

### Local Testing

Use Play Billing Library test methods:

```kotlin
// In debug builds, use test product IDs
const val TEST_PRODUCT_ID = "android.test.purchased"

// Or set up test accounts in Play Console
```

### Integration Testing

1. Deploy to internal testing track
2. Add license testers in Play Console
3. License testers can make real purchases that are automatically refunded
4. Test all flows:
   - New subscription
   - Trial start
   - Trial conversion
   - Upgrade/downgrade
   - Cancellation
   - Restore purchases
   - Grace period

### Production Testing Checklist

- [ ] Verify all product IDs match Play Console
- [ ] Test purchase flow on multiple devices
- [ ] Verify subscription status updates correctly
- [ ] Test feature gating works properly
- [ ] Confirm analytics events are tracked
- [ ] Test restore purchases on new device
- [ ] Verify grace period handling
- [ ] Test promo code application
- [ ] Confirm cancellation detection
- [ ] Verify trial eligibility logic

## Security Best Practices

### 1. Server-Side Verification

**Important**: In production, verify all purchases on your backend:

```kotlin
// After successful purchase
val purchaseToken = purchase.purchaseToken
val productId = purchase.products.first()

// Send to your backend for verification
api.verifyPurchase(purchaseToken, productId)
```

Backend verification with Google Play Developer API:
```python
from google.oauth2 import service_account
from googleapiclient.discovery import build

credentials = service_account.Credentials.from_service_account_file(
    'service-account-key.json',
    scopes=['https://www.googleapis.com/auth/androidpublisher']
)

service = build('androidpublisher', 'v3', credentials=credentials)

result = service.purchases().subscriptions().get(
    packageName='com.obsidianbackup',
    subscriptionId=product_id,
    token=purchase_token
).execute()

# Verify result.expiryTimeMillis, autoRenewing, etc.
```

### 2. Obfuscate Product IDs

Use ProGuard/R8 to obfuscate billing-related code:

```proguard
# Keep billing classes
-keep class com.android.billingclient.** { *; }
-keep class com.obsidianbackup.billing.** { *; }
```

### 3. Secure Data Storage

Subscription state is stored in DataStore with encryption:

```kotlin
private val Context.subscriptionDataStore: DataStore<Preferences> 
    by preferencesDataStore(
        name = "subscription_prefs",
        // Consider adding encryption
    )
```

### 4. Network Security

Ensure all API calls use HTTPS and certificate pinning if communicating with your backend.

## Troubleshooting

### Purchase Not Detected

1. Check billing connection state:
```kotlin
val connectionState by billingRepository.connectionState.collectAsState()
if (connectionState !is BillingConnectionState.Connected) {
    // Reconnect
}
```

2. Manually query purchases:
```kotlin
billingRepository.queryPurchases()
```

### Feature Not Unlocking

1. Verify subscription state:
```kotlin
val state = subscriptionState.value
Log.d("Billing", "Tier: ${state.tier}, Active: ${state.isActive}")
```

2. Check feature mapping:
```kotlin
val featureFlags = FeatureFlags()
val available = featureFlags.isFeatureAvailable(featureId, currentTier)
```

### Billing Connection Fails

1. Check manifest has billing permission:
```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

2. Verify Play Store is installed and updated
3. Check device has active Google account
4. Ensure app is published (at least to internal testing)

## Migration Guide

### From Old Billing System

If migrating from an older billing implementation:

1. **Keep old product IDs** for existing subscribers:
```kotlin
// Map old to new
val legacyProductMap = mapOf(
    "titan_backup_pro" to PRODUCT_PRO_MONTHLY,
    "titan_backup_pro_monthly" to PRODUCT_PRO_MONTHLY,
    "titan_backup_pro_yearly" to PRODUCT_PRO_YEARLY
)
```

2. **Handle migration** in BillingRepository:
```kotlin
private fun migrateLegacyPurchases(purchases: List<Purchase>) {
    purchases.forEach { purchase ->
        purchase.products.forEach { productId ->
            legacyProductMap[productId]?.let { newProductId ->
                // Grant access based on new product
            }
        }
    }
}
```

3. **Test thoroughly** with existing subscribers

## Performance Considerations

### Caching

Subscription state is cached in memory:
```kotlin
private val _subscriptionState = MutableStateFlow(SubscriptionState())
```

### Background Sync

Periodically sync subscription status:
```kotlin
class SubscriptionSyncWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        billingRepository.queryPurchases()
        return Result.success()
    }
}

// Schedule daily sync
val syncRequest = PeriodicWorkRequestBuilder<SubscriptionSyncWorker>(
    1, TimeUnit.DAYS
).build()
```

## Support

For billing-related support:
- **Documentation**: https://developer.android.com/google/play/billing
- **GitHub Issues**: Report bugs in the project repository
- **Email**: support@obsidianbackup.com

## License

This monetization infrastructure is part of ObsidianBackup and follows the same license terms.

---

**Last Updated**: 2024
**Billing Library Version**: 6.0.1
**Minimum Android Version**: API 26 (Android 8.0)
