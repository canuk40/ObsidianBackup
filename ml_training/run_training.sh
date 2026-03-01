#!/bin/bash
# Automated training script with dependency check

set -e

echo "==================================================================="
echo "ObsidianBackup ML Model Training"
echo "==================================================================="

# Check Python 3
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 not found. Please install Python 3.8+"
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"

# Check/install dependencies
echo "Checking dependencies..."
python3 -c "import tensorflow, pandas, sklearn, numpy" 2>/dev/null || {
    echo "Installing dependencies..."
    pip3 install -r requirements.txt
}

echo "✅ All dependencies installed"
echo ""

# Run training
echo "Starting model training..."
python3 train_backup_predictor.py

# Check outputs
if [ -f "backup_predictor.tflite" ] && [ -f "scaler_params.json" ]; then
    echo ""
    echo "✅ Training successful!"
    echo "📦 Generated files:"
    ls -lh backup_predictor.tflite scaler_params.json
    echo ""
    echo "Next steps:"
    echo "  1. Copy backup_predictor.tflite to app/src/main/assets/"
    echo "  2. Copy scaler_params.json to app/src/main/res/raw/"
    echo "  3. Update BackupPredictor.kt INPUT_SIZE to 8"
else
    echo "❌ Training failed - output files not found"
    exit 1
fi
