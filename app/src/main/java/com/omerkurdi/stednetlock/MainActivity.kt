package com.omerkurdi.stednetlock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omerkurdi.stednetlock.data.NetworkProfile
import com.omerkurdi.stednetlock.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NetworkSwitcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NetworkSwitcherScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatus()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NetworkSwitcherScreen(viewModel: NetworkSwitcherViewModel) {
    val context = LocalContext.current
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsStateWithLifecycle()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
    val isServiceBound by viewModel.isServiceBound.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val selectedSimSubId by viewModel.selectedSimSubId.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showHowToStart by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    // Automatically collapse and hide setup guide when connected successfully
    val isShizukuFullyConnected = isShizukuRunning && isPermissionGranted && isServiceBound
    LaunchedEffect(isShizukuFullyConnected) {
        if (isShizukuFullyConnected) {
            showHowToStart = false
        }
    }

    // State for runtime permission
    var hasPhonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasPhonePermission = granted
            viewModel.shizukuManager.refreshSubscriptions()
        }
    )

    LaunchedEffect(hasPhonePermission) {
        if (!hasPhonePermission) {
            phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                actions = {
                    IconButton(
                        onClick = { showSupportDialog = true },
                        modifier = Modifier.testTag("support_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Support & Contact",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_profile_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Section 0: Current Locked Profile status
            item {
                val activeProfile = profiles.find { it.id == activeProfileId }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("active_profile_status_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT LOCK STATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeProfile?.name ?: "No Profile Locked (System Default)",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Section 1: Shizuku Service Status
            item {
                var forceShowStatus by remember { mutableStateOf(false) }
                val showShizukuCard = !(isShizukuRunning && isPermissionGranted && isServiceBound) || forceShowStatus

                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!showShizukuCard) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .clickable { forceShowStatus = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Shizuku Engine Connected & Authorized",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Show Status",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showShizukuCard,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("shizuku_status_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ Shizuku Connection Service",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isShizukuRunning && isPermissionGranted && isServiceBound) {
                                        TextButton(onClick = { forceShowStatus = false }) {
                                            Text("Hide Status", fontSize = 11.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Status indicators
                                StatusRow(
                                    label = "Shizuku Status",
                                    value = if (isShizukuRunning) "Running" else "Not Running",
                                    isSuccess = isShizukuRunning,
                                    testTag = "shizuku_running_status"
                                )
                                StatusRow(
                                    label = "Shizuku Permission",
                                    value = if (isPermissionGranted) "Authorized" else "Unauthorized",
                                    isSuccess = isPermissionGranted,
                                    testTag = "shizuku_permission_status"
                                )
                                StatusRow(
                                    label = "Binder Service Bound",
                                    value = if (isServiceBound) "Connected" else "Disconnected",
                                    isSuccess = isServiceBound,
                                    testTag = "shizuku_service_status"
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!isPermissionGranted && isShizukuRunning) {
                                        Button(
                                            onClick = { viewModel.requestPermission() },
                                            modifier = Modifier.weight(1f).testTag("grant_permission_btn")
                                        ) {
                                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Authorize", fontSize = 12.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { showHowToStart = !showHowToStart },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        modifier = Modifier.weight(1f).testTag("how_to_start_btn")
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Guide", fontSize = 12.sp)
                                    }
                                }

                                AnimatedVisibility(visible = showHowToStart) {
                                    Column(
                                        modifier = Modifier
                                            .padding(top = 12.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "How to start Shizuku:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "1. Enable 'Developer options' and 'Wireless debugging' in Android developer options.\n" +
                                                    "2. Open Shizuku application, tap 'Pairing' and then tap 'Start'.\n" +
                                                    "3. Or, execute this ADB command on your computer to start Shizuku instantly:\n",
                                            fontSize = 12.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "adb shell sh /sdcard/Android/data/rikka.shizuku/files/start.sh",
                                                color = Color(0xFF00FF00),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                modifier = Modifier.testTag("adb_command_text")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Active Sim Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sim_selector_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📱 Target SIM Card (Subscription)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap a SIM card to set as active target:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        activeSims.forEach { sim ->
                            val isSelected = sim.subId == selectedSimSubId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { viewModel.selectSim(sim.subId) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sim.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "SubId: ${sim.subId} | Slot: ${sim.slotIndex + 1}",
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Profiles Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚙️ Choose Network Profile",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${profiles.size} Profile(s)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Section 4: Profiles list items
            if (profiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Initializing default profiles...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        isTargeting = true,
                        isActiveProfile = profile.id == activeProfileId,
                        onApply = {
                            viewModel.applyNetworkProfile(profile) { success ->
                                if (success) {
                                    Toast.makeText(context, "Network Profile Applied successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to apply. Verify Shizuku service is running and authorized.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onDelete = {
                            viewModel.deleteProfile(profile)
                            Toast.makeText(context, "Deleted profile: ${profile.name}", Toast.LENGTH_SHORT).show()
                        },
                        onToggleWidget = { show ->
                            viewModel.toggleShowOnWidget(profile.id, show)
                        }
                    )
                }
            }

            // Section 5: Support creator card
            item {
                SupportCard(
                    onOpenLink = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://linktr.ee/OmerKurdi79")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    // Dialog for adding custom network mode profile
    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, b2g, b3g, b4g, b5g ->
                viewModel.addNewProfile(name, b2g, b3g, b4g, b5g)
                showAddDialog = false
                Toast.makeText(context, "Custom profile created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog for support and donation info page
    if (showSupportDialog) {
        SupportDialog(
            onDismiss = { showSupportDialog = false },
            onOpenLink = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://linktr.ee/OmerKurdi79")
                )
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun StatusRow(label: String, value: String, isSuccess: Boolean, testTag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
fun ProfileCard(
    profile: NetworkProfile,
    isTargeting: Boolean,
    isActiveProfile: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onToggleWidget: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveProfile) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else if (profile.isDefaultAuto) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isActiveProfile) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("profile_card_${profile.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isActiveProfile || profile.isDefaultAuto) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isActiveProfile) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF00FF00))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Active Lock", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (profile.isDefaultAuto) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("Safety", fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modes: ${profile.getTechnologiesString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = profile.showOnWidget,
                            onCheckedChange = onToggleWidget,
                            modifier = Modifier.testTag("widget_switch_${profile.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Show on Widget",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!profile.isSystemDefault) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_profile_${profile.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("apply_profile_${profile.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActiveProfile) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = if (isActiveProfile) Icons.Default.CheckCircle else Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActiveProfile) "Locked & Active (Force Re-Lock)" else "Apply & Lock Network Mode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileDialog(onDismiss: () -> Unit, onSave: (String, Boolean, Boolean, Boolean, Boolean) -> Unit) {
    var profileName by remember { mutableStateOf("") }
    var is2g by remember { mutableStateOf(false) }
    var is3g by remember { mutableStateOf(false) }
    var is4g by remember { mutableStateOf(true) }
    var is5g by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_profile_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✨ Custom Network Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name (e.g., LTE + 5G)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_field")
                )

                Text(
                    text = "Select Network Bands to Enable:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CustomCheckboxRow(label = "5G NR (New Radio)", checked = is5g, onCheckedChange = { is5g = it }, tag = "chk_5g")
                CustomCheckboxRow(label = "4G LTE (Long Term Evolution)", checked = is4g, onCheckedChange = { is4g = it }, tag = "chk_4g")
                CustomCheckboxRow(label = "3G UMTS/HSPA/WCDMA", checked = is3g, onCheckedChange = { is3g = it }, tag = "chk_3g")
                CustomCheckboxRow(label = "2G GSM/GPRS/EDGE", checked = is2g, onCheckedChange = { is2g = it }, tag = "chk_2g")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (profileName.trim().isNotEmpty()) {
                                onSave(profileName.trim(), is2g, is3g, is4g, is5g)
                            }
                        },
                        enabled = profileName.trim().isNotEmpty() && (is2g || is3g || is4g || is5g),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp)
    }
}

@Composable
fun SupportCard(onOpenLink: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("support_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "SUPPORT THE CREATOR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Support Development",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "If StedNet Lock made your mobile network experience better, please consider supporting the creator. Your support keeps this tool free, open-source, ad-free, and active!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenLink,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("support_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Support & Contact (Linktree)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SupportDialog(onDismiss: () -> Unit, onOpenLink: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("support_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Thank you for using StedNet Lock!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Hi there! I created StedNet Lock to make mobile network switching and profile management clean, seamless, completely ad-free, and open to everyone.\n\n" +
                            "If you love using this utility and wish to support continued development, get in touch, or say thanks with a donation, you can visit the Linktree page. Every contribution is deeply appreciated! 💖",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onOpenLink()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_open_linktree_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Linktree Page",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

