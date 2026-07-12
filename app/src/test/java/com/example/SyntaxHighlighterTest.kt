package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.components.SyntaxHighlighter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SyntaxHighlighterTest {

    @Test
    fun test_highlight_kotlin_file() {
        val code = """
            package com.example
            @Volatile
            class MyClass {
                // This is a comment
                val number = 42
                fun test() {
                    val str = "Hello"
                }
            }
        """.trimIndent()

        val annotated = SyntaxHighlighter.highlight(code, "test.kt")
        assertNotNull(annotated)
        val text = annotated.text
        assertEquals(code, text)

        // Find comment index
        val commentIdx = text.indexOf("// This is a comment")
        assertTrue(commentIdx != -1)
        val commentSpan = annotated.spanStyles.find { it.start == commentIdx }
        assertNotNull(commentSpan)
        assertEquals(Color(0xFF6A9955), commentSpan!!.item.color)
        assertEquals(FontStyle.Italic, commentSpan.item.fontStyle)

        // Find keyword "class"
        val classIdx = text.indexOf("class")
        assertTrue(classIdx != -1)
        val classSpan = annotated.spanStyles.find { it.start == classIdx }
        assertNotNull(classSpan)
        assertEquals(Color(0xFF569CD6), classSpan!!.item.color)
        assertEquals(FontWeight.Bold, classSpan.item.fontWeight)

        // Find annotation "@Volatile"
        val annotationIdx = text.indexOf("@Volatile")
        assertTrue(annotationIdx != -1)
        val annotationSpan = annotated.spanStyles.find { it.start == annotationIdx }
        assertNotNull(annotationSpan)
        assertEquals(Color(0xFFDCDCAA), annotationSpan!!.item.color)
    }

    @Test
    fun test_highlight_json_file() {
        val json = """
            {
                "id": 123,
                "name": "Gist",
                "is_public": true
            }
        """.trimIndent()

        val annotated = SyntaxHighlighter.highlight(json, "test.json")
        val text = annotated.text
        assertEquals(json, text)

        // Find key "id"
        val keyIdx = text.indexOf("\"id\"")
        assertTrue(keyIdx != -1)
        val keySpan = annotated.spanStyles.find { it.start == keyIdx }
        assertNotNull(keySpan)
        assertEquals(Color(0xFF9CDCFE), keySpan!!.item.color)

        // Find value 123
        val numIdx = text.indexOf("123")
        assertTrue(numIdx != -1)
        val numSpan = annotated.spanStyles.find { it.start == numIdx }
        assertNotNull(numSpan)
        assertEquals(Color(0xFFB5CEA8), numSpan!!.item.color)
    }

    @Test
    fun test_highlight_xml_file() {
        val xml = """
            <gist id="123">
                <!-- This is XML comment -->
                <file name="test.kt" />
            </gist>
        """.trimIndent()

        val annotated = SyntaxHighlighter.highlight(xml, "test.xml")
        val text = annotated.text
        assertEquals(xml, text)

        // Find comment <!-- This is XML comment -->
        val commentIdx = text.indexOf("<!-- This is XML comment -->")
        assertTrue(commentIdx != -1)
        val commentSpan = annotated.spanStyles.find { it.start == commentIdx }
        assertNotNull(commentSpan)
        assertEquals(Color(0xFF6A9955), commentSpan!!.item.color)

        // Find XML Tag <gist
        val tagIdx = text.indexOf("<gist")
        assertTrue(tagIdx != -1)
        val tagSpan = annotated.spanStyles.find { it.start == tagIdx }
        assertNotNull(tagSpan)
        assertEquals(Color(0xFF569CD6), tagSpan!!.item.color)
    }

    @Test
    fun test_highlight_fallback() {
        val raw = "some random file content with a // comment and 42"
        val annotated = SyntaxHighlighter.highlight(raw, "test.txt")
        val text = annotated.text
        assertEquals(raw, text)

        // Check that generic fallback still parsed comment
        val commentIdx = text.indexOf("// comment and 42")
        assertTrue(commentIdx != -1)
        val commentSpan = annotated.spanStyles.find { it.start == commentIdx }
        assertNotNull(commentSpan)
        assertEquals(Color(0xFF6A9955), commentSpan!!.item.color)
    }
}
