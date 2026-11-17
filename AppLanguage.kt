package com.example.ui.i18n

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HighwayAmberBright
import com.example.ui.theme.KrishnaCrimson
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val shortLabel: String
) {
    ENGLISH("en", "English", "EN"),
    HINDI("hi", "हिन्दी", "हिं"),
    GUJARATI("gu", "ગુજરાતી", "ગુ");

    companion object {
        fun fromCode(code: String?): AppLanguage = when (code?.lowercase(Locale.ROOT)) {
            "hi" -> HINDI
            "gu" -> GUJARATI
            else -> ENGLISH
        }
    }
}

object LanguagePrefs {
    private const val PREFS_NAME = "krishna_travels_prefs"
    private const val KEY_LANGUAGE = "app_language"

    fun load(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (!saved.isNullOrBlank()) {
            return AppLanguage.fromCode(saved)
        }
        return when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "hi" -> AppLanguage.HINDI
            "gu" -> AppLanguage.GUJARATI
            else -> AppLanguage.ENGLISH
        }
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalAppStrings = compositionLocalOf<AppStrings> { englishStrings() }
val LocalOnLanguageSelected = compositionLocalOf<(AppLanguage) -> Unit> { {} }

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.ENGLISH -> englishStrings()
    AppLanguage.HINDI -> hindiStrings()
    AppLanguage.GUJARATI -> gujaratiStrings()
}

@Composable
fun LanguageSelectorPillRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    darkSurface: Boolean = false,
    modifier: Modifier = Modifier
) {
    val containerColor = if (darkSurface) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == selected
                val bg = when {
                    isSelected -> KrishnaCrimson
                    else -> Color.Transparent
                }
                val fg = when {
                    isSelected -> Color.White
                    darkSurface -> Color.White.copy(alpha = 0.85f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg)
                        .clickable { onSelect(lang) }
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .testTag("lang_pill_${lang.code}")
                ) {
                    Text(
                        text = lang.nativeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = fg,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TopBarLanguageButton(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    tint: Color = HighwayAmberBright
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .clickable { expanded = true }
                .testTag("topbar_language_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Translate,
                    contentDescription = "Language",
                    tint = tint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selected.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${lang.nativeName} (${lang.shortLabel})",
                            fontWeight = if (lang == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (lang == selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = KrishnaCrimson
                            )
                        }
                    },
                    onClick = {
                        onSelect(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
