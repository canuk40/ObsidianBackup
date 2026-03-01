package com.obsidianbackup.ui.components.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * Lottie animation wrapper for backup progress
 */
@Composable
fun BackupProgressAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(BACKUP_PROGRESS_ANIMATION)
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(200.dp)
    )
}

/**
 * Success checkmark animation with bounce
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(SUCCESS_ANIMATION)
    )
    
    var isPlaying by remember { mutableStateOf(true) }
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = 1,
        speed = 1f
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
            onAnimationEnd()
        }
    }
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(120.dp)
    )
}

/**
 * Error shake animation
 */
@Composable
fun ErrorAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(ERROR_ANIMATION)
    )
    
    var isPlaying by remember { mutableStateOf(true) }
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = 1,
        speed = 1f
    )
    
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
            onAnimationEnd()
        }
    }
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(120.dp)
    )
}

/**
 * Cloud sync animation
 */
@Composable
fun CloudSyncAnimation(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(CLOUD_SYNC_ANIMATION)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(150.dp)
    )
}

/**
 * Empty state illustration
 */
@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(EMPTY_STATE_ANIMATION)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = 0.5f
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(200.dp)
    )
}

/**
 * Loading spinner animation
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    size: Int = 80
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(LOADING_ANIMATION)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = 1.5f
    )
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size.dp)
    )
}

/**
 * Pull to refresh indicator
 */
@Composable
fun PullToRefreshAnimation(
    pullProgress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.JsonString(PULL_TO_REFRESH_ANIMATION)
    )
    
    val progress = if (isRefreshing) {
        val animProgress by animateLottieCompositionAsState(
            composition = composition,
            isPlaying = true,
            iterations = LottieConstants.IterateForever
        )
        animProgress
    } else {
        pullProgress
    }
    
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(60.dp)
    )
}

// Simplified Lottie JSON animations (inline for easy deployment)
// In production, these should be separate .json files in assets/raw

private const val SUCCESS_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 60,
  "w": 200,
  "h": 200,
  "nm": "Success",
  "ddd": 0,
  "assets": [],
  "layers": [
    {
      "ddd": 0,
      "ind": 1,
      "ty": 4,
      "nm": "Checkmark",
      "sr": 1,
      "ks": {
        "o": { "a": 0, "k": 100 },
        "r": { "a": 0, "k": 0 },
        "p": { "a": 0, "k": [100, 100, 0] },
        "a": { "a": 0, "k": [0, 0, 0] },
        "s": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0, 0, 100], "h": 1 },
            { "t": 30, "s": [120, 120, 100], "e": [173, 110, 34] },
            { "t": 45, "s": [90, 90, 100], "e": [173, 110, 34] },
            { "t": 60, "s": [100, 100, 100] }
          ]
        }
      },
      "ao": 0,
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ind": 0,
              "ty": "sh",
              "ks": {
                "a": 0,
                "k": {
                  "i": [[0, 0], [0, 0], [0, 0]],
                  "o": [[0, 0], [0, 0], [0, 0]],
                  "v": [[-20, 0], [-5, 15], [25, -20]],
                  "c": false
                }
              }
            },
            {
              "ty": "st",
              "c": { "a": 0, "k": [0.298, 0.686, 0.314, 1] },
              "o": { "a": 0, "k": 100 },
              "w": { "a": 0, "k": 8 },
              "lc": 2,
              "lj": 2
            },
            {
              "ty": "tr",
              "p": { "a": 0, "k": [0, 0] },
              "a": { "a": 0, "k": [0, 0] },
              "s": { "a": 0, "k": [100, 100] },
              "r": { "a": 0, "k": 0 },
              "o": { "a": 0, "k": 100 }
            }
          ]
        }
      ],
      "ip": 0,
      "op": 60,
      "st": 0
    }
  ]
}
"""

private const val ERROR_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 60,
  "w": 200,
  "h": 200,
  "nm": "Error",
  "layers": [
    {
      "ty": 4,
      "nm": "X",
      "ks": {
        "o": { "a": 0, "k": 100 },
        "p": {
          "a": 1,
          "k": [
            { "t": 0, "s": [100, 100, 0], "h": 1 },
            { "t": 10, "s": [90, 100, 0], "h": 1 },
            { "t": 20, "s": [110, 100, 0], "h": 1 },
            { "t": 30, "s": [90, 100, 0], "h": 1 },
            { "t": 40, "s": [110, 100, 0], "h": 1 },
            { "t": 50, "s": [100, 100, 0] }
          ]
        },
        "s": { "a": 0, "k": [100, 100, 100] }
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "sh",
              "ks": {
                "a": 0,
                "k": {
                  "i": [[0, 0], [0, 0]],
                  "o": [[0, 0], [0, 0]],
                  "v": [[-20, -20], [20, 20]],
                  "c": false
                }
              }
            },
            {
              "ty": "sh",
              "ks": {
                "a": 0,
                "k": {
                  "i": [[0, 0], [0, 0]],
                  "o": [[0, 0], [0, 0]],
                  "v": [[20, -20], [-20, 20]],
                  "c": false
                }
              }
            },
            {
              "ty": "st",
              "c": { "a": 0, "k": [0.957, 0.263, 0.212, 1] },
              "o": { "a": 0, "k": 100 },
              "w": { "a": 0, "k": 8 },
              "lc": 2,
              "lj": 2
            }
          ]
        }
      ]
    }
  ]
}
"""

