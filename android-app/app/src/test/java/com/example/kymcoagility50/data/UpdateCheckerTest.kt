package com.example.kymcoagility50.data

import com.example.kymcoagility50.data.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Test

/** Shapes taken from the real api.github.com/repos/.../releases/latest response. */
class UpdateCheckerTest {
    private fun release(tag: String, body: String, apkUrl: String = "https://github.com/achma-learning/kymco-agility-moto/releases/download/$tag/agility-companion.apk") = """
        {"tag_name": "$tag", "name": "Agility Companion ${tag.removePrefix("v")}", "body": "$body",
         "html_url": "https://github.com/achma-learning/kymco-agility-moto/releases/tag/$tag",
         "published_at": "2026-10-11T22:31:29Z", "assets": [
           {"name": "agility-companion-web.zip", "size": 12858859, "browser_download_url": "https://github.com/achma-learning/kymco-agility-moto/releases/download/$tag/agility-companion-web.zip"},
           {"name": "agility-companion.apk", "size": 69231896, "browser_download_url": "$apkUrl"}]}
    """.trimIndent()

    @Test fun readsVersionCodeFromTheReleaseBody() {
        val u = UpdateChecker.parse(release("v2.1", "Notes\\n\\n<!-- versionCode: 57 -->\\n"))
        assertEquals(57L, u.versionCode)
        assertEquals("2.1", u.versionName)
        assertEquals(69231896L, u.apkBytes)
        assertEquals("2026-10-11", u.publishedOn)
    }

    @Test fun olderReleasesFallBackToTheTag() {
        // v2.0.5 was published before the body carried the versionCode line.
        assertEquals(5L, UpdateChecker.parse(release("v2.0.5", "**Android app** — download it")).versionCode)
    }

    @Test(expected = IllegalStateException::class)
    fun aReleaseWithoutAnApkIsRejected() {
        UpdateChecker.parse(release("v2.0.6", "").replace("agility-companion.apk", "something-else.apk"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun onlyDownloadsFromThisRepository() {
        UpdateChecker.parse(release("v2.0.6", "", apkUrl = "https://example.com/agility-companion.apk"))
    }

    @Test(expected = IllegalStateException::class)
    fun unknownVersionIsRejected() {
        UpdateChecker.parse(release("latest-build", "no version here"))
    }
}
