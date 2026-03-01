package com.obsidianbackup.transfer

import android.content.Context
import com.obsidianbackup.storage.BackupCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wi-Fi transfer HTTP server — serves backup files over local network.
 *
 * Inspired by Titanium Backup's built-in HTTP server feature.
 * Starts a lightweight HTTP server on a configurable port that allows
 * browsing and downloading backups from any device on the same network.
 */
@Singleton
class WifiTransferServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupCatalog: BackupCatalog
) {
    companion object {
        private const val TAG = "[WifiTransfer]"
        private const val DEFAULT_PORT = 8080
        private const val SERVER_NAME = "ObsidianBackup Transfer Server"
    }

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    val isRunning: Boolean get() = running

    /**
     * Start the HTTP server on the given port.
     * Returns the server URL (e.g., http://192.168.1.100:8080).
     */
    suspend fun start(port: Int = DEFAULT_PORT, backupDir: File): String = withContext(Dispatchers.IO) {
        if (running) stop()

        val socket = ServerSocket(port)
        serverSocket = socket
        running = true

        // Accept connections in background
        Thread({
            Timber.d("$TAG Server started on port $port")
            while (running) {
                try {
                    val clientSocket = socket.accept()
                    Thread { handleClient(clientSocket, backupDir) }.start()
                } catch (e: Exception) {
                    if (running) Timber.w(e, "$TAG Accept error")
                }
            }
        }, "WifiTransferServer").start()

        val ip = getLocalIpAddress()
        "http://$ip:$port"
    }

    /**
     * Stop the HTTP server.
     */
    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Timber.w(e, "$TAG Error closing server")
        }
        serverSocket = null
        Timber.d("$TAG Server stopped")
    }

    private fun handleClient(socket: Socket, backupDir: File) {
        try {
            socket.use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = PrintWriter(client.getOutputStream(), true)

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0]
                val path = URLDecoder.decode(parts[1], "UTF-8")

                // Read headers (drain them)
                var line = reader.readLine()
                while (line != null && line.isNotEmpty()) {
                    line = reader.readLine()
                }

                if (method != "GET") {
                    sendResponse(writer, 405, "text/plain", "Method Not Allowed")
                    return
                }

                when {
                    path == "/" || path == "/index.html" -> serveIndex(writer, backupDir)
                    path.startsWith("/download/") -> serveFile(client, writer, backupDir, path.removePrefix("/download/"))
                    path == "/api/list" -> serveApiList(writer, backupDir)
                    else -> sendResponse(writer, 404, "text/plain", "Not Found")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "$TAG Client handler error")
        }
    }

    private fun serveIndex(writer: PrintWriter, backupDir: File) {
        val files = backupDir.listFiles()?.sortedBy { it.name } ?: emptyList()
        val html = buildString {
            append("<!DOCTYPE html><html><head>")
            append("<title>$SERVER_NAME</title>")
            append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
            append("<style>")
            append("body{font-family:system-ui;max-width:800px;margin:0 auto;padding:20px;background:#1a1a1a;color:#e0e0e0}")
            append("h1{color:#bb86fc}a{color:#03dac6;text-decoration:none}")
            append("a:hover{text-decoration:underline}")
            append(".file{padding:12px;border-bottom:1px solid #333;display:flex;justify-content:space-between}")
            append(".size{color:#888;font-size:0.9em}")
            append("</style></head><body>")
            append("<h1>🗄️ ObsidianBackup</h1>")
            append("<p>${files.size} backup files available</p>")
            for (file in files) {
                val encodedName = URLEncoder.encode(file.name, "UTF-8")
                val size = formatFileSize(file.length())
                append("<div class='file'>")
                append("<a href='/download/$encodedName'>${file.name}</a>")
                append("<span class='size'>$size</span>")
                append("</div>")
            }
            append("</body></html>")
        }
        sendResponse(writer, 200, "text/html", html)
    }

    private fun serveFile(socket: Socket, writer: PrintWriter, backupDir: File, fileName: String) {
        val file = File(backupDir, fileName)
        if (!file.exists() || !file.canonicalPath.startsWith(backupDir.canonicalPath)) {
            sendResponse(writer, 404, "text/plain", "File not found")
            return
        }

        val out = socket.getOutputStream()
        val header = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Length: ${file.length()}\r\n" +
            "Content-Disposition: attachment; filename=\"${file.name}\"\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray())
        file.inputStream().use { it.copyTo(out) }
        out.flush()
    }

    private fun serveApiList(writer: PrintWriter, backupDir: File) {
        val files = backupDir.listFiles()?.map { file ->
            """{"name":"${file.name}","size":${file.length()},"modified":${file.lastModified()}}"""
        } ?: emptyList()
        sendResponse(writer, 200, "application/json", "[${files.joinToString(",")}]")
    }

    private fun sendResponse(writer: PrintWriter, code: Int, contentType: String, body: String) {
        val status = when (code) {
            200 -> "OK"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "Error"
        }
        writer.print("HTTP/1.1 $code $status\r\n")
        writer.print("Content-Type: $contentType; charset=utf-8\r\n")
        writer.print("Content-Length: ${body.toByteArray().size}\r\n")
        writer.print("Server: $SERVER_NAME\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(body)
        writer.flush()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "localhost"
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "$TAG Cannot determine IP")
        }
        return "localhost"
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
