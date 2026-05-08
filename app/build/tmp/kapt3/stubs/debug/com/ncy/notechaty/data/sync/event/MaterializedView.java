package com.ncy.notechaty.data.sync.event;

import com.ncy.notechaty.data.model.ChatData;
import com.ncy.notechaty.data.model.ChatMetadata;
import com.ncy.notechaty.data.model.Message;

/**
 * Pure fold from an event log to a [ChatData]. Processes events in HLC order so the result
 * is deterministic regardless of the order events were appended.
 *
 * Rules:
 * - ChatMetadataChanged: whole-record LWW (later HLC wins; achieved naturally by ordering).
 * - MessageCreated / MessageEdited: applied unless the message id is tombstoned.
 * - MessageTombstoned: removes the message and ignores all later ops on that id.
 * - ChatTombstoned: yields `null` (chat should not exist on disk).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0003\u001a\u0004\u0018\u00010\u00042\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a8\u0006\b"}, d2 = {"Lcom/ncy/notechaty/data/sync/event/MaterializedView;", "", "()V", "fold", "Lcom/ncy/notechaty/data/model/ChatData;", "events", "", "Lcom/ncy/notechaty/data/sync/event/ChatEvent;", "app_debug"})
public final class MaterializedView {
    @org.jetbrains.annotations.NotNull()
    public static final com.ncy.notechaty.data.sync.event.MaterializedView INSTANCE = null;
    
    private MaterializedView() {
        super();
    }
    
    /**
     * Fold the events into a ChatData, or null if the chat is tombstoned / has no metadata.
     */
    @org.jetbrains.annotations.Nullable()
    public final com.ncy.notechaty.data.model.ChatData fold(@org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.sync.event.ChatEvent> events) {
        return null;
    }
}