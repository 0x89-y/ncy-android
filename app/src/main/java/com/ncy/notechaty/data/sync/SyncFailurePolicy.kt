package com.ncy.notechaty.data.sync

/** Distinguishes permanent trust failures from ordinary offline/network errors. */
object SyncFailurePolicy {
    const val REPAIR_MESSAGE =
        "Pairing credential was revoked or changed. Remove this device and pair again."
    const val REJECTED_MESSAGE =
        "Pairing was rejected on the other device. Remove this device before trying again."
    const val UNVERIFIED_REJECTION_MESSAGE =
        "The other device refused this pairing. Still retrying; if this keeps happening, " +
            "remove this device and pair again."

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

    private fun isTrustFailure(reason: String): Boolean =
        isIdentityFailure(reason) || isRejectedCredential(reason) || isHostRejection(reason)

    /**
     * @param authenticated false when the failure arrived as a plaintext frame
     *   before the encrypted session existed. Anyone on the network path can forge
     *   one of those, so it never stops retries by itself: a single injected
     *   `auth_failed` used to halt sync for good and tell the user to re-pair.
     */
    fun stopsAutomaticRetry(reason: String, authenticated: Boolean = true): Boolean =
        authenticated && isTrustFailure(reason)

    fun userFacingReason(reason: String, authenticated: Boolean = true): String =
        when {
            !authenticated && isTrustFailure(reason) -> UNVERIFIED_REJECTION_MESSAGE
            isHostRejection(reason) -> REJECTED_MESSAGE
            isRejectedCredential(reason) -> REPAIR_MESSAGE
            else -> reason
        }

    private fun isHostRejection(reason: String): Boolean =
        reason.lowercase().contains("pairing_rejected")
}
