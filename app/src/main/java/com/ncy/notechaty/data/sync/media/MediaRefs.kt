package com.ncy.notechaty.data.sync.media

import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp

/**
 * Which content-addressed blobs an event refers to.
 *
 * Shared rather than per-caller: the sync layer uses it to decide what to fetch, and
 * storage cleanup uses it to decide what is still wanted. Those two answers must come
 * from the same rule — a reference the fetcher honours but the sweeper misses is a
 * blob deleted out from under a live message.
 *
 * Refs that don't match the content-addressed naming pattern are skipped; legacy
 * random-named media was never transferable and is not ours to reason about.
 */
object MediaRefs {

    data class Ref(
        val hash: String,
        val mediaType: String,
        val sizeBytes: Long? = null,
    )

    fun extract(event: ChatEvent): List<Ref> {
        val refs = mutableListOf<Ref>()
        when (val op = event.op) {
            is ChatEventOp.MessageCreated -> collectFromMessage(op.message, refs)
            is ChatEventOp.MessageEdited -> collectFromMessage(op.message, refs)
            is ChatEventOp.ChatMetadataChanged -> {
                // Avatars and backgrounds carry hashed refs the receiver fetches, and
                // are just as easy to delete by accident as message attachments.
                MediaHash.extractHash(op.metadata.avatarFile)?.let { refs += Ref(it, "image") }
                MediaHash.extractHash(op.metadata.backgroundFile)?.let { refs += Ref(it, "image") }
            }
            // A sealed body lists its sealed attachments in the clear (§11.8); one
            // written before that list existed says nothing without the key.
            is ChatEventOp.EncryptedMessage -> op.blobs?.forEach { blob ->
                if (MediaHash.isValidHash(blob.hash) && blob.mediaType in MEDIA_TYPES) {
                    refs += Ref(blob.hash, blob.mediaType, blob.sizeBytes)
                }
            }
            else -> { /* tombstones carry no refs */ }
        }
        return refs.distinctBy { it.hash to it.mediaType }
    }

    private val MEDIA_TYPES = setOf("voice", "image", "file")

    /** The refs of a plain message — what a sealed envelope lists for it. */
    fun of(message: Message): List<Ref> =
        mutableListOf<Ref>().also { collectFromMessage(message, it) }.distinctBy { it.hash to it.mediaType }

    private fun collectFromMessage(message: Message, refs: MutableList<Ref>) {
        MediaHash.extractHash(message.voiceFile)?.let { refs += Ref(it, "voice") }
        for (img in message.imageFiles) {
            MediaHash.extractHash(img)?.let { refs += Ref(it, "image") }
        }
        for (attachment in message.fileAttachments) {
            MediaHash.extractHash(attachment.file)?.let {
                refs += Ref(it, "file", attachment.sizeBytes)
            }
        }
        for (block in message.content) {
            for (span in block.spans) {
                MediaHash.extractHash(span.inlineImage)?.let { refs += Ref(it, "image") }
            }
        }
    }
}
