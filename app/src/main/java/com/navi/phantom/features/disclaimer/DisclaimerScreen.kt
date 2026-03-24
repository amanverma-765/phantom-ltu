package com.navi.phantom.features.disclaimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(
    onAgree: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    showAgreeButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Disclaimer") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showAgreeButton) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = onAgree,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "I Agree",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Warning header
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DISCLAIMER AND TERMS OF USE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This application (\"Phantom LTU\") is provided strictly for educational, " +
                    "software testing, and application development purposes only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "By proceeding, you acknowledge and agree to the following terms:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            DisclaimerSection(
                number = "1",
                title = "INTENDED USE",
                body = "This software is designed exclusively for developers and testers who need to " +
                    "simulate GPS coordinates for testing location-based features in their own " +
                    "applications. Any use beyond this intended purpose is strictly prohibited."
            )

            DisclaimerSection(
                number = "2",
                title = "PROHIBITED ACTIVITIES",
                body = "You shall NOT use this application to:\n" +
                    "• Falsify your location for attendance or work verification systems\n" +
                    "• Deceive any person, organization, or service\n" +
                    "• Violate any applicable local, state, national, or international law\n" +
                    "• Circumvent security measures of any third-party application\n" +
                    "• Engage in any form of fraud or misrepresentation"
            )

            DisclaimerSection(
                number = "3",
                title = "NO WARRANTY",
                body = "This software is provided \"AS IS\" without warranty of any kind, express or " +
                    "implied. The developers make no guarantees regarding the reliability, accuracy, " +
                    "or completeness of this software."
            )

            DisclaimerSection(
                number = "4",
                title = "USER RESPONSIBILITY",
                body = "You are solely responsible for your use of this application and any consequences " +
                    "that may arise from such use. The developers shall not be held liable for any " +
                    "misuse of this software."
            )

            DisclaimerSection(
                number = "5",
                title = "LIABILITY LIMITATION",
                body = "In no event shall the developers be liable for any direct, indirect, incidental, " +
                    "special, or consequential damages arising from the use or inability to use this software."
            )

            DisclaimerSection(
                number = "6",
                title = "COMPLIANCE",
                body = "You agree to comply with all applicable laws and regulations in your jurisdiction " +
                    "regarding the use of GPS spoofing software."
            )

            DisclaimerSection(
                number = "7",
                title = "ACCEPTANCE",
                body = "By tapping \"I Agree\" below, you confirm that you have read, understood, and agree " +
                    "to abide by these terms. If you do not agree, please uninstall this application immediately."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisclaimerSection(
    number: String,
    title: String,
    body: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "$number. $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
