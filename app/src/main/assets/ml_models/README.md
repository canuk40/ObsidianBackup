# ML Models Directory

This directory contains TensorFlow Lite models for on-device machine learning.

## Models

### backup_predictor.tflite
Neural network model for predicting optimal backup times.

- **Input**: 7 features (device context)
- **Output**: Probability score (0-1)
- **Size**: ~50KB
- **Training**: See /ml_training/train_backup_predictor.py

## Training Your Own Model

1. Use ObsidianBackup for several weeks to collect data
2. Export analytics data from the app
3. Run training script in /ml_training/ directory
4. Place trained model here

## Model Format

TensorFlow Lite (.tflite) format with dynamic range quantization for optimal performance on mobile devices.

