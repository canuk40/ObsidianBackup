package com.obsidianbackup.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianbackup.ui.theme.ObsidianColors
import com.obsidianbackup.ui.theme.Spacing

data class OssLicense(
    val name: String,
    val author: String,
    val version: String,
    val license: String,
    val url: String
)

private val APACHE_2_SUMMARY = """
Apache License, Version 2.0

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
""".trim()

private val GPL2_SUMMARY = """
GNU General Public License v2.0

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

Full license: https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
""".trim()

private val BSD3_SUMMARY = """
BSD 3-Clause License

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright notice.
2. Redistributions in binary form must reproduce the above copyright notice.
3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
""".trim()

private val MIT_SUMMARY = """
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
""".trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onNavigateBack: () -> Unit) {
    val licenses = remember {
        listOf(
            OssLicense("Kotlin", "JetBrains", "2.0.21", "Apache 2.0", "https://kotlinlang.org"),
            OssLicense("Jetpack Compose", "Google", "1.6.0", "Apache 2.0", "https://developer.android.com/jetpack/compose"),
            OssLicense("Hilt", "Google", "2.48", "Apache 2.0", "https://dagger.dev/hilt/"),
            OssLicense("Room", "Google", "2.6.1", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
            OssLicense("Health Connect", "Google", "1.1.0", "Apache 2.0", "https://developer.android.com/health-and-fitness/guides/health-connect"),
            OssLicense("TensorFlow Lite", "Google", "2.14.0", "Apache 2.0", "https://www.tensorflow.org/lite"),
            OssLicense("Shizuku", "RikkaApps", "13.1.5", "Apache 2.0", "https://github.com/RikkaApps/Shizuku"),
            OssLicense("OkHttp", "Square", "4.12.0", "Apache 2.0", "https://square.github.io/okhttp/"),
            OssLicense("Retrofit", "Square", "2.9.0", "Apache 2.0", "https://square.github.io/retrofit/"),
            OssLicense("Coil", "Coil Contributors", "2.5.0", "Apache 2.0", "https://coil-kt.github.io/coil/"),
            OssLicense("kotlinx.coroutines", "JetBrains", "1.7.3", "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
            OssLicense("kotlinx.serialization", "JetBrains", "1.6.2", "Apache 2.0", "https://github.com/Kotlin/kotlinx.serialization"),
            OssLicense("Timber", "Jake Wharton", "5.0.1", "Apache 2.0", "https://github.com/JakeWharton/timber"),
            OssLicense("DataStore", "Google", "1.0.0", "Apache 2.0", "https://developer.android.com/topic/libraries/architecture/datastore"),
            OssLicense("WorkManager", "Google", "2.9.0", "Apache 2.0", "https://developer.android.com/topic/libraries/architecture/workmanager"),
            OssLicense("Lifecycle", "Google", "2.7.0", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
            OssLicense("Navigation Compose", "Google", "2.7.6", "Apache 2.0", "https://developer.android.com/jetpack/compose/navigation"),
            OssLicense("Material Icons", "Google", "1.6.0", "Apache 2.0", "https://fonts.google.com/icons"),
            OssLicense("Security Crypto", "Google", "1.1.0-alpha06", "Apache 2.0", "https://developer.android.com/jetpack/androidx/releases/security"),
            OssLicense("Zstd", "Facebook", "1.5.5", "BSD 3-Clause", "https://facebook.github.io/zstd/"),
            OssLicense("SQLCipher for Android", "Zetetic LLC", "4.5.4", "BSD-style", "https://www.zetetic.net/sqlcipher/"),
            OssLicense("BusyBox for Android NDK", "osm0sis", "1.29.2", "GPL v2", "https://github.com/osm0sis/android-busybox-ndk"),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        item {
            Text(
                "ObsidianBackup uses the following open source libraries:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
        }

        items(licenses) { license ->
            LicenseCard(license)
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                "Thank you to all open source contributors!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LicenseCard(license: OssLicense) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    val licenseColor = when {
        license.license.startsWith("Apache") -> ObsidianColors.MoltenOrange
        license.license.startsWith("GPL") -> MaterialTheme.colorScheme.error
        license.license.startsWith("MIT") -> ObsidianColors.MoltenGold
        else -> MaterialTheme.colorScheme.secondary
    }

    val licenseText = when {
        license.license.startsWith("Apache") -> APACHE_2_SUMMARY
        license.license.startsWith("GPL v2") -> GPL2_SUMMARY
        license.license.startsWith("BSD") -> BSD3_SUMMARY
        license.license.startsWith("MIT") -> MIT_SUMMARY
        else -> "See project website for full license text."
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = license.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // License badge
                Surface(
                    color = licenseColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = license.license,
                        style = MaterialTheme.typography.labelSmall,
                        color = licenseColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Author — was using .outline (too dark), now uses onSurfaceVariant
            Text(
                text = "by ${license.author}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Version — same fix
            Text(
                text = "v${license.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

                // License text
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(Spacing.xs))

                // URL button
                OutlinedButton(
                    onClick = {
                        try { uriHandler.openUri(license.url) } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        license.url,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
