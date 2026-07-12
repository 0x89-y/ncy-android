package com.ncy.notechaty.data.sync.transport

/** Loop prevention for incremental sync, with reinstall/restore recovery semantics. */
object SyncCatchupPolicy {
    fun shouldSend(eventOriginDevice: String, remoteDeviceId: String, fullReplay: Boolean): Boolean =
        fullReplay || eventOriginDevice != remoteDeviceId

    fun markForSend(
        eventId: String,
        eventOriginDevice: String,
        remoteDeviceId: String,
        fullReplay: Boolean,
        sentEventIds: MutableSet<String>,
    ): Boolean = shouldSend(eventOriginDevice, remoteDeviceId, fullReplay) &&
        sentEventIds.add(eventId)

}
