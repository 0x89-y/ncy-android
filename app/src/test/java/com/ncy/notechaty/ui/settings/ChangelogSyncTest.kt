package com.ncy.notechaty.ui.settings

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The release notes exist twice: CHANGELOG.md for the repository, and
 * [CHANGELOG] for Settings → About. Nothing linked them, so the markdown was
 * updated while the app kept showing an older release — and it stayed that way
 * across several changes, because the only way to notice was to open About.
 *
 * The wording deliberately differs (the markdown groups by Added/Changed/Fixed,
 * the app shows one flat list), so only the release identity is compared.
 */
class ChangelogSyncTest {

    /** Gradle runs unit tests from the module directory, so the repo root is above. */
    private fun repoFile(name: String): File =
        sequenceOf(File(name), File("../$name"), File("../../$name"))
            .firstOrNull { it.isFile }
            ?: error("could not locate $name from ${File(".").absolutePath}")

    private val versionHeading =
        Regex("""^##\s*(\d+\.\d+\.\d+)\s*[—–-]\s*(\d{4}-\d{2}-\d{2})\s*$""")

    @Test
    fun `the app shows the same release as CHANGELOG_md`() {
        val heading = repoFile("CHANGELOG.md").readLines()
            .firstNotNullOfOrNull { versionHeading.find(it.trim()) }
            ?: error("no version heading found in CHANGELOG.md")
        val (version, date) = heading.destructured

        val top = CHANGELOG.first()
        assertEquals("in-app changelog version is behind CHANGELOG.md", version, top.version)
        assertEquals("in-app changelog date is behind CHANGELOG.md", date, top.date)
    }

    @Test
    fun `the app shows the version this build reports`() {
        val versionName = repoFile("app/build.gradle.kts").readLines()
            .firstNotNullOfOrNull {
                Regex("""versionName\s*=\s*"([^"]+)"""").find(it)?.groupValues?.get(1)
            }
            ?: error("no versionName found in app/build.gradle.kts")
        assertEquals(versionName, CHANGELOG.first().version)
    }

    @Test
    fun `entries stay newest-first with no repeated release`() {
        val versions = CHANGELOG.map { it.version }
        assertEquals("a release is listed twice", versions.distinct().size, versions.size)
        val dates = CHANGELOG.map { it.date }
        assertTrue("entries are not newest-first", dates == dates.sortedDescending())
    }
}
