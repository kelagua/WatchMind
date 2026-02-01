package com.watchmind

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.icons.Icons
import androidx.wear.compose.material.icons.rounded.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(applicationContext)
                    )
                    WatchMindApp(viewModel)
                }
            }
        }
    }
}

@Immutable
data class ChatMessage(
    val role: String,
    val content: String
)

enum class ScreenMode {
    Chat,
    Settings
}

@Composable
fun WatchMindApp(viewModel: ChatViewModel) {
    var screenMode by remember { mutableStateOf(ScreenMode.Chat) }
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(screenMode = screenMode, onToggle = {
            screenMode = if (screenMode == ScreenMode.Chat) ScreenMode.Settings else ScreenMode.Chat
        })
        when (screenMode) {
            ScreenMode.Chat -> ChatScreen(viewModel)
            ScreenMode.Settings -> SettingsScreen(viewModel)
        }
    }
}

@Composable
private fun AppHeader(screenMode: ScreenMode, onToggle: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = if (screenMode == ScreenMode.Chat) {
                    stringResource(id = R.string.chat)
                } else {
                    stringResource(id = R.string.settings)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(),
        actions = {
            Button(onClick = onToggle, modifier = Modifier.padding(end = 8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state = viewModel.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        StatusRow(state.statusText)
        Spacer(modifier = Modifier.height(8.dp))
        ScalingLazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages) { message ->
                MessageBubble(message)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = state.currentInput,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = stringResource(id = R.string.hint_message)) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { viewModel.sendMessage() },
                enabled = state.canSend,
                modifier = Modifier.height(52.dp)
            ) {
                Text(text = stringResource(id = R.string.send))
            }
        }
    }
}

@Composable
private fun StatusRow(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = status)
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val label = if (message.role == "user") "You" else "Assistant"
    Chip(
        onClick = {},
        colors = ChipDefaults.secondaryChipColors(),
        label = { Text(text = "$label: ${message.content}") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    val state = viewModel.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = viewModel::updateApiKey,
            label = { Text(text = stringResource(id = R.string.hint_api_key)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = viewModel::updateBaseUrl,
            label = { Text(text = stringResource(id = R.string.hint_base_url)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.model,
            onValueChange = viewModel::updateModel,
            label = { Text(text = stringResource(id = R.string.hint_model)) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = viewModel::persistSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.save))
        }
    }
}

class ChatViewModel(private val appContext: Context) : ViewModel() {
    private val client = OkHttpClient()
    private val preferences = appContext.getSharedPreferences("watchmind_prefs", Context.MODE_PRIVATE)

    var uiState by mutableStateOf(
        ChatUiState(
            apiKey = preferences.getString("api_key", "") ?: "",
            baseUrl = preferences.getString("base_url", "https://api.openai.com/v1") ?: "",
            model = preferences.getString("model", "gpt-4o-mini") ?: "",
            statusText = appContext.getString(R.string.status_idle)
        ).withSendState()
    )
        private set

    fun updateInput(value: String) {
        uiState = uiState.copy(currentInput = value).withSendState()
    }

    fun updateApiKey(value: String) {
        uiState = uiState.copy(apiKey = value).withSendState()
    }

    fun updateBaseUrl(value: String) {
        uiState = uiState.copy(baseUrl = value)
    }

    fun updateModel(value: String) {
        uiState = uiState.copy(model = value)
    }

    fun persistSettings() {
        preferences.edit()
            .putString("api_key", uiState.apiKey)
            .putString("base_url", uiState.baseUrl)
            .putString("model", uiState.model)
            .apply()
        uiState = uiState.copy(statusText = appContext.getString(R.string.status_idle))
    }

    fun sendMessage() {
        val trimmed = uiState.currentInput.trim()
        if (trimmed.isEmpty()) return
        if (uiState.apiKey.isBlank()) {
            uiState = uiState.copy(statusText = appContext.getString(R.string.status_error))
            return
        }

        val updatedMessages = uiState.messages + ChatMessage("user", trimmed)
        uiState = uiState.copy(
            messages = updatedMessages,
            currentInput = "",
            statusText = appContext.getString(R.string.status_sending)
        ).withSendState()

        viewModelScope.launch(Dispatchers.IO) {
            val responseText = runChatRequest(updatedMessages)
            val status = if (responseText.errorMessage != null) {
                appContext.getString(R.string.status_error)
            } else {
                appContext.getString(R.string.status_idle)
            }
            val finalMessages = if (responseText.reply != null) {
                updatedMessages + ChatMessage("assistant", responseText.reply)
            } else {
                updatedMessages
            }
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(messages = finalMessages, statusText = status).withSendState()
            }
        }
    }

    private fun runChatRequest(messages: List<ChatMessage>): ChatResponse {
        val payload = JSONObject().apply {
            put("model", uiState.model)
            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })
        }

        val requestBody = payload.toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${uiState.baseUrl.trimEnd('/')}/chat/completions")
            .addHeader("Authorization", "Bearer ${uiState.apiKey}")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    return ChatResponse(errorMessage = "HTTP ${response.code}")
                }
                val reply = JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                ChatResponse(reply = reply.trim())
            }
        } catch (exception: Exception) {
            ChatResponse(errorMessage = exception.message ?: "Unknown error")
        }
    }
}

@Immutable
data class ChatUiState(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val currentInput: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val statusText: String = "",
    val canSend: Boolean = false
)

private fun ChatUiState.withSendState(): ChatUiState {
    val canSend = apiKey.isNotBlank() && currentInput.isNotBlank()
    return copy(canSend = canSend)
}

@Immutable
data class ChatResponse(
    val reply: String? = null,
    val errorMessage: String? = null
)

class ChatViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
