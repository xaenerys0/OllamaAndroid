package com.ollamaandroid.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ollamaandroid.app.data.db.MessageEntity
import com.ollamaandroid.app.data.network.ReasoningLevel
import com.ollamaandroid.app.ui.components.MarkdownText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Conversations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text("New chat") },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        selected = false,
                        onClick = {
                            viewModel.newConversation()
                            scope.launch { drawerState.close() }
                        },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(conversations, key = { it.id }) { conversation ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = conversation.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                                badge = {
                                    IconButton(onClick = { viewModel.deleteConversation(conversation.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete conversation",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                                selected = conversation.id == uiState.currentConversationId,
                                onClick = {
                                    viewModel.selectConversation(conversation.id)
                                    scope.launch { drawerState.close() }
                                },
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onOpenSettings()
                        },
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        ModelSelector(
                            selectedModel = settings.selectedModel,
                            models = uiState.availableModels,
                            loading = uiState.modelsLoading,
                            onExpand = { viewModel.refreshModels() },
                            onSelect = { viewModel.selectModel(it) },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open conversations")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                if (!settings.isConfigured) {
                    OnboardingCard(onOpenSettings = onOpenSettings)
                } else {
                    MessageList(
                        messages = messages,
                        streamingReply = uiState.streaming,
                        modifier = Modifier.weight(1f),
                    )
                    ChatInputBar(
                        isStreaming = uiState.streaming != null,
                        supportsThinking = uiState.supportsThinking,
                        reasoning = settings.reasoning,
                        onSelectReasoning = { viewModel.selectReasoning(it) },
                        onSend = { viewModel.sendMessage(it) },
                        onStop = { viewModel.stopStreaming() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSelector(
    selectedModel: String,
    models: List<String>,
    loading: Boolean,
    onExpand: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    expanded = true
                    if (models.isEmpty()) onExpand()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = selectedModel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 220.dp),
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose model")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (loading) {
                DropdownMenuItem(
                    text = { Text("Loading models…") },
                    onClick = {},
                    enabled = false,
                )
            } else if (models.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No models found — tap to retry") },
                    onClick = { onExpand() },
                )
            }
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onSelect(model)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<MessageEntity>,
    streamingReply: StreamingReply?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Follow the bottom while messages arrive or stream in.
    LaunchedEffect(messages.size, streamingReply?.content?.length, streamingReply?.thinking?.length) {
        val itemCount = messages.size + (if (streamingReply != null) 1 else 0)
        if (itemCount > 0) {
            listState.scrollToItem(itemCount - 1)
        }
    }

    if (messages.isEmpty() && streamingReply == null) {
        EmptyChatPlaceholder(modifier = modifier)
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            MessageBubble(
                isUser = message.role == "user",
                content = message.content,
                thinking = message.thinking,
                interrupted = message.interrupted,
            )
        }
        if (streamingReply != null) {
            item(key = "streaming") {
                MessageBubble(
                    isUser = false,
                    content = streamingReply.content,
                    thinking = streamingReply.thinking.ifBlank { null },
                    interrupted = false,
                    streaming = true,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    isUser: Boolean,
    content: String,
    thinking: String? = null,
    interrupted: Boolean,
    streaming: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!thinking.isNullOrBlank()) {
                    // Auto-expand while the model is still reasoning (no answer yet).
                    ThinkingSection(
                        thinking = thinking,
                        initiallyExpanded = streaming && content.isEmpty(),
                    )
                }
                if (content.isEmpty() && streaming) {
                    if (thinking.isNullOrBlank()) {
                        ThinkingIndicator()
                    }
                } else {
                    MarkdownText(content = content)
                }
                if (interrupted) {
                    Text(
                        text = "Response interrupted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingSection(
    thinking: String,
    initiallyExpanded: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
        ) {
            Icon(
                Icons.Outlined.Psychology,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = if (expanded) "Hide thinking" else "Show thinking",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Text(
                text = thinking,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 20.dp),
            )
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Thinking…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyChatPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ask anything",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingCard(onOpenSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.Key,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Connect to Ollama",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add your Ollama API key to start chatting with cloud models.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onOpenSettings) {
                    Text("Open settings")
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    isStreaming: Boolean,
    supportsThinking: Boolean,
    reasoning: ReasoningLevel,
    onSelectReasoning: (ReasoningLevel) -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (supportsThinking) {
                ReasoningPicker(
                    reasoning = reasoning,
                    onSelect = onSelectReasoning,
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 6,
            )
            Spacer(modifier = Modifier.size(8.dp))
            FilledIconButton(
                onClick = {
                    if (isStreaming) {
                        onStop()
                    } else if (input.isNotBlank()) {
                        onSend(input)
                        input = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                enabled = isStreaming || input.isNotBlank(),
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isStreaming) "Stop" else "Send",
                )
            }
        }
    }
}

/**
 * Reasoning selector for thinking-capable models. Tinted when a non-default
 * level is active.
 */
@Composable
private fun ReasoningPicker(
    reasoning: ReasoningLevel,
    onSelect: (ReasoningLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = reasoning != ReasoningLevel.DEFAULT

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.Psychology,
                contentDescription = "Reasoning: ${reasoning.label}",
                tint = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Reasoning",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ReasoningLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.label) },
                    trailingIcon = {
                        if (level == reasoning) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    },
                )
            }
        }
    }
}
