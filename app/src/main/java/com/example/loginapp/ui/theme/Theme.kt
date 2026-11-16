package com.example.loginapp.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.loginapp.*

@Composable fun LoginAppTheme(content:@Composable ()->Unit){
    val base=Typography()
    fun style(s:androidx.compose.ui.text.TextStyle)=s.copy(fontFamily=Jakarta)
    MaterialTheme(colorScheme=lightColorScheme(primary=Green,onPrimary=Color.White,secondary=Mint,primaryContainer=PaleMint,background=Page,onBackground=Ink,surface=Color.White,onSurface=Ink,onSurfaceVariant=Muted,error=Red),
        typography=Typography(bodyLarge=style(base.bodyLarge),bodyMedium=style(base.bodyMedium),bodySmall=style(base.bodySmall),labelLarge=style(base.labelLarge),labelMedium=style(base.labelMedium),labelSmall=style(base.labelSmall),titleLarge=style(base.titleLarge),titleMedium=style(base.titleMedium),titleSmall=style(base.titleSmall),headlineLarge=style(base.headlineLarge),headlineMedium=style(base.headlineMedium),headlineSmall=style(base.headlineSmall)),content=content)
}
