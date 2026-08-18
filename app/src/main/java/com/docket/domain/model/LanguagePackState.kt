package com.docket.domain.model

enum class LanguagePackStatus { NOT_INSTALLED, DOWNLOADING, INSTALLED, FAILED }

data class LanguagePackState(
    val language: OcrLanguage,
    val status: LanguagePackStatus
)
