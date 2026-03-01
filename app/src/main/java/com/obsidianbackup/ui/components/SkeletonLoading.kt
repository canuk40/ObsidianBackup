package com.obsidianbackup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading component with shimmer effect
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColorShades = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColorShades,
                    start = Offset(shimmerTranslate - 200f, shimmerTranslate - 200f),
                    end = Offset(shimmerTranslate, shimmerTranslate)
                )
            )
    )
}

/**
 * Skeleton loading for app list item
 */
@Composable
fun AppItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon skeleton
        SkeletonBox(
            modifier = Modifier.size(48.dp),
            shape = CircleShape
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // App name skeleton
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Package name skeleton
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
            )
        }
        
        // Checkbox skeleton
        SkeletonBox(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

/**
 * Skeleton loading for backup card
 */
@Composable
fun BackupCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Backup name
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(24.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Date
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                )
            }
            
            // Size indicator
            SkeletonBox(
                modifier = Modifier
                    .width(60.dp)
                    .height(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress bar
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

/**
 * Skeleton loading for dashboard stats
 */
@Composable
fun DashboardStatsSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                )
                
                SkeletonBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(20.dp)
                )
            }
        }
    }
}

/**
 * Full page loading skeleton for apps screen
 */
@Composable
fun AppsScreenSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(8) {
            AppItemSkeleton()
        }
    }
}

/**
 * Full page loading skeleton for backups screen
 */
@Composable
fun BackupsScreenSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            BackupCardSkeleton()
        }
    }
}

/**
 * Generic skeleton list
 */
@Composable
fun SkeletonList(
    itemCount: Int = 5,
    itemContent: @Composable () -> Unit = {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
) {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(itemCount) {
            itemContent()
        }
    }
}
