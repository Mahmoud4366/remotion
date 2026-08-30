package com.example.vpn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vpn.R
import com.example.vpn.VpnStatus
import com.example.vpn.VpnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: VpnViewModel, onNavigateToSettings: () -> Unit) {
    val state by viewModel.vpnState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Auto Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.auto_mode),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(id = R.string.auto_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isAutoMode,
                    onCheckedChange = { viewModel.setAutoMode(it) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Central Connect Button
            AnimatedConnectButton(
                status = state.status,
                onClick = {
                    if (state.status == VpnStatus.CONNECTED) viewModel.disconnect()
                    else viewModel.connect()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            val statusText = state.connectionPhase ?: when (state.status) {
                VpnStatus.DISCONNECTED -> stringResource(R.string.status_ready)
                VpnStatus.CONNECTING -> stringResource(R.string.status_connecting)
                VpnStatus.VERIFYING -> stringResource(R.string.status_verifying)
                VpnStatus.CONNECTED -> stringResource(R.string.status_connected)
                VpnStatus.ERROR -> stringResource(R.string.status_error)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (state.status == VpnStatus.CONNECTED) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
            )

            if (state.status == VpnStatus.CONNECTED) {
                Text(
                    text = "${stringResource(R.string.duration)}: ${formatDuration(state.duration)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info Cards (Ping, Down, Up, Server Info) - Only visible when connected
            if (state.status == VpnStatus.CONNECTED) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(title = stringResource(R.string.server), value = state.selectedProfile.take(15), modifier = Modifier.weight(1f))
                        InfoCard(title = stringResource(R.string.country), value = state.serverCountry ?: "-", modifier = Modifier.weight(1f))
                        InfoCard(title = stringResource(R.string.protocol), value = state.protocol, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(title = stringResource(R.string.ping), value = "${state.ping} ms", modifier = Modifier.weight(1f))
                        InfoCard(title = stringResource(R.string.download), value = "${state.downloadSpeed} KB/s", modifier = Modifier.weight(1f))
                        InfoCard(title = stringResource(R.string.upload), value = "${state.uploadSpeed} KB/s", modifier = Modifier.weight(1f))
                    }
                }
            } else {
                // Placeholder space
                Spacer(modifier = Modifier.height(120.dp))
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AnimatedConnectButton(status: VpnStatus, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == VpnStatus.CONNECTING || status == VpnStatus.VERIFYING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (status) {
            VpnStatus.CONNECTED -> Color(0xFF4CAF50)
            VpnStatus.CONNECTING, VpnStatus.VERIFYING -> Color(0xFFFFA000)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(500),
        label = "buttonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(220.dp)
    ) {
        // Outer glow
        if (status == VpnStatus.CONNECTING || status == VpnStatus.VERIFYING || status == VpnStatus.CONNECTED) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale * 0.95f)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = 0.4f))
            )
        }

        // Main Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable { onClick() }
        ) {
            val iconText = when (status) {
                VpnStatus.CONNECTED -> "ON"
                VpnStatus.CONNECTING, VpnStatus.VERIFYING -> "..."
                else -> "OFF"
            }
            Text(
                text = iconText,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
