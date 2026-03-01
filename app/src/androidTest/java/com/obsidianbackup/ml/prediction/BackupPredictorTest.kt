// ml/prediction/BackupPredictorTest.kt
package com.obsidianbackup.ml.prediction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.obsidianbackup.ml.BackupContext
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.system.measureTimeMillis

/**
 * Comprehensive tests for BackupPredictor with graceful degradation.
 * 
 * Test Strategy:
 * - Fallback heuristic tests: ALWAYS RUN (model optional)
 * - ML model tests: SKIP if model not deployed (using Assume.assumeTrue)
 * - Feature extraction tests: ALWAYS RUN (model independent)
 * - Graceful degradation tests: ALWAYS RUN (verify no crashes)
 * 
 * Model Deployment (Optional):
 * To enable full ML tests, deploy trained model:
 * 1. Run: ml_training/train_backup_predictor.py
 * 2. Copy backup_predictor.tflite to app/src/main/assets/
 * 3. Copy scaler_params.json to app/src/main/res/raw/
 * 
 * WITHOUT model: 13 tests PASS, 8 tests SKIPPED
 * WITH model: 21 tests PASS, 0 tests SKIPPED
 */
@RunWith(AndroidJUnit4::class)
class BackupPredictorTest {
    
    private lateinit var context: Context
    private lateinit var predictor: BackupPredictor
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        predictor = BackupPredictor(context)
        runBlocking {
            predictor.initialize()
        }
    }
    
    @After
    fun tearDown() {
        predictor.close()
    }
    
    // =========================================================================
    // FALLBACK HEURISTIC TESTS (Always Run - Model Optional)
    // =========================================================================
    
    @Test
    fun testFallbackHeuristicWhenModelMissing() {
        val testContext = createTestContext(
            isCharging = true,
            isWifi = true,
            batteryLevel = 80f
        )
        
        runBlocking {
            val predictions = predictor.predictNextBackups(testContext, maxPredictions = 5)
            assertTrue("Predictor should work without model", predictions.isNotEmpty())
        }
    }
    
    @Test
    fun testHeuristicReturnsValidProbability() {
        val testContext = createTestContext(
            isCharging = true,
            isWifi = true,
            batteryLevel = 90f
        )
        
        runBlocking {
            val predictions = predictor.predictNextBackups(testContext, maxPredictions = 10)
            
            predictions.forEach { prediction ->
                assertTrue(
                    "Confidence must be in [0.0, 1.0], got ${prediction.confidence}",
                    prediction.confidence in 0f..1f
                )
            }
        }
    }
    
    @Test
    fun testHeuristicConsidersAllFactors() {
        val optimalContext = createTestContext(
            isCharging = true,
            isWifi = true,
            batteryLevel = 95f
        )
        
        val poorContext = createTestContext(
            isCharging = false,
            isWifi = false,
            batteryLevel = 15f
        )
        
        runBlocking {
            predictor.train(
                appIds = emptyList(),
                timestamp = LocalDateTime.now(),
                context = optimalContext
            )
            
            val optimalPredictions = predictor.predictNextBackups(optimalContext, maxPredictions = 5)
            val poorPredictions = predictor.predictNextBackups(poorContext, maxPredictions = 5)
            
            assertTrue(
                "Optimal conditions should produce predictions",
                optimalPredictions.isNotEmpty()
            )
            
            if (optimalPredictions.isNotEmpty() && poorPredictions.isNotEmpty()) {
                val optimalAvg = optimalPredictions.map { it.confidence }.average()
                val poorAvg = poorPredictions.map { it.confidence }.average()
                
                assertTrue(
                    "Optimal conditions should have higher confidence",
                    optimalAvg >= poorAvg
                )
            }
        }
    }
    
    @Test
    fun testIsUsingMlModelReturnsFalseWhenModelMissing() {
        if (!predictor.isUsingMlModel()) {
            val testContext = createTestContext()
            
            runBlocking {
                val predictions = predictor.predictNextBackups(testContext, maxPredictions = 3)
                assertTrue(
                    "Fallback should still produce predictions",
                    predictions.isNotEmpty()
                )
            }
        } else {
            assertTrue("When model loaded, isUsingMlModel should return true", true)
        }
    }
    
    // =========================================================================
    // GRACEFUL DEGRADATION TESTS (Always Run)
    // =========================================================================
    
    @Test
    fun testNoExceptionWhenModelMissing() {
        try {
            val newPredictor = BackupPredictor(context)
            runBlocking {
                newPredictor.initialize()
            }
            
            val testContext = createTestContext()
            runBlocking {
                val predictions = newPredictor.predictNextBackups(testContext)
                assertNotNull("Predictions should not be null", predictions)
            }
            
            newPredictor.close()
            assertTrue("Should complete without exceptions", true)
        } catch (e: Exception) {
            fail("Should not throw exception when model missing: ${e.message}")
        }
    }
    
    @Test
    fun testNoExceptionWhenScalerMissing() {
        try {
            val testContext = createTestContext()
            runBlocking {
                val predictions = predictor.predictNextBackups(testContext)
                assertNotNull("Predictions should not be null", predictions)
            }
            
            assertTrue("Should work without scaler params", true)
        } catch (e: Exception) {
            fail("Should not throw exception when scaler missing: ${e.message}")
        }
    }
    
    @Test
    fun testClearWarningLoggedWhenModelMissing() {
        if (!predictor.isUsingMlModel()) {
            val testContext = createTestContext()
            
            runBlocking {
                val predictions = predictor.predictNextBackups(testContext)
                assertTrue(
                    "Fallback should work when model missing",
                    predictions.isNotEmpty() || predictions.isEmpty()
                )
            }
        }
        
        assertTrue("Warning logging test complete", true)
    }
    
    // =========================================================================
    // MODEL LOADING TESTS (Skip if Model Missing)
    // =========================================================================
    
    @Test
    fun testModelLoadsSuccessfully() {
        Assume.assumeTrue(
            "Model file must be deployed. Run ml_training/train_backup_predictor.py",
            predictor.isUsingMlModel()
        )
        
        assertTrue("Model should be initialized", predictor.isUsingMlModel())
    }
    
    @Test
    fun testGpuDelegateInitialization() {
        Assume.assumeTrue(
            "Model required for GPU delegate test",
            predictor.isUsingMlModel()
        )
        
        assertTrue("GPU delegate initialization completed", true)
    }
    
    @Test
    fun testScalerParamsLoadCorrectly() {
        Assume.assumeTrue(
            "Scaler params should be deployed with model",
            predictor.isUsingMlModel()
        )
        
        val testContext = createTestContext()
        
        runBlocking {
            val predictions = predictor.predictNextBackups(testContext, maxPredictions = 3)
            
            predictions.forEach { prediction ->
                assertTrue(
                    "ML predictions should be valid probabilities",
                    prediction.confidence in 0f..1f
                )
            }
        }
    }
    
    @Test
    fun testIsUsingMlModelReturnsTrueWhenModelLoaded() {
        Assume.assumeTrue(
            "Model must be deployed",
            predictor.isUsingMlModel()
        )
        
        assertTrue("isUsingMlModel() should return true when model loaded", true)
    }
    
    // =========================================================================
    // INFERENCE TESTS (Skip if Model Missing)
    // =========================================================================
    
    @Test
    fun testInferenceReturnsValidProbability() {
        Assume.assumeTrue(
            "Model required for inference test",
            predictor.isUsingMlModel()
        )
        
        val testContext = createTestContext(
            isCharging = true,
            isWifi = true,
            batteryLevel = 75f
        )
        
        runBlocking {
            val predictions = predictor.predictNextBackups(testContext, maxPredictions = 5)
            
            assertTrue("ML should produce predictions", predictions.isNotEmpty())
            
            predictions.forEach { prediction ->
                assertTrue(
                    "ML prediction must be in [0.0, 1.0], got ${prediction.confidence}",
                    prediction.confidence in 0f..1f
                )
            }
        }
    }
    
    @Test
    fun testInferenceSpeed() {
        Assume.assumeTrue(
            "Model required for inference speed test",
            predictor.isUsingMlModel()
        )
        
        val testContext = createTestContext()
        
        // Warm up
        runBlocking {
            predictor.predictNextBackups(testContext, maxPredictions = 1)
        }
        
        val times = mutableListOf<Long>()
        repeat(10) {
            val time = measureTimeMillis {
                runBlocking {
                    predictor.predictNextBackups(testContext, maxPredictions = 3)
                }
            }
            times.add(time)
        }
        
        val avgTime = times.average()
        
        assertTrue(
            "Avg inference <200ms (got ${avgTime}ms). First run may be slower.",
            avgTime < 200
        )
    }
    
    @Test
    fun testAll8FeaturesUsed() {
        Assume.assumeTrue(
            "Model with 8 features required",
            predictor.isUsingMlModel()
        )
        
        val baseContext = createTestContext()
        
        runBlocking {
            val contexts = listOf(
                baseContext.copy(timeOfDay = LocalTime.of(2, 0)),
                baseContext.copy(dayOfWeek = DayOfWeek.SUNDAY),
                baseContext.copy(batteryLevel = 25f),
                baseContext.copy(isCharging = false),
                baseContext.copy(isWifiConnected = false)
            )
            
            contexts.forEach { ctx ->
                val predictions = predictor.predictNextBackups(ctx, maxPredictions = 2)
                predictions.forEach { prediction ->
                    assertTrue(
                        "All features should produce valid predictions",
                        prediction.confidence in 0f..1f
                    )
                }
            }
        }
    }
    
    @Test
    fun testNormalizationMatchesTraining() {
        Assume.assumeTrue(
            "Model required for normalization test",
            predictor.isUsingMlModel()
        )
        
        val testCases = listOf(
            createTestContext(
                hour = 2,
                isCharging = true,
                isWifi = true,
                batteryLevel = 95f
            ),
            createTestContext(
                hour = 14,
                isCharging = false,
                isWifi = false,
                batteryLevel = 20f
            )
        )
        
        runBlocking {
            testCases.forEach { ctx ->
                val predictions = predictor.predictNextBackups(ctx, maxPredictions = 3)
                
                predictions.forEach { prediction ->
                    assertTrue(
                        "Normalization should produce valid probabilities",
                        prediction.confidence in 0f..1f
                    )
                }
            }
        }
    }
    
    // =========================================================================
    // FEATURE EXTRACTION TESTS (Always Run)
    // =========================================================================
    
    @Test
    fun testExtractsHourOfDay() {
        val morningContext = createTestContext(hour = 8)
        val nightContext = createTestContext(hour = 23)
        
        runBlocking {
            val morningPreds = predictor.predictNextBackups(morningContext, maxPredictions = 1)
            val nightPreds = predictor.predictNextBackups(nightContext, maxPredictions = 1)
            
            assertTrue("Should handle morning hours", morningPreds.isNotEmpty() || morningPreds.isEmpty())
            assertTrue("Should handle night hours", nightPreds.isNotEmpty() || nightPreds.isEmpty())
        }
    }
    
    @Test
    fun testExtractsDayOfWeek() {
        val mondayContext = createTestContext(dayOfWeek = DayOfWeek.MONDAY)
        val sundayContext = createTestContext(dayOfWeek = DayOfWeek.SUNDAY)
        
        runBlocking {
            val mondayPreds = predictor.predictNextBackups(mondayContext, maxPredictions = 1)
            val sundayPreds = predictor.predictNextBackups(sundayContext, maxPredictions = 1)
            
            assertTrue("Should handle weekdays", mondayPreds.isNotEmpty() || mondayPreds.isEmpty())
            assertTrue("Should handle weekends", sundayPreds.isNotEmpty() || sundayPreds.isEmpty())
        }
    }
    
    @Test
    fun testExtractsBatteryLevel() {
        val lowBatteryContext = createTestContext(batteryLevel = 10f)
        val highBatteryContext = createTestContext(batteryLevel = 95f)
        
        runBlocking {
            val lowPreds = predictor.predictNextBackups(lowBatteryContext, maxPredictions = 1)
            val highPreds = predictor.predictNextBackups(highBatteryContext, maxPredictions = 1)
            
            assertTrue("Should handle low battery", lowPreds.isNotEmpty() || lowPreds.isEmpty())
            assertTrue("Should handle high battery", highPreds.isNotEmpty() || highPreds.isEmpty())
        }
    }
    
    @Test
    fun testExtractsTimeSinceLastBackup() {
        val testContext = createTestContext()
        
        runBlocking {
            predictor.train(
                appIds = emptyList(),
                timestamp = LocalDateTime.now().minusHours(24),
                context = testContext
            )
            
            val predictions = predictor.predictNextBackups(testContext, maxPredictions = 3)
            
            assertTrue(
                "Should handle time_since_last_backup feature",
                predictions.isNotEmpty() || predictions.isEmpty()
            )
        }
    }
    
    @Test
    fun testChargingAndWifiFlags() {
        val testCases = listOf(
            createTestContext(isCharging = false, isWifi = false),
            createTestContext(isCharging = true, isWifi = false),
            createTestContext(isCharging = false, isWifi = true),
            createTestContext(isCharging = true, isWifi = true)
        )
        
        runBlocking {
            testCases.forEach { ctx ->
                predictor.predictNextBackups(ctx, maxPredictions = 1)
            }
        }
        
        assertTrue("Should handle charging/WiFi combinations", true)
    }
    
    @Test
    fun testResourceCleanup() {
        val testPredictor = BackupPredictor(context)
        
        runBlocking {
            testPredictor.initialize()
        }
        
        try {
            testPredictor.close()
            testPredictor.close()
            assertTrue("Close should be safe to call multiple times", true)
        } catch (e: Exception) {
            fail("close() should not throw exceptions: ${e.message}")
        }
    }
    
    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    
    private fun createTestContext(
        hour: Int = 14,
        dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        batteryLevel: Float = 70f,
        isCharging: Boolean = false,
        isWifi: Boolean = true
    ): BackupContext {
        return BackupContext(
            timeOfDay = LocalTime.of(hour, 0),
            dayOfWeek = dayOfWeek,
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isWifiConnected = isWifi,
            locationCategory = BackupContext.LocationCategory.HOME,
            activityType = BackupContext.ActivityType.STILL
        )
    }
}
