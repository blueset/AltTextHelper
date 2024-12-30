package studio1a23.altTextAi.ui.components

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.IdRes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import ca.blarg.prism4j.languages.Prism4jGrammarLocator
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j

@Composable
@NonRestartableComposable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    @FontRes fontResource: Int? = null,
    style: TextStyle = LocalTextStyle.current,
    @IdRes viewId: Int? = null,
    textTruncated: (Boolean) -> Unit = {}
) {
    val defaultColor: Color = LocalContentColor.current
    val context: Context = LocalContext.current
    val markdownRender: Markwon = remember { createMarkdownRender(context) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createTextView(
                context = ctx,
                color = color,
                defaultColor = defaultColor,
                fontSize = fontSize,
                textAlign = textAlign,
                maxLines = maxLines,
                fontResource = fontResource,
                style = style,
                viewId = viewId,
                textTruncated = textTruncated
            )
        },
        update = { textView ->
            markdownRender.setMarkdown(textView, markdown)
        }
    )
}

private fun createTextView(
    context: Context,
    color: Color = Color.Unspecified,
    defaultColor: Color,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    @FontRes fontResource: Int? = null,
    style: TextStyle,
    @IdRes viewId: Int? = null,
    textTruncated: (Boolean) -> Unit
): TextView {

    val textColor = color.takeOrElse { style.color.takeOrElse { defaultColor } }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            textAlign = textAlign,
        )
    )
    return TextView(context).apply {

        setTextColor(textColor.toArgb())
        setMaxLines(maxLines)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, mergedStyle.fontSize.value)

        viewId?.let { id = viewId }
        textAlignment = when (textAlign) {
            TextAlign.Left, TextAlign.Start -> View.TEXT_ALIGNMENT_TEXT_START
            TextAlign.Right, TextAlign.End -> View.TEXT_ALIGNMENT_TEXT_END
            TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
            else -> View.TEXT_ALIGNMENT_TEXT_START
        }

        fontResource?.let { font ->
            typeface = ResourcesCompat.getFont(context, font)
        }

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val isTruncated = (layout?.lineCount ?: 0) > maxLines
            textTruncated(isTruncated)
            if (isTruncated && text.substring(text.lastIndex) != "\u2026") {
                // Workaround known issue with spannables and ellipsize https://github.com/noties/Markwon/issues/180
                val end = layout.getLineEnd(maxLines - 1)
                val newVal = text.subSequence(0, end - 1)
                setText(newVal, TextView.BufferType.SPANNABLE)
                append("\u2026")
            }
        }

        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                val isTruncated = (layout?.lineCount ?: 0) > maxLines
                textTruncated(isTruncated)
            }
        })
    }
}

internal fun createMarkdownRender(context: Context): Markwon {
    val prism4j = Prism4j(Prism4jGrammarLocator())
    return Markwon.builder(context)
        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(HtmlPlugin.create())
        .usePlugin(SyntaxHighlightPlugin.create(prism4j, Prism4jThemeDefault.create()))
        .build()
}
