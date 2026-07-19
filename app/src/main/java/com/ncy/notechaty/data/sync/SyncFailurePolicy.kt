package com.ncy.notechaty.data.sync

/** Distinguishes permanent trust failures from ordinary offline/network errors. */
object SyncFailurePolicy {
    const val REPAIR_MESSAGE =
        "Pairing credential was revoked or changed. Remove this device and pair again."
    const val REJECTED_MESSAGE =
        "Pairing was rejected on the other device. Remove this device before trying again."

    fun isIdentityFailure(reason: String): Boolean {
        val lower = reason.lowercase()
        return lower.contains("identity_collision") || lower.contains("identity_mismatch")
    }

    fun isRejectedCredential(reason: String): Boolean {
        val lower = reason.lowercase()
        return lower.contains("pairing_revoked") ||
            lower.contains("bad_auth") ||
            lower.contains("auth_failed") ||
            lower.contains("hmac mismatch") ||
            lower.contains("invalid pairing secret") ||
            lower.contains("already-claimed pairing code")
    }

    fun stopsAutomaticRetry(reason: String): Boolean =
        isIdentityFailure(reason) || isRejectedCredential(reason) || isHostRejection(reason)

    fun userFacingReason(reason: String): String =
        when {
            isHostRejection(reason) -> REJECTED_MESSAGE
            isRejectedCredential(reason) -> REPAIR_MESSAGE
            else -> reason
        }

    private fun isHostRejection(reason: String): Boolean =
        reason.lowercase().contains("pairing_rejected")
}
