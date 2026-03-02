package com.music.appmain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.layer3.api.Layer3Config
import com.music.appmain.permission.PermissionManager
import com.music.appmain.permission.PermissionsState
import com.music.appmain.ui.Layer1DataPanel
import com.music.appmain.ui.Layer2DataPanel
import com.music.appmain.ui.Layer3DataPanel
import com.music.appmain.ui.StatusIndicator
import com.music.perception.api.PerceptionConfig
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    
    private lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionManager = PermissionManager(this)
        
        setContent {
            MaterialTheme {
                MainScreen(
                    permissionManager = permissionManager,
                    lifecycleOwner = this
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        permissionManager.checkPermissions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    permissionManager: PermissionManager,
    lifecycleOwner: ComponentActivity
) {
    val permissionsState by permissionManager.permissionsState.collectAsState()
    
    val perceptionConfig = remember {
        PerceptionConfig.Builder()
            .setIpCameraUrl("")
            .setIpCameraAuth("", "")
            .setDashScopeApiKey("")
            .setRefreshInterval(3000L)
            .build()
    }
    
    val layer3Config = remember {
        Layer3Config.Builder()
            .build()
    }
    
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(
            application = lifecycleOwner.application,
            lifecycleOwner = lifecycleOwner,
            perceptionConfig = perceptionConfig,
            layer3Config = layer3Config
        )
    )
    
    val isInitialized by viewModel.isInitializedFlow.collectAsState()
    val isRunning by viewModel.isRunningFlow.collectAsState()
    val engineState by viewModel.engineStateFlow.collectAsState()
    val standardizedSignals by viewModel.standardizedSignalsFlow.collectAsState()
    val sceneDescriptor by viewModel.sceneDescriptorFlow.collectAsState()
    val effectCommands by viewModel.effectCommandsFlow.collectAsState()
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(permissionsState.allGranted) {
        if (!permissionsState.allGranted) {
            showPermissionDialog = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "音乐推荐系统",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomControlBar(
                isRunning = isRunning,
                isInitialized = isInitialized,
                permissionsGranted = permissionsState.allGranted,
                onStart = { viewModel.start() },
                onStop = { viewModel.stop() },
                onRequestPermissions = {
                    permissionManager.requestPermissions()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            StatusIndicator(
                permissionsState = permissionsState,
                isInitialized = isInitialized,
                isRunning = isRunning,
                engineState = engineState,
                modifier = Modifier.fillMaxWidth()
            )
            
            Layer1DataPanel(
                signals = standardizedSignals,
                modifier = Modifier.fillMaxWidth()
            )
            
            Layer2DataPanel(
                sceneDescriptor = sceneDescriptor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Layer3DataPanel(
                effectCommands = effectCommands,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showPermissionDialog && !permissionsState.allGranted) {
        PermissionRequestDialog(
            permissionsState = permissionsState,
            permissionManager = permissionManager,
            onDismiss = { showPermissionDialog = false },
            onGranted = { showPermissionDialog = false }
        )
    }
}

@Composable
private fun BottomControlBar(
    isRunning: Boolean,
    isInitialized: Boolean,
    permissionsGranted: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!permissionsGranted) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("授权权限")
                }
            } else if (!isInitialized) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                ) {
                    Text("初始化中...")
                }
            } else if (isRunning) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("停止")
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("启动")
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestDialog(
    permissionsState: PermissionsState,
    permissionManager: PermissionManager,
    onDismiss: () -> Unit,
    onGranted: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("需要权限") },
        text = {
            Column {
                Text("应用需要以下权限才能正常运行：")
                Spacer(modifier = Modifier.height(8.dp))
                permissionsState.deniedPermissions.forEach { permission ->
                    val name = permissionManager.getPermissionName(permission)
                    val desc = permissionManager.getPermissionDescription(permission)
                    Text(
                        text = "• $name: $desc",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    permissionManager.requestPermissions { results ->
                        if (results.all { it.isGranted }) {
                            onGranted()
                        }
                    }
                }
            ) {
                Text("授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}
