package com.obsidianbackup.wear.tiles

import android.content.Context
import androidx.wear.protolayout.*
import androidx.wear.protolayout.material.Button
import androidx.wear.protolayout.material.ButtonColors
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders
import com.google.android.gms.wearable.Wearable
import com.obsidianbackup.wear.R
import com.obsidianbackup.wear.data.DataLayerPaths
import com.obsidianbackup.wear.data.MessageTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Wear OS Tile for quick backup actions
 * Provides one-tap backup trigger from watch face
 */
class BackupTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(tileLayout(this))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCES_VERSION)
            .build()
    }

    private fun tileLayout(context: Context): LayoutElementBuilders.LayoutElement {
        return PrimaryLayout.Builder(DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(192)
            .setScreenHeightDp(192)
            .build())
            .setContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        Text.Builder(context, "ObsidianBackup")
                            .setTypography(Typography.TYPOGRAPHY_TITLE3)
                            .build()
                    )
                    .addContent(
                        LayoutElementBuilders.Spacer.Builder()
                            .setHeight(DimensionBuilders.dp(8f))
                            .build()
                    )
                    .addContent(
                        CompactChip.Builder(
                            context,
                            "Backup Now",
                            ModifiersBuilders.Clickable.Builder()
                                .setOnClick(ActionBuilders.LaunchAction.Builder()
                                    .setAndroidActivity(
                                        ActionBuilders.AndroidActivity.Builder()
                                            .setPackageName(context.packageName)
                                            .setClassName("com.obsidianbackup.MainActivity")
                                            .build()
                                    )
                                    .build())
                                .setId("backup_trigger")
                                .build(),
                            DeviceParametersBuilders.DeviceParameters.Builder()
                                .setScreenWidthDp(192)
                                .setScreenHeightDp(192)
                                .build()
                        )
                            .setChipColors(
                                ChipColors.primaryChipColors(ColorBuilders.argb(0xFF4CAF50.toInt()))
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}
