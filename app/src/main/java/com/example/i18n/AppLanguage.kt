package com.example.i18n

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val shortCode: String
) {
    UZ(code = "uz", nativeName = "O'zbekcha", shortCode = "O'Z"),
    RU(code = "ru", nativeName = "Русский", shortCode = "RU"),
    EN(code = "en", nativeName = "English", shortCode = "EN");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: UZ
    }
}
