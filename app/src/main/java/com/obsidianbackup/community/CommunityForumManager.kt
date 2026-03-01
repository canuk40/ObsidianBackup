package com.obsidianbackup.community

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.obsidianbackup.logging.ObsidianLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages community forum links and integrations
 */
@Singleton
class CommunityForumManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: ObsidianLogger
) {
    
    fun openDiscord() {
        openUrl("https://discord.com/channels/1474784278415016077/1475176547823648971")
    }
    
    fun openReddit() {
        openUrl("https://reddit.com/r/obsidianbackup")
    }
    
    fun openGitHub() {
        openUrl("https://github.com/obsidianbackup/obsidianbackup")
    }
    
    fun openDocumentation() {
        openUrl("https://docs.obsidianbackup.app")
    }
    
    fun openSupportPage() {
        openUrl("https://obsidianbackup.app/support")
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            logger.i("CommunityForumManager", "Opened URL: $url")
        } catch (e: Exception) {
            logger.e("CommunityForumManager", "Failed to open URL: $url", e)
        }
    }
    
    fun getCommunityLinks(): List<CommunityLink> {
        return listOf(
            CommunityLink(
                name = "Discord Community",
                description = "Join our Discord server for real-time chat and support",
                url = "https://discord.com/channels/1474784278415016077/1475176547823648971",
                icon = "discord"
            ),
            CommunityLink(
                name = "Reddit",
                description = "Discuss features and share tips on our subreddit",
                url = "https://reddit.com/r/obsidianbackup",
                icon = "reddit"
            ),
            CommunityLink(
                name = "GitHub",
                description = "Report bugs, request features, or contribute code",
                url = "https://github.com/obsidianbackup/obsidianbackup",
                icon = "github"
            ),
            CommunityLink(
                name = "Documentation",
                description = "Comprehensive guides and tutorials",
                url = "https://docs.obsidianbackup.app",
                icon = "docs"
            ),
            CommunityLink(
                name = "Support",
                description = "Get help with troubleshooting and FAQs",
                url = "https://obsidianbackup.app/support",
                icon = "support"
            )
        )
    }
}

data class CommunityLink(
    val name: String,
    val description: String,
    val url: String,
    val icon: String
)
