package com.ncy.notechaty.data.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b&\n\u0002\u0010\b\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\u00a3\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u0012\b\b\u0002\u0010\u000b\u001a\u00020\n\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\n\u0012\b\b\u0002\u0010\r\u001a\u00020\u000e\u0012\b\b\u0002\u0010\u000f\u001a\u00020\u0010\u0012\u000e\b\u0002\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u0012\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u0012\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\n\u0012\u000e\b\u0002\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u0006\u00a2\u0006\u0002\u0010\u0016J\t\u0010*\u001a\u00020\u0003H\u00c6\u0003J\u000f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006H\u00c6\u0003J\u000f\u0010,\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006H\u00c6\u0003J\u0010\u0010-\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001fJ\u000f\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00150\u0006H\u00c6\u0003J\t\u0010/\u001a\u00020\u0003H\u00c6\u0003J\u000f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u00c6\u0003J\u000b\u00101\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u00102\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001fJ\t\u00103\u001a\u00020\nH\u00c6\u0003J\u0010\u00104\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u001fJ\t\u00105\u001a\u00020\u000eH\u00c6\u0003J\t\u00106\u001a\u00020\u0010H\u00c6\u0003J\u00b0\u0001\u00107\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\u000b\u001a\u00020\n2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\n2\b\b\u0002\u0010\r\u001a\u00020\u000e2\b\b\u0002\u0010\u000f\u001a\u00020\u00102\u000e\b\u0002\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u00062\u000e\b\u0002\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u00062\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\n2\u000e\b\u0002\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u0006H\u00c6\u0001\u00a2\u0006\u0002\u00108J\u0013\u00109\u001a\u00020\u00102\b\u0010:\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010;\u001a\u00020<H\u00d6\u0001J\u0006\u0010=\u001a\u00020\u0003J\t\u0010>\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001aR\u0011\u0010\u000b\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u0015\u0010\f\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010 \u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u0018R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001aR\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010#R\u0015\u0010\u0013\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010 \u001a\u0004\b$\u0010\u001fR\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001aR\u0011\u0010\r\u001a\u00020\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0015\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010 \u001a\u0004\b(\u0010\u001fR\u0013\u0010\b\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010\u0018\u00a8\u0006?"}, d2 = {"Lcom/ncy/notechaty/data/model/Message;", "", "id", "", "chatId", "content", "", "Lcom/ncy/notechaty/data/model/RichTextBlock;", "voiceFile", "voiceDurationMs", "", "createdAt", "editedAt", "type", "Lcom/ncy/notechaty/data/model/MessageType;", "isStarred", "", "imageFiles", "tags", "reminderAt", "checklistItems", "Lcom/ncy/notechaty/data/model/ChecklistItem;", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/Long;JLjava/lang/Long;Lcom/ncy/notechaty/data/model/MessageType;ZLjava/util/List;Ljava/util/List;Ljava/lang/Long;Ljava/util/List;)V", "getChatId", "()Ljava/lang/String;", "getChecklistItems", "()Ljava/util/List;", "getContent", "getCreatedAt", "()J", "getEditedAt", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getId", "getImageFiles", "()Z", "getReminderAt", "getTags", "getType", "()Lcom/ncy/notechaty/data/model/MessageType;", "getVoiceDurationMs", "getVoiceFile", "component1", "component10", "component11", "component12", "component13", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/String;Ljava/lang/Long;JLjava/lang/Long;Lcom/ncy/notechaty/data/model/MessageType;ZLjava/util/List;Ljava/util/List;Ljava/lang/Long;Ljava/util/List;)Lcom/ncy/notechaty/data/model/Message;", "equals", "other", "hashCode", "", "plainTextPreview", "toString", "app_debug"})
public final class Message {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String id = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String chatId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.RichTextBlock> content = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String voiceFile = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long voiceDurationMs = null;
    private final long createdAt = 0L;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long editedAt = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.model.MessageType type = null;
    private final boolean isStarred = false;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> imageFiles = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> tags = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long reminderAt = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.ncy.notechaty.data.model.ChecklistItem> checklistItems = null;
    
    public Message(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.RichTextBlock> content, @org.jetbrains.annotations.Nullable()
    java.lang.String voiceFile, @org.jetbrains.annotations.Nullable()
    java.lang.Long voiceDurationMs, long createdAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long editedAt, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.MessageType type, boolean isStarred, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> imageFiles, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> tags, @org.jetbrains.annotations.Nullable()
    java.lang.Long reminderAt, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.ChecklistItem> checklistItems) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getChatId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.RichTextBlock> getContent() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getVoiceFile() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getVoiceDurationMs() {
        return null;
    }
    
    public final long getCreatedAt() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getEditedAt() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.MessageType getType() {
        return null;
    }
    
    public final boolean isStarred() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getImageFiles() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getTags() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getReminderAt() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.ChecklistItem> getChecklistItems() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String plainTextPreview() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component11() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component12() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.ChecklistItem> component13() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ncy.notechaty.data.model.RichTextBlock> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component5() {
        return null;
    }
    
    public final long component6() {
        return 0L;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.MessageType component8() {
        return null;
    }
    
    public final boolean component9() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.ncy.notechaty.data.model.Message copy(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    java.lang.String chatId, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.RichTextBlock> content, @org.jetbrains.annotations.Nullable()
    java.lang.String voiceFile, @org.jetbrains.annotations.Nullable()
    java.lang.Long voiceDurationMs, long createdAt, @org.jetbrains.annotations.Nullable()
    java.lang.Long editedAt, @org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.model.MessageType type, boolean isStarred, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> imageFiles, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> tags, @org.jetbrains.annotations.Nullable()
    java.lang.Long reminderAt, @org.jetbrains.annotations.NotNull()
    java.util.List<com.ncy.notechaty.data.model.ChecklistItem> checklistItems) {
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