package com.example.iagointelbras.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.iagointelbras.R

@Composable
fun CasaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = colorResource(R.color.casa_primary),
            onPrimary = Color.White,
            primaryContainer = colorResource(R.color.casa_primary_container),
            secondary = colorResource(R.color.casa_secondary),
            background = colorResource(R.color.casa_background),
            surface = Color.White,
            error = colorResource(R.color.casa_error)
        ),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
private fun CasaThemePreview() {
    CasaTheme { Text(stringResource(R.string.app_name)) }
}
