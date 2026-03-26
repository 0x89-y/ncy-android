package com.ncy.notechaty.data.model;

/**
 * Root object for chat.json serialization.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\u000f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0003J-\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u001a"}, d2 = {"Lcom/ncy/notechaty/data/model/ChatData;", "", "version", "", "chat", "Lcom/ncy/notechaty/data/model/ChatMetadata;", "messages", "", "Lcom/ncy/notechaty/data/model/Message;", "(ILcom/ncy/notechaty/data/model/ChatMetadata;Ljava/util/List;)V", "getChat", "()Lcom/ncy/notechaty/data/model/ChatMetadata;", "getMessages", "()Ljava/util/List;", "getVersion", "()I", "component1", "component2", "component3", "copy", "equals", "", "other", "hashCode", "toString", "", "app_debug"})
public final class ChatData {
    private final int version = 0;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.model.ChatMetadata chat = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.Message> messages = null;
    
    public ChatData(int version, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ChatMetadata chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages) {
        super();
    }
    
    public final int getVersion() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ChatMetadata getChat() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Message> getMessages() {
        return null;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ChatMetadata component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.Message> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.ChatData copy(int version, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.ChatMetadata chat, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.Message> messages) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}