package studio1a23.altTextAi.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteTextField(
    candidates: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    content: @Composable (
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val filteredOptions =
        candidates
            .map { Pair(it, it.indexOf(value, ignoreCase = true)) }
            .filter { it.second != -1 }
            .sortedBy { it.second }

    ExposedDropdownMenuBox(
        expanded = menuExpanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { menuExpanded = it },
        modifier = modifier
    ) {
        content(
            value,
            onValueChange,
            Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .onFocusChanged {
                    if (it.isFocused && !menuExpanded) {
                        menuExpanded = true
                    } else if (!it.isFocused && menuExpanded) {
                        menuExpanded = false
                    }
                }
        )

        if (filteredOptions.isNotEmpty()) {
            DropdownMenu(
                modifier = Modifier.exposedDropdownSize(true),
                properties = PopupProperties(focusable = false),
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                filteredOptions.forEach { option ->
                    val beforeMatch = option.first.substring(0, option.second)
                    val match = option.first.substring(option.second, option.second + value.length)
                    val afterMatch = option.first.substring(option.second + value.length)
                    DropdownMenuItem(
                        text = {
                            Text(buildAnnotatedString {
                                append(beforeMatch)
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(match)
                                }
                                append(afterMatch)
                            })
                        },
                        onClick = {
                            onValueChange(option.first)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
