package com.hololingo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hololingo.core.connectivity.ConnectionState
import com.hololingo.core.connectivity.DeviceRole
import com.hololingo.core.connectivity.NearbyConnectionManager
import com.hololingo.core.connectivity.NearbyDevice
import com.hololingo.core.connectivity.ViewerCommand
import com.hololingo.core.data.AnswerEvaluator
import com.hololingo.core.data.ConceptRepository
import com.hololingo.core.recognition.HandwritingRecognizer
import com.hololingo.feature.practice.PracticeRoute

private val Ink = Color(0xFF182225)
private val Mint = Color(0xFF45D6A8)
private val DeepGreen = Color(0xFF0B3B33)
private val Paper = Color(0xFFF4F1E8)
private val Muted = Color(0xFF64706E)

private enum class AppPage { ROLE, CONTROLLER, PRACTICE, VIEWER }

@Composable
fun HoloLingoApp(
    repository: ConceptRepository,
    recognizer: HandwritingRecognizer,
    evaluator: AnswerEvaluator,
    connectionManager: NearbyConnectionManager,
) {
    val context = LocalContext.current
    val connectionState by connectionManager.state.collectAsState()
    var page by remember { mutableStateOf(AppPage.ROLE) }
    var pendingRole by remember { mutableStateOf<DeviceRole?>(null) }
    var permissionError by remember { mutableStateOf<String?>(null) }
    var activeScene by remember { mutableStateOf<ViewerCommand.PlayScene?>(null) }
    val permissions = remember { requiredNearbyPermissions() }

    fun start(role: DeviceRole) {
        permissionError = null
        when (role) {
            DeviceRole.LEARNING_TABLET -> {
                page = AppPage.CONTROLLER
                connectionManager.startController("${Build.MODEL} 학습 태블릿")
            }
            DeviceRole.HOLOGRAM_VIEWER -> {
                page = AppPage.VIEWER
                connectionManager.startViewer("${Build.MODEL} 홀로그램")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val role = pendingRole
        pendingRole = null
        if (result.values.all { it } && role != null) start(role)
        else permissionError = "주변 기기를 연결하려면 근처 기기 권한을 허용해 주세요."
    }

    fun requestStart(role: DeviceRole) {
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            start(role)
        } else {
            pendingRole = role
            permissionLauncher.launch(permissions)
        }
    }

    DisposableEffect(connectionManager) { onDispose(connectionManager::disconnect) }
    LaunchedEffect(connectionManager) {
        connectionManager.commands.collect { command ->
            if (command is ViewerCommand.PlayScene) activeScene = command
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { inset ->
        Box(Modifier.fillMaxSize().padding(inset)) {
            when (page) {
                AppPage.ROLE -> RoleSelectionScreen(
                    error = permissionError,
                    onLearningSelected = { requestStart(DeviceRole.LEARNING_TABLET) },
                    onViewerSelected = { requestStart(DeviceRole.HOLOGRAM_VIEWER) },
                )
                AppPage.CONTROLLER -> ControllerConnectionScreen(
                    state = connectionState,
                    onDeviceSelected = connectionManager::requestConnection,
                    onAccept = connectionManager::acceptPairing,
                    onReject = connectionManager::rejectPairing,
                    onRetry = { connectionManager.startController(controllerName()) },
                    onBack = {
                        connectionManager.disconnect()
                        page = AppPage.ROLE
                    },
                    onStartLearning = { page = AppPage.PRACTICE },
                )
                AppPage.PRACTICE -> Column(Modifier.fillMaxSize()) {
                    ConnectedBar(
                        deviceName = (connectionState as? ConnectionState.Connected)?.deviceName ?: "출력 기기",
                        onDisconnect = {
                            connectionManager.disconnect()
                            page = AppPage.ROLE
                        },
                    )
                    Box(Modifier.weight(1f)) {
                        PracticeRoute(
                            repository = repository,
                            recognizer = recognizer,
                            evaluator = evaluator,
                            onCorrectAnswer = { scene, conceptId ->
                                connectionManager.send(ViewerCommand.PlayScene(scene.id.value, conceptId.value))
                            },
                        )
                    }
                }
                AppPage.VIEWER -> ViewerScreen(
                    state = connectionState,
                    activeScene = activeScene,
                    onAccept = connectionManager::acceptPairing,
                    onReject = connectionManager::rejectPairing,
                    onRetry = {
                        activeScene = null
                        connectionManager.startViewer(viewerName())
                    },
                    onBack = {
                        connectionManager.disconnect()
                        activeScene = null
                        page = AppPage.ROLE
                    },
                )
            }
        }
    }
}

@Composable
private fun RoleSelectionScreen(error: String?, onLearningSelected: () -> Unit, onViewerSelected: () -> Unit) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(40.dp),
        ) {
            Brand()
            Spacer(Modifier.height(36.dp))
            Text("이 기기에서 무엇을 할까요?", fontWeight = FontWeight.Black, fontSize = 30.sp, color = Ink)
            Text("두 기기에서 각각 다른 역할을 선택해 주세요.", color = Muted)
            Spacer(Modifier.height(28.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
            ) {
                RoleCard("학습 태블릿", "글자를 쓰고 정답을 확인해요", "출력 기기 연결", onLearningSelected, Modifier.weight(1f))
                RoleCard("홀로그램 화면", "연결을 기다리고 3D를 재생해요", "주변에 기기 광고", onViewerSelected, Modifier.weight(1f))
            }
            error?.let {
                Spacer(Modifier.height(18.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    step: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.height(220.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(48.dp).background(Mint.copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (title.startsWith("학습")) "✎" else "◇", color = DeepGreen, fontSize = 24.sp)
            }
            Column {
                Text(title, fontWeight = FontWeight.Black, fontSize = 23.sp, color = Ink)
                Text(description, color = Muted)
            }
            Text("$step  →", color = DeepGreen, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ControllerConnectionScreen(
    state: ConnectionState,
    onDeviceSelected: (NearbyDevice) -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onStartLearning: () -> Unit,
) {
    ConnectionShell("출력 기기 연결", onBack) {
        when (state) {
            is ConnectionState.Discovering -> DeviceList(state.devices, onDeviceSelected)
            is ConnectionState.Pairing -> PairingPanel(state, onAccept, onReject)
            is ConnectionState.Connected -> ConnectedPanel(state.deviceName, onStartLearning)
            is ConnectionState.Error -> ErrorPanel(state.message, onRetry)
            else -> LoadingPanel("주변의 홀로그램 화면을 찾고 있어요")
        }
    }
}

@Composable
private fun DeviceList(devices: List<NearbyDevice>, onDeviceSelected: (NearbyDevice) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("발견된 기기", color = Muted, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (devices.isEmpty()) {
            LoadingPanel("출력 기기에서 ‘홀로그램 화면’을 먼저 선택해 주세요")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices, key = { it.endpointId }) { device ->
                    Card(
                        onClick = { onDeviceSelected(device) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(22.dp)) {
                            Box(Modifier.size(42.dp).background(Mint.copy(alpha = .2f), CircleShape))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Bold, color = Ink)
                                Text("연결 가능", color = Muted, fontSize = 13.sp)
                            }
                            Text("선택  →", color = DeepGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingPanel(state: ConnectionState.Pairing, onAccept: () -> Unit, onReject: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("두 기기의 번호가 같은지 확인하세요", fontWeight = FontWeight.Black, fontSize = 25.sp, color = Ink)
        Text(state.deviceName, color = Muted)
        Spacer(Modifier.height(26.dp))
        Surface(color = DeepGreen, shape = RoundedCornerShape(24.dp)) {
            Text(
                state.authenticationDigits.chunked(3).joinToString("  "),
                color = Mint,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 30.dp),
            )
        }
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onReject) { Text("취소") }
            Button(
                onClick = onAccept,
                enabled = !state.isLocallyAccepted,
                colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = DeepGreen),
            ) {
                Text(
                    if (state.isLocallyAccepted) "상대 기기 확인 대기 중" else "번호가 같아요 · 연결",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ConnectedPanel(deviceName: String, onStartLearning: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(72.dp).background(Mint.copy(alpha = .22f), CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = DeepGreen, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("연결이 완료됐어요", fontWeight = FontWeight.Black, fontSize = 26.sp, color = Ink)
        Text(deviceName, color = Muted)
        Spacer(Modifier.height(26.dp))
        Button(onClick = onStartLearning, colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)) {
            Text("천자문 학습 시작", modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun ViewerScreen(
    state: ConnectionState,
    activeScene: ViewerCommand.PlayScene?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Surface(color = Color(0xFF071C19), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(32.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) { Text("역할 다시 선택") }
            when (state) {
                is ConnectionState.Advertising -> ViewerWaiting(state.deviceName, Modifier.align(Alignment.Center))
                is ConnectionState.Pairing -> Surface(
                    color = Paper,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 720.dp),
                ) {
                    Box(Modifier.padding(36.dp)) { PairingPanel(state, onAccept, onReject) }
                }
                is ConnectionState.Connected -> ViewerCanvas(state.deviceName, activeScene)
                is ConnectionState.Error -> Box(Modifier.align(Alignment.Center)) { ErrorPanel(state.message, onRetry) }
                else -> Box(Modifier.align(Alignment.Center)) { LoadingPanel("홀로그램 화면을 준비하고 있어요", dark = true) }
            }
        }
    }
}

@Composable
private fun ViewerWaiting(deviceName: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        CircularProgressIndicator(color = Mint, modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(24.dp))
        Text("학습 태블릿을 기다리는 중", color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
        Text("광고 중 · $deviceName", color = Color.White.copy(alpha = .62f))
        Spacer(Modifier.height(18.dp))
        Text("학습 태블릿에서 ‘출력 기기 연결’을 선택하세요", color = Mint)
    }
}

@Composable
private fun ViewerCanvas(deviceName: String, activeScene: ViewerCommand.PlayScene?) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("● $deviceName 연결됨", color = Mint, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (activeScene == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("정답을 기다리고 있어요", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    Text("학습 태블릿에서 정답을 맞히면 3D 장면이 재생됩니다", color = Color.White.copy(alpha = .6f))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HologramMountain()
                    Text(activeScene.conceptId, color = Mint, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    Text("3D 재생 중 · ${activeScene.sceneId}", color = Color.White.copy(alpha = .6f))
                }
            }
        }
    }
}

@Composable
private fun HologramMountain() {
    Canvas(
        Modifier.size(420.dp, 270.dp).background(
            Brush.radialGradient(listOf(Mint.copy(alpha = .16f), Color.Transparent)),
        ),
    ) {
        val mountain = Path().apply {
            moveTo(size.width * .08f, size.height * .86f)
            lineTo(size.width * .48f, size.height * .12f)
            lineTo(size.width * .94f, size.height * .86f)
            close()
        }
        drawPath(mountain, Mint.copy(alpha = .2f))
        drawPath(mountain, Mint, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
        drawLine(Mint.copy(alpha = .5f), Offset(0f, size.height * .86f), Offset(size.width, size.height * .86f), 2f)
    }
}

@Composable
private fun ConnectionShell(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(250.dp).fillMaxHeight()) {
                Brand()
                Spacer(Modifier.height(40.dp))
                Text("연결 순서", color = Muted, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                listOf("출력 기기 선택", "주변 기기 찾기", "인증번호 확인", "연결 완료").forEachIndexed { index, label ->
                    Text("${index + 1}. $label", color = if (index == 1) DeepGreen else Muted, modifier = Modifier.padding(vertical = 7.dp))
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onBack) { Text("역할 다시 선택") }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .82f)),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(Modifier.fillMaxSize().padding(34.dp)) {
                    Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    Spacer(Modifier.height(28.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun LoadingPanel(message: String, dark: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(48.dp),
    ) {
        CircularProgressIndicator(color = Mint)
        Spacer(Modifier.height(18.dp))
        Text(message, color = if (dark) Color.White else Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("다시 시도") }
    }
}

@Composable
private fun ConnectedBar(deviceName: String, onDisconnect: () -> Unit) {
    Surface(color = DeepGreen) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
        ) {
            Text("●  $deviceName 연결됨", color = Mint, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onDisconnect) { Text("연결 종료", color = Color.White) }
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(DeepGreen, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Text("H", color = Mint, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text("HOLOLINGO", color = Ink, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 20.sp)
    }
}

private fun controllerName() = "${Build.MODEL} 학습 태블릿"
private fun viewerName() = "${Build.MODEL} 홀로그램"

private fun requiredNearbyPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
        add(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
    if (Build.VERSION.SDK_INT >= 37) add("android.permission.ACCESS_LOCAL_NETWORK")
}.toTypedArray()
