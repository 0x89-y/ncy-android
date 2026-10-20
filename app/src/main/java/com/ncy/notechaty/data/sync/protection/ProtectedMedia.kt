package com.ncy.notechaty.data.sync.protection

import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.sync.event.SealedBlobRef
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.media.MediaRefs
import java.io.File
import java.util.UUID

/**
 * A protected chat's attachments, sealed with the chat key (protocol §11.8).
 *
 * Sealing happens before hashing, so a sealed attachment is just another
 * content-addressed file named `<hash of sealed bytes>.<ext>`: transfer and
 * verification are unchanged, and a device without the key stores and relays it.
 * References in the message are rewritten to the sealed file, and the envelope
 * lists them in the clear ([SealedBlobRef]) so locked devices keep them too.
 *
 * Avatars and backgrounds stay plain: like the chat name, they belong to the
 * always-readable chat metadata the chat list shows while locked.
 */
object ProtectedMedia {

    /**
     * Rewrite every attachment of [message] under [newKey]: plain ones are sealed;
     * ones sealed under [oldKey] (a PIN change) are re-sealed; with [newKey] null
     * (protection removed) sealed ones are opened back to plain. Media not on this
     * device is left as it is. An original no other message uses ([stillUsed]) is
     * deleted, so no plain copy outlives protection.
     */
    fun rewrite(
        chatDir: File,
        message: Message,
        newKey: ByteArray?,
        oldKey: ByteArray? = null,
        stillUsed: (hash: String) -> Boolean = { false },
        /** Reference → rewritten reference, shared across one batch so every edit of a message lands on the same file. */
        memo: MutableMap<String, String>? = null,
    ): Message {
        fun ref(reference: String?): String? = reference?.let { original ->
            memo?.get(original) ?: rewriteReference(chatDir, original, newKey, oldKey, stillUsed)
                .also { memo?.put(original, it) }
        }
        return message.copy(
            voiceFile = ref(message.voiceFile),
            imageFiles = message.imageFiles.map { ref(it) ?: it },
            fileAttachments = message.fileAttachments.map { it.copy(file = ref(it.file) ?: it.file) },
            content = message.content.map { block ->
                block.copy(spans = block.spans.map { span -> span.copy(inlineImage = ref(span.inlineImage)) })
            },
        )
    }

    /** What a sealed envelope lists for [message]: the sealed size, where known. */
    fun blobRefs(chatDir: File, message: Message): List<SealedBlobRef> = MediaRefs.of(message).map { ref ->
        val onDisk = fileFor(chatDir, ref.hash, ref.mediaType)
        SealedBlobRef(ref.hash, ref.mediaType, onDisk?.takeIf { it.isFile }?.length() ?: ref.sizeBytes)
    }

    private fun fileFor(chatDir: File, hash: String, mediaType: String): File? = when (mediaType) {
        "voice" -> File(chatDir, "voice/$hash.m4a")
        "image" -> File(chatDir, "images/$hash.jpg")
        "file" -> File(chatDir, "files/$hash.bin")
        else -> null
    }

    private fun rewriteReference(
        chatDir: File,
        reference: String,
        newKey: ByteArray?,
        oldKey: ByteArray?,
        stillUsed: (String) -> Boolean,
    ): String {
        val hash = MediaHash.extractHash(reference) ?: return reference
        val original = File(chatDir, reference)
        if (!original.isFile) return reference
        val dir = original.parentFile ?: return reference
        val ext = original.extension
        val sealed = SealedBlob.isSealed(original)
        // Nothing to do: already where it should be.
        if (newKey != null && sealed && oldKey == null) return reference
        if (newKey == null && !sealed) return reference

        val plain: File
        val opened: File?
        if (sealed) {
            val key = oldKey ?: return reference
            opened = File(dir, "opening_${UUID.randomUUID()}.part")
            if (runCatching { SealedBlob.openFile(key, original, opened) }.isFailure) {
                // Not under the old key, so presumably already under the new one.
                return reference
            }
            plain = opened
        } else {
            plain = original
            opened = null
        }
        val result = File(dir, "sealing_${UUID.randomUUID()}.part")
        try {
            if (newKey != null) SealedBlob.sealFile(newKey, plain, result) else plain.copyTo(result, overwrite = true)
            val newHash = MediaHash.ofFile(result)
            val target = File(dir, "$newHash.$ext")
            if (target.exists()) result.delete() else if (!result.renameTo(target)) {
                result.copyTo(target, overwrite = false)
                result.delete()
            }
            if (newHash != hash && !stillUsed(hash)) original.delete()
            return reference.replace(hash, newHash)
        } finally {
            result.delete()
            opened?.delete()
        }
    }
}
