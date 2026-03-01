package com.obsidianbackup.wear.data

import android.util.Log
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Listens for Data Layer events from the phone app
 */
@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var dataLayerRepository: DataLayerRepository

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            val uri = event.dataItem.uri
            Log.d(TAG, "Data changed: ${uri.path}")

            when (uri.path) {
                DataLayerPaths.BACKUP_STATUS_PATH -> {
                    val data = event.dataItem.data
                    if (data != null) {
                        dataLayerRepository.updateBackupStatus(data)
                    }
                }
                DataLayerPaths.BACKUP_PROGRESS_PATH -> {
                    val data = event.dataItem.data
                    if (data != null) {
                        dataLayerRepository.updateBackupProgress(data)
                    }
                }
                DataLayerPaths.SETTINGS_PATH -> {
                    val data = event.dataItem.data
                    if (data != null) {
                        dataLayerRepository.updateSettings(data)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Message received: ${messageEvent.path}")
        
        when (messageEvent.path) {
            DataLayerPaths.BACKUP_STATUS_PATH -> {
                dataLayerRepository.updateBackupStatus(messageEvent.data)
            }
            DataLayerPaths.BACKUP_PROGRESS_PATH -> {
                dataLayerRepository.updateBackupProgress(messageEvent.data)
            }
        }
    }

    companion object {
        private const val TAG = "DataLayerListener"
    }
}