private const val BACKUP_PROGRESS_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 120,
  "w": 200,
  "h": 200,
  "nm": "Backup",
  "layers": [
    {
      "ty": 4,
      "nm": "Circle",
      "ks": {
        "o": { "a": 0, "k": 100 },
        "p": { "a": 0, "k": [100, 100, 0] },
        "s": { "a": 0, "k": [100, 100, 100] }
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "el",
              "p": { "a": 0, "k": [0, 0] },
              "s": { "a": 0, "k": [80, 80] }
            },
            {
              "ty": "st",
              "c": { "a": 0, "k": [0.259, 0.522, 0.957, 1] },
              "o": { "a": 0, "k": 100 },
              "w": { "a": 0, "k": 6 }
            },
            {
              "ty": "tr",
              "r": {
                "a": 1,
                "k": [
                  { "t": 0, "s": [0] },
                  { "t": 120, "s": [360] }
                ]
              }
            }
          ]
        }
      ]
    }
  ]
}
"""

private const val CLOUD_SYNC_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 120,
  "w": 200,
  "h": 200,
  "nm": "CloudSync",
  "layers": [
    {
      "ty": 4,
      "nm": "Cloud",
      "ks": {
        "o": { "a": 0, "k": 100 },
        "p": {
          "a": 1,
          "k": [
            { "t": 0, "s": [100, 90, 0] },
            { "t": 60, "s": [100, 110, 0] },
            { "t": 120, "s": [100, 90, 0] }
          ]
        }
      }
    }
  ]
}
"""

private const val EMPTY_STATE_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 180,
  "w": 200,
  "h": 200,
  "nm": "Empty",
  "layers": [
    {
      "ty": 4,
      "nm": "Box",
      "ks": {
        "o": { "a": 0, "k": 80 },
        "p": { "a": 0, "k": [100, 100, 0] },
        "s": { "a": 0, "k": [100, 100, 100] }
      }
    }
  ]
}
"""

private const val LOADING_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 120,
  "w": 100,
  "h": 100,
  "nm": "Loading",
  "layers": [
    {
      "ty": 4,
      "nm": "Spinner",
      "ks": {
        "r": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0] },
            { "t": 120, "s": [360] }
          ]
        }
      }
    }
  ]
}
"""

private const val PULL_TO_REFRESH_ANIMATION = """
{
  "v": "5.7.4",
  "fr": 60,
  "ip": 0,
  "op": 60,
  "w": 100,
  "h": 100,
  "nm": "PullRefresh",
  "layers": [
    {
      "ty": 4,
      "nm": "Arrow",
      "ks": {
        "r": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0] },
            { "t": 60, "s": [360] }
          ]
        }
      }
    }
  ]
}
"""
