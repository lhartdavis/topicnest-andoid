package com.techtower.meetingtranscriber.settings

enum class ApiKeyValidationStatus {
    UNKNOWN,
    CHECKING,
    VALID,
    INVALID,
}

data class ApiKeyValidationSnapshot(
    val status: ApiKeyValidationStatus = ApiKeyValidationStatus.UNKNOWN,
    val message: String? = null,
    val label: String? = null,
    val checkedAtMillis: Long? = null,
)

data class ApiKeyValidationResult(
    val status: ApiKeyValidationStatus,
    val message: String,
    val label: String? = null,
)
