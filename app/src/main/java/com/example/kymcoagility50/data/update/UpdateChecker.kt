package com.example.kymcoagility50.data.update

import com.example.kymcoagility50.privacy.NetworkGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tag: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val pageUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val url: String
)

/** The newest APK on GitHub Releases, as the Settings "Check for updates" button sees it. */
data class AppUpdate(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val apkBytes: Long,
    val pageUrl: String,
    val publishedOn: String?
)

/**
 * Asks GitHub which release is the latest. Called only from a tap (see PRIVACY.md); the
 * request goes to api.github.com and carries nothing about the owner.
 */
object UpdateChecker {
    private const val REPO = "achma-learning/kymco-agility-moto"
    const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
    /** The release workflow attaches the APK under this exact name. */
    const val APK_NAME = "agility-companion.apk"
    private const val DOWNLOAD_PREFIX = "https://github.com/$REPO/releases/download/"

    private val json = Json { ignoreUnknownKeys = true }
    // The release body carries "<!-- versionCode: 57 -->" (build-and-release.yml). Releases
    // made before that line existed are tagged v2.0.<versionCode>, so fall back to the tag.
    private val BODY_CODE = Regex("""<!--\s*versionCode:\s*(\d+)\s*-->""")
    private val TAG_CODE = Regex("""^v?\d+\.\d+\.(\d+)$""")

    suspend fun latest(): Result<AppUpdate> = NetworkGuard.userRequested {
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    useCaches = false
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "agility-companion-android")
                }
                try {
                    when (val code = conn.responseCode) {
                        200 -> parse(conn.inputStream.bufferedReader().use { it.readText() })
                        404 -> error("No release published yet")
                        403, 429 -> error("GitHub is rate-limiting this network; try again in an hour")
                        else -> error("Update check failed ($code)")
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    fun parse(text: String): AppUpdate {
        val release = json.decodeFromString(GitHubRelease.serializer(), text)
        val apk = release.assets.firstOrNull { it.name == APK_NAME } ?: error("This release has no APK")
        // Only ever download from this repository's own release files.
        require(apk.url.startsWith(DOWNLOAD_PREFIX)) { "Unexpected download location" }
        val code = BODY_CODE.find(release.body.orEmpty())?.groupValues?.get(1)?.toLong()
            ?: TAG_CODE.find(release.tag)?.groupValues?.get(1)?.toLong()
            ?: error("Can't tell this release's version")
        return AppUpdate(
            versionCode = code,
            versionName = release.tag.removePrefix("v"),
            apkUrl = apk.url,
            apkBytes = apk.size,
            pageUrl = release.pageUrl,
            publishedOn = release.publishedAt?.take(10)
        )
    }
}
