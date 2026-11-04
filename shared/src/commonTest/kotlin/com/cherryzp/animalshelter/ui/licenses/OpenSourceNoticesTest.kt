package com.cherryzp.animalshelter.ui.licenses

import com.cherryzp.animalshelter.ui.common.UiText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 오픈소스 고지 목록(5a O1)의 형식. */
class OpenSourceNoticesTest {
    private fun group(key: String): NoticeGroup = OpenSourceNotices.single { it.key == key }

    @Test
    fun `묶음마다 줄이 하나 이상이고 묶음 키는 겹치지 않는다`() {
        OpenSourceNotices.forEach { assertTrue(it.entries.isNotEmpty(), it.key) }
        val keys = OpenSourceNotices.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "$keys")
    }

    @Test
    fun `전문 경로는 files·licenses의 파일 중 하나, 없으면 라이선스 주소`() {
        OpenSourceNotices.forEach { g ->
            val file = g.textFile
            if (file == null) {
                assertTrue(g.licenseUrl?.startsWith("https://") == true, g.key)
            } else {
                assertTrue(file in LicenseTextFiles, "${g.key}: $file")
                assertTrue(file.startsWith("files/licenses/"), file)
            }
        }
        // 전문 파일은 모두 어느 묶음이 쓴다
        assertEquals(LicenseTextFiles.toSet(), OpenSourceNotices.mapNotNull { it.textFile }.toSet())
    }

    @Test
    fun `주소는 모두 https`() {
        OpenSourceNotices.flatMap { it.entries }.forEach {
            assertTrue(it.url.startsWith("https://"), "${it.name}: ${it.url}")
        }
    }

    @Test
    fun `이름은 겹치지 않는다`() {
        val names = OpenSourceNotices.flatMap { g -> g.entries.map { it.name } }
        assertEquals(names.size, names.toSet().size, "$names")
    }

    @Test
    fun `Pretendard는 OFL 묶음, Material Symbols는 Apache 묶음`() {
        assertEquals(UiText.Raw("SIL Open Font License 1.1"), group("ofl").title)
        assertTrue(group("ofl").entries.any { it.name == "Pretendard" })
        assertEquals(UiText.Raw("Apache License 2.0"), group("apache").title)
        assertTrue(group("apache").entries.any { it.name == "Material Symbols" })
    }

    @Test
    fun `웹 구성 요소 묶음은 줄마다 라이선스 이름과 웹 고지, 그 밖의 묶음은 묶음 제목이 라이선스`() {
        assertTrue(group("web").entries.all { it.license != null })
        assertEquals(WEB_NOTICES_TEXT, group("web").textFile)
        OpenSourceNotices.filter { it.key != "web" }.forEach { g ->
            assertTrue(g.title is UiText.Raw, g.key)
            assertTrue(g.entries.all { it.license == null }, g.key)
        }
    }

    @Test
    fun `전문은 60줄 안팎의 빈 줄에서 끊고 글은 잃지 않는다`() {
        val paragraphs = (1..5).map { p -> (1..30).joinToString("\n") { "p$p-$it" } }
        val chunks = chunkLines(paragraphs.joinToString("\n\n"))
        assertEquals(3, chunks.size, "$chunks")
        assertEquals(
            paragraphs.flatMap { it.lines() },
            chunks.flatMap { it.lines() }.filter { it.isNotBlank() }
        )
        assertEquals(listOf("한 줄"), chunkLines("한 줄\n"))
    }
}
