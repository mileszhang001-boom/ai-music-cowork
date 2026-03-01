package com.music.localmusic.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.music.core.api.models.MusicHints
import com.music.localmusic.LocalMusicModule
import com.music.localmusic.models.Track
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LocalMusicDemoScreen()
                }
            }
        }
    }
}

@Composable
fun LocalMusicDemoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasPermission by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("等待权限授权...") }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var trackCount by remember { mutableStateOf(0) }
    var searchKeyword by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("") }
    var selectedTempo by remember { mutableStateOf("") }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        
        hasPermission = readStorage
        
        if (readStorage) {
            statusMessage = "正在初始化..."
            val success = LocalMusicModule.initialize()
            isInitialized = success
            if (success) {
                trackCount = LocalMusicModule.getTrackCount()
                statusMessage = "已初始化，共 $trackCount 首曲目"
            } else {
                statusMessage = "初始化失败，请检查数据库文件"
            }
        } else {
            statusMessage = "存储权限被拒绝"
        }
    }
    
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (allGranted) {
            hasPermission = true
            statusMessage = "正在初始化..."
            val success = LocalMusicModule.initialize()
            isInitialized = success
            if (success) {
                trackCount = LocalMusicModule.getTrackCount()
                statusMessage = "已初始化，共 $trackCount 首曲目"
            } else {
                statusMessage = "初始化失败，请检查数据库文件"
            }
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "本地音乐检索 Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        when {
                            isInitialized -> Color.Green
                            hasPermission -> Color.Yellow
                            else -> Color.Red
                        },
                        RoundedCornerShape(5.dp)
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusMessage,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            label = { Text("搜索关键词") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (isInitialized && searchKeyword.isNotBlank()) {
                    scope.launch {
                        val repository = LocalMusicModule.getRepository()
                        tracks = repository?.searchTracks(searchKeyword) ?: emptyList()
                        statusMessage = "找到 ${tracks.size} 首匹配曲目"
                    }
                }
            }),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "流派筛选:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        val genres = listOf("pop", "rock", "jazz", "classical", "electronic", "hip-hop")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            genres.forEach { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { selectedGenre = if (selectedGenre == genre) "" else genre },
                    label = { Text(genre, fontSize = 11.sp) }
                )
            }
        }
        
        Text(
            text = "节奏筛选:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        val tempos = listOf("slow", "moderate", "fast")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tempos.forEach { tempo ->
                FilterChip(
                    selected = selectedTempo == tempo,
                    onClick = { selectedTempo = if (selectedTempo == tempo) "" else tempo },
                    label = { Text(tempo, fontSize = 12.sp) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (isInitialized) {
                        scope.launch {
                            val repository = LocalMusicModule.getRepository()
                            val hints = MusicHints(
                                genres = if (selectedGenre.isNotEmpty()) listOf(selectedGenre) else null,
                                tempo = selectedTempo.ifEmpty { null }
                            )
                            tracks = repository?.queryTracks(hints) ?: emptyList()
                            statusMessage = "找到 ${tracks.size} 首匹配曲目"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isInitialized
            ) {
                Text("按条件查询")
            }
            
            Button(
                onClick = {
                    if (isInitialized) {
                        scope.launch {
                            val repository = LocalMusicModule.getRepository()
                            tracks = repository?.getAllTracks(20) ?: emptyList()
                            statusMessage = "加载 ${tracks.size} 首曲目"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isInitialized
            ) {
                Text("显示全部")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "搜索结果 (${tracks.size}):",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tracks) { track ->
                TrackItem(track)
            }
        }
    }
}

@Composable
fun TrackItem(track: Track) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = track.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Text(
                text = track.artist,
                fontSize = 14.sp,
                color = Color.Gray,
                maxLines = 1
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                track.genre?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                track.bpm?.let {
                    Text(
                        text = "$it BPM",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                track.energy?.let {
                    Text(
                        text = "能量: ${"%.2f".format(it)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                track.valence?.let {
                    Text(
                        text = "情绪: ${"%.2f".format(it)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            track.moodTags?.takeIf { it.isNotEmpty() }?.let { tags ->
                Text(
                    text = "情绪标签: ${tags.take(3).joinToString(", ")}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
