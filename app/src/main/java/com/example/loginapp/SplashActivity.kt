package com.example.loginapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val GreenPrimary = Color(0xFF006C49)

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar / nav bar putih
        window.statusBarColor = android.graphics.Color.parseColor("#006C49")
        window.navigationBarColor = android.graphics.Color.parseColor("#006C49")

        setContent {
            SplashScreen(onFinish = {
                // Pindah ke MainActivity setelah animasi selesai
                startActivity(Intent(this, MainActivity::class.java))
                finish() // tutup SplashActivity agar tidak bisa kembali
            })
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    // Animasi fade-in logo
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOut),
        label = "fade"
    )
    // Animasi scale logo
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Timer: tunggu 2.5 detik lalu lanjut ke Login
    LaunchedEffect(Unit) {
        delay(2500)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
        ) {
            // Logo / Ikon aplikasi (kotak hijau tua dengan inisial)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🥗",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nama Aplikasi
            Text(
                text = "NutriCare",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Tagline
            Text(
                text = "Nutrisi & Kesehatan Harian",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Loading indicator di bawah layar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .width(120.dp)
                        .height(3.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "v1.0.0",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

