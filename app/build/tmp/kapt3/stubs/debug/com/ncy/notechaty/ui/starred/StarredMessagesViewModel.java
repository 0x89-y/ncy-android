package com.ncy.notechaty.ui.starred;

import androidx.lifecycle.ViewModel;
import com.ncy.notechaty.data.model.Chat;
import com.ncy.notechaty.data.model.Message;
import com.ncy.notechaty.data.repository.ChatRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\r\u001a\u00020\u000eH\u0002J\u000e\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0010\u001a\u00020\bR\u001a\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0011"}, d2 = {"Lcom/ncy/notechaty/ui/starred/StarredMessagesViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/ncy/notechaty/data/repository/ChatRepository;", "(Lcom/ncy/notechaty/data/repository/ChatRepository;)V", "_starredMessages", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/ncy/notechaty/ui/starred/StarredMessage;", "starredMessages", "Lkotlinx/coroutines/flow/StateFlow;", "getStarredMessages", "()Lkotlinx/coroutines/flow/StateFlow;", "loadStarredMessages", "", "toggleStar", "starredMessage", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class StarredMessagesViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ncy.notechaty.data.repository.ChatRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ncy.notechaty.ui.starred.StarredMessage>> _starredMessages = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.ui.starred.StarredMessage>> starredMessages = null;
    
    @javax.inject.Inject()
    public StarredMessagesViewModel(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.data.repository.ChatRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ncy.notechaty.ui.starred.StarredMessage>> getStarredMessages() {
        return null;
    }
    
    private final void loadStarredMessages() {
    }
    
    public final void toggleStar(@org.jetbrains.annotations.NotNull()
    com.ncy.notechaty.ui.starred.StarredMessage starredMessage) {
    }
}