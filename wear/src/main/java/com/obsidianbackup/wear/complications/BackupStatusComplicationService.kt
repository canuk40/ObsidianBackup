package com.obsidianbackup.wear.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.obsidianbackup.wear.R
import com.obsidianbackup.wear.data.DataLayerRepository
import com.obsidianbackup.wear.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Complication data source for watch face integration
 * Shows backup status and allows tap-to-open functionality
 */
@AndroidEntryPoint
class BackupStatusComplicationService : ComplicationDataSourceService() {

    @Inject
    lateinit var dataLayerRepository: DataLayerRepository

    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val status = dataLayerRepository.backupStatus.value
        
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val complicationData = when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val text = if (status.isRunning) {
                    "Running"
                } else if (status.lastBackupSuccess) {
                    "OK"
                } else {
                    "Failed"
                }
                
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder("Backup status: $text").build()
                )
                    .setTapAction(pendingIntent)
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(
                            Icon.createWithResource(this, R.drawable.ic_backup)
                        ).build()
                    )
                    .build()
            }
            
            ComplicationType.LONG_TEXT -> {
                val lastBackupText = if (status.lastBackupTime > 0) {
                    dateFormat.format(Date(status.lastBackupTime))
                } else {
                    "Never"
                }
                
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Last: $lastBackupText").build(),
                    contentDescription = PlainComplicationText.Builder("Last backup: $lastBackupText").build()
                )
                    .setTapAction(pendingIntent)
                    .setTitle(PlainComplicationText.Builder("Backup").build())
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(
                            Icon.createWithResource(this, R.drawable.ic_backup)
                        ).build()
                    )
                    .build()
            }
            
            ComplicationType.RANGED_VALUE -> {
                val progress = dataLayerRepository.backupProgress.value
                val value = if (status.isRunning) progress.percentage.toFloat() else 100f
                
                RangedValueComplicationData.Builder(
                    value = value,
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder("Backup progress: ${value.toInt()}%").build()
                )
                    .setTapAction(pendingIntent)
                    .setText(PlainComplicationText.Builder("${value.toInt()}%").build())
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(
                            Icon.createWithResource(this, R.drawable.ic_backup)
                        ).build()
                    )
                    .build()
            }
            
            ComplicationType.SMALL_IMAGE -> {
                val iconRes = when {
                    status.isRunning -> R.drawable.ic_backup_running
                    status.lastBackupSuccess -> R.drawable.ic_backup_success
                    else -> R.drawable.ic_backup_failed
                }
                
                SmallImageComplicationData.Builder(
                    smallImage = SmallImage.Builder(
                        Icon.createWithResource(this, iconRes),
                        SmallImageType.ICON
                    ).build(),
                    contentDescription = PlainComplicationText.Builder("Backup status").build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }
            
            else -> {
                NoDataComplicationData()
            }
        }

        listener.onComplicationData(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("OK").build(),
                    contentDescription = PlainComplicationText.Builder("Backup status: OK").build()
                )
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(
                            Icon.createWithResource(this, R.drawable.ic_backup)
                        ).build()
                    )
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder("Last: 14:30").build(),
                    contentDescription = PlainComplicationText.Builder("Last backup: 14:30").build()
                )
                    .setTitle(PlainComplicationText.Builder("Backup").build())
                    .build()
            }
            else -> null
        }
    }
}
