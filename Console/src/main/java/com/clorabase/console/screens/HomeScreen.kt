package com.clorabase.console.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clorabase.console.R
import com.clorabase.console.theme.White
import androidx.core.net.toUri


@Preview(showBackground = true)
@Composable
fun HomeScreen(onNavigateTo: (screen: Screen) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillWidthOfParent(10.dp)
                    .height(210.dp)
                    .offset(y = (-10).dp)
                    .background(Color(0xFF00CC06))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Choose a feature to integrate in your app",
                        fontSize = 30.sp,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 35.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No account needed!",
                        color = White,
                        modifier = Modifier.padding(bottom = 30.dp)
                    )
                }
            }
        }

        item {
            HomeCard(
                imageResId = R.drawable.database,
                title = "Database",
                description = "In our no-sql database, you can store your data in a easy and secure way.",
                onClick = { onNavigateTo(Screen.Database) }
            )
        }
        item {
            HomeCard(
                imageResId = R.drawable.storage,
                title = "Storage",
                description = "Upload and download files required by the app anytime.",
                onClick = { onNavigateTo(Screen.Storage) }
            )
        }
        item {
            HomeCard(
                imageResId = R.drawable.messaging,
                title = "In-App messaging",
                description = "Send message inside app to engage user, alertdialogs for notifying something",
                onClick = { onNavigateTo(Screen.InAppMessaging) }
            )
        }
        item {
            HomeCard(
                imageResId = R.drawable.inapp,
                title = "In-App updates",
                description = "Notify users about new update directly from the console. They can update it from the app directly",
                onClick = { onNavigateTo(Screen.InAppUpdates) }
            )
        }
        item {
            val context = LocalContext.current;
            TextButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://clorabase-docs.netlify.app".toUri()))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("See documentation", fontSize = 20.sp)
                Image(Icons.Filled.ArrowForward, "Arrow")
            }
        }
    }
}

@Composable
fun HomeCard(
    imageResId: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 25.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

fun Modifier.fillWidthOfParent(parentPadding: Dp) = layout { measurable, constraints ->
    // This is to force layout to go beyond the borders of its parent
    val placeable = measurable.measure(
        constraints.copy(
            maxWidth = constraints.maxWidth + 2 * parentPadding.roundToPx(),
        ),
    )
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}
