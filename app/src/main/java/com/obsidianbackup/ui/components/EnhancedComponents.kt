package com.obsidianbackup.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.obsidianbackup.ui.utils.rememberHapticFeedback

/**
 * Enhanced button with haptic feedback and scale animation
 */
@Composable
fun EnhancedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.medium()
        }
    }
    
    Button(
        onClick = {
            haptic.light()
            onClick()
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Floating action button with breathing animation
 */
@Composable
fun EnhancedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "fab_scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.heavy()
        }
    }
    
    FloatingActionButton(
        onClick = {
            haptic.medium()
            onClick()
        },
        modifier = modifier.scale(scale),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Icon button with ripple and haptic feedback
 */
@Composable
fun EnhancedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "icon_scale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.light()
        }
    }
    
    IconButton(
        onClick = {
            haptic.virtualKey()
            onClick()
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Card with press animation and elevation change
 */
@Composable
fun EnhancedCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = tween(150),
        label = "card_elevation"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed && onClick != null) {
            haptic.light()
        }
    }
    
    if (onClick != null) {
        Card(
            onClick = {
                haptic.virtualKey()
                onClick()
            },
            modifier = modifier.scale(scale),
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            interactionSource = interactionSource,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            content = content
        )
    }
}

/**
 * Switch with haptic feedback
 */
@Composable
fun EnhancedSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = rememberHapticFeedback()
    
    Switch(
        checked = checked,
        onCheckedChange = {
            if (it) {
                haptic.success()
            } else {
                haptic.light()
            }
            onCheckedChange?.invoke(it)
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * Checkbox with haptic feedback
 */
@Composable
fun EnhancedCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = rememberHapticFeedback()
    
    Checkbox(
        checked = checked,
        onCheckedChange = {
            if (it) {
                haptic.medium()
            } else {
                haptic.light()
            }
            onCheckedChange?.invoke(it)
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * Slider with haptic feedback on value change
 */
@Composable
fun EnhancedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val haptic = rememberHapticFeedback()
    var lastValue by remember { mutableStateOf(value) }
    
    Slider(
        value = value,
        onValueChange = { newValue ->
            // Haptic on significant change or step
            if (steps > 0 && newValue != lastValue) {
                haptic.virtualKey()
            } else if (kotlin.math.abs(newValue - lastValue) > 0.1f) {
                haptic.virtualKey()
                lastValue = newValue
            }
            onValueChange(newValue)
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = {
            haptic.light()
            onValueChangeFinished?.invoke()
        },
        colors = colors,
        interactionSource = interactionSource
    )
}
