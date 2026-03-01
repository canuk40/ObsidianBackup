#!/usr/bin/env python3
"""TensorFlow Lite Backup Predictor Training Script"""
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import roc_auc_score
import json
import sys
from datetime import datetime

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)
tf.random.set_seed(RANDOM_SEED)

INPUT_FEATURES = 8
HIDDEN_LAYER_1 = 32
HIDDEN_LAYER_2 = 16
EPOCHS = 50
BATCH_SIZE = 32
LEARNING_RATE = 0.001
VALIDATION_SPLIT = 0.15
TEST_SPLIT = 0.15
TOTAL_SAMPLES = 10000

BASE_SUCCESS_RATE = 0.50
NIGHT_BOOST = 0.35
CHARGING_BOOST = 0.25
WIFI_BOOST = 0.20
HIGH_BATTERY_BOOST = 0.15
WEEKEND_BOOST = 0.10
HOME_BOOST = 0.10
STATIONARY_BOOST = 0.05
NIGHT_START = 22
NIGHT_END = 6

def generate_data(n):
    print(f"Generating {n} samples...")
    df = pd.DataFrame({
        'hour': np.random.randint(0, 24, n),
        'day': np.random.randint(0, 7, n),
        'battery': np.random.beta(3, 1.5, n) * 100,
        'charging': np.random.choice([0, 1], n, p=[0.7, 0.3]),
        'wifi': np.random.choice([0, 1], n, p=[0.4, 0.6]),
        'location': np.random.choice([0, 1, 2], n, p=[0.4, 0.25, 0.35]),
        'activity': np.random.choice([0, 1], n, p=[0.3, 0.7]),
        'time_since': np.clip(np.random.exponential(24, n), 0, 168)
    })
    
    prob = np.full(n, BASE_SUCCESS_RATE)
    prob[(df['hour'] >= NIGHT_START) | (df['hour'] < NIGHT_END)] += NIGHT_BOOST
    prob[df['charging'] == 1] += CHARGING_BOOST
    prob[df['wifi'] == 1] += WIFI_BOOST
    prob[df['battery'] > 50] += HIGH_BATTERY_BOOST
    prob[df['day'] >= 5] += WEEKEND_BOOST
    prob[df['location'] == 2] += HOME_BOOST
    prob[df['activity'] == 1] += STATIONARY_BOOST
    prob = np.clip(prob, 0, 1)
    df['label'] = np.random.binomial(1, prob)
    print(f"Success rate: {df['label'].mean():.2%}")
    return df

def normalize(df, scaler=None):
    cols = ['hour', 'day', 'battery', 'charging', 'wifi', 'location', 'activity', 'time_since']
    X = df[cols].values
    if scaler is None:
        scaler = StandardScaler()
        X = scaler.fit_transform(X)
    else:
        X = scaler.transform(X)
    return X, scaler

def build():
    model = keras.Sequential([
        keras.layers.Input((INPUT_FEATURES,)),
        keras.layers.Dense(HIDDEN_LAYER_1, activation='relu', kernel_initializer='he_normal'),
        keras.layers.Dense(HIDDEN_LAYER_2, activation='relu', kernel_initializer='he_normal'),
        keras.layers.Dense(1, activation='sigmoid')
    ])
    model.compile(optimizer=keras.optimizers.Adam(LEARNING_RATE),
                  loss='binary_crossentropy',
                  metrics=['accuracy', keras.metrics.AUC(name='auc')])
    return model

def convert(model, path):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite = converter.convert()
    with open(path, 'wb') as f:
        f.write(tflite)
    return len(tflite)

def test_tflite(path, X, y):
    interp = tf.lite.Interpreter(path)
    interp.allocate_tensors()
    inp = interp.get_input_details()[0]
    out = interp.get_output_details()[0]
    preds, times = [], []
    for i in range(len(X)):
        data = X[i:i+1].astype(np.float32)
        interp.set_tensor(inp['index'], data)
        t0 = datetime.now()
        interp.invoke()
        t1 = (datetime.now() - t0).total_seconds() * 1000
        times.append(t1)
        preds.append(interp.get_tensor(out['index'])[0][0])
    preds = np.array(preds)
    acc = ((preds > 0.5).astype(int) == y).mean()
    auc = roc_auc_score(y, preds)
    return {'acc': acc, 'auc': auc, 'time': np.mean(times)}

def save_scaler(sc, path):
    with open(path, 'w') as f:
        json.dump({'mean': sc.mean_.tolist(), 'scale': sc.scale_.tolist(),
                   'features': ['hour', 'day', 'battery', 'charging', 'wifi', 'location', 'activity', 'time_since']}, f, indent=2)

def main():
    print("=" * 80)
    print("BACKUP PREDICTOR TRAINING")
    print("=" * 80)
    
    df = generate_data(TOTAL_SAMPLES)
    X, scaler = normalize(df)
    y = df['label'].values
    
    split = int((1 - TEST_SPLIT) * len(X))
    X_train, X_test = X[:split], X[split:]
    y_train, y_test = y[:split], y[split:]
    print(f"Train: {len(X_train)}, Test: {len(X_test)}")
    
    model = build()
    model.summary()
    print("Training...")
    model.fit(X_train, y_train, epochs=EPOCHS, batch_size=BATCH_SIZE,
              validation_split=VALIDATION_SPLIT, verbose=1)
    
    loss, acc, auc = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test AUC: {auc:.4f}")
    
    model.save('backup_predictor_keras.h5')
    size = convert(model, 'backup_predictor.tflite')
    metrics = test_tflite('backup_predictor.tflite', X_test, y_test)
    save_scaler(scaler, 'scaler_params.json')
    
    print("\n" + "=" * 80)
    print("COMPLETE")
    print(f"Model: {size/1024:.1f} KB")
    print(f"AUC: {metrics['auc']:.4f} (>0.75 required)")
    print(f"Inference: {metrics['time']:.2f} ms (<100ms required)")
    
    ok = metrics['auc'] > 0.75 and metrics['time'] < 100 and size < 1024*1024
    print("REQUIREMENTS MET" if ok else "Requirements not met")
    return 0 if ok else 1

if __name__ == '__main__':
    sys.exit(main())
