# ML Model Training Instructions

## Prerequisites

Install Python dependencies:
```bash
pip3 install -r requirements.txt
```

Or individually:
```bash
pip3 install tensorflow pandas scikit-learn numpy
```

## Training the Model

Run the training script:
```bash
cd ml_training
python3 train_backup_predictor.py
```

Expected runtime: ~2-3 minutes on CPU

## Output Files

After successful training, you'll have:

1. **backup_predictor.tflite** - Optimized model for Android (<1MB)
2. **scaler_params.json** - Feature normalization parameters
3. **backup_predictor_keras.h5** - Original Keras model (for future retraining)

## Success Criteria

The script validates:
- ✅ AUC > 0.75 on test set
- ✅ Inference time < 100ms per prediction
- ✅ Model size < 1MB
- ✅ Output probabilities in range [0.0, 1.0]

## Deployment

After training completes successfully:

1. Copy model to Android assets:
   ```bash
   cp backup_predictor.tflite ../app/src/main/assets/
   ```

2. Copy scaler params to resources:
   ```bash
   cp scaler_params.json ../app/src/main/res/raw/
   ```

3. Update BackupPredictor.kt to use 8 features (see implementation notes)

## Model Architecture

```
Input(8 features) 
  → Dense(32, ReLU, He initialization)
  → Dense(16, ReLU, He initialization)
  → Dense(1, Sigmoid)
```

**Optimization**: Dynamic Range Quantization (4x size reduction)

## 8 Input Features

1. hour_of_day (0-23, normalized)
2. day_of_week (0-6, normalized)
3. battery_level (0-100, normalized)
4. is_charging (0 or 1)
5. is_wifi_connected (0 or 1)
6. location_category (0-2, normalized)
7. activity_type (0-1, normalized)
8. time_since_last_backup (0-168 hours, normalized)

## Research Citations

- TensorFlow Binary Classification: https://www.tensorflow.org/guide/core/logistic_regression_core
- Model Quantization: https://www.tensorflow.org/model_optimization/guide/quantization/post_training
- Feature Engineering: https://www.synogize.io/feature-engineering-for-time-series-data-a-deep-yet-intuitive-guide
- GPU Delegate: https://ai.google.dev/edge/litert/android/gpu
