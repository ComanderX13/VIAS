package com.pepinazos.vias

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Actividad principal
// ---------------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ViasApp()
                }
            }
        }
    }
}

@Composable
fun ViasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1565C0),
            primaryContainer = Color(0xFFD6E4FF),
            onPrimaryContainer = Color(0xFF0D2B5C),
            background = Color.White,
            surface = Color.White
        ),
        content = content
    )
}

// ---------------------------------------------------------------------------
// Sesión (prototipo: se guarda solo en el teléfono)
// ---------------------------------------------------------------------------

private fun prefs(ctx: Context): SharedPreferences =
    ctx.getSharedPreferences("vias_prefs", Context.MODE_PRIVATE)

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

// ---------------------------------------------------------------------------
// Navegación simple por estado
// ---------------------------------------------------------------------------

enum class Screen { Login, Home, Routes, Parking, Report }

@Composable
fun ViasApp() {
    val ctx = LocalContext.current
    var screen by remember {
        mutableStateOf(
            if (prefs(ctx).getBoolean("logged_in", false)) Screen.Home else Screen.Login
        )
    }

    BackHandler(enabled = screen != Screen.Home && screen != Screen.Login) {
        screen = Screen.Home
    }

    when (screen) {
        Screen.Login -> LoginScreen(onLoggedIn = { screen = Screen.Home })
        Screen.Home -> HomeScreen(onNavigate = { screen = it })
        Screen.Routes -> RoutesScreen(onBack = { screen = Screen.Home })
        Screen.Parking -> InfoScreen(
            title = "Parqueos",
            text = "Aún no implementamos esto xd, pero aquí podrás encontrar parqueos libres cerca de tí",
            onBack = { screen = Screen.Home }
        )
        Screen.Report -> InfoScreen(
            title = "Reportar",
            text = "acá se pueden reportar cierres de vías, adjuntando una imagen y dando una justificación",
            onBack = { screen = Screen.Home }
        )
    }
}

// ---------------------------------------------------------------------------
// Pantalla de login (solo la primera vez)
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val ctx = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "VIAS",
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Inicia sesión para continuar",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Gmail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu Gmail se usa para vincular Google Maps con la app.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = Color(0xFFD32F2F),
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val user = username.trim()
                val mail = email.trim()
                error = when {
                    user.isEmpty() -> "Ingresa un nombre de usuario."
                    password.length < 4 -> "La contraseña debe tener al menos 4 caracteres."
                    !Patterns.EMAIL_ADDRESS.matcher(mail).matches() ||
                        !mail.lowercase().endsWith("@gmail.com") ->
                        "Ingresa un correo de Gmail válido (…@gmail.com)."
                    else -> null
                }
                if (error == null) {
                    prefs(ctx).edit()
                        .putBoolean("logged_in", true)
                        .putString("username", user)
                        .putString("email", mail)
                        .putString("password_hash", sha256(password))
                        .apply()
                    onLoggedIn()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Entrar", fontSize = 16.sp)
        }

        Spacer(Modifier.height(48.dp))
        Credits()
    }
}

// ---------------------------------------------------------------------------
// Pantalla de inicio
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    val iconTint = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "A donde hoy?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MenuButton(label = "Rutas", onClick = { onNavigate(Screen.Routes) }) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = "Rutas",
                    tint = iconTint,
                    modifier = Modifier.size(44.dp)
                )
            }
            MenuButton(label = "Parqueos", onClick = { onNavigate(Screen.Parking) }) {
                StopSignIcon()
            }
            MenuButton(label = "Reportar", onClick = { onNavigate(Screen.Report) }) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Reportar",
                    tint = iconTint,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Credits()
    }
}

@Composable
fun MenuButton(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Señal de "PARE" dibujada: octágono rojo con la palabra STOP. */
@Composable
fun StopSignIcon(modifier: Modifier = Modifier.size(52.dp)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val path = Path()
            for (i in 0 until 8) {
                val angle = Math.toRadians(22.5 + 45.0 * i)
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color(0xFFD32F2F))
            drawPath(path, Color.White, style = Stroke(width = 2.dp.toPx()))
        }
        Text(
            text = "STOP",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// ---------------------------------------------------------------------------
// Pantallas informativas (Parqueos y Reportar)
// ---------------------------------------------------------------------------

@Composable
fun InfoScreen(title: String, text: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(Modifier.width(4.dp))
            Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }
        Credits(modifier = Modifier.fillMaxWidth())
    }
}

// ---------------------------------------------------------------------------
// Pantalla de rutas: mapa de Google con tu ubicación
// ---------------------------------------------------------------------------

@SuppressLint("MissingPermission")
@Composable
fun RoutesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    var hasPermission by remember {
        mutableStateOf(
            granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermission = result.values.any { it } }

    val cameraState = rememberCameraPositionState {
        // Vista del mundo hasta tener el primer GPS fix
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }
    val fused = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var destination by remember { mutableStateOf<LatLng?>(null) }
    var recenterTick by remember { mutableIntStateOf(0) }

    fun locate() {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    myLocation = LatLng(loc.latitude, loc.longitude)
                    recenterTick++
                } else {
                    fused.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            myLocation = LatLng(last.latitude, last.longitude)
                            recenterTick++
                        }
                    }
                }
            }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission) locate()
    }
    LaunchedEffect(recenterTick) {
        val here = myLocation
        if (recenterTick > 0 && here != null) {
            cameraState.animate(CameraUpdateFactory.newLatLngZoom(here, 16f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false
            ),
            onMapClick = { destination = it },
            onMapLongClick = { destination = it }
        ) {
            destination?.let { d ->
                Marker(
                    state = remember(d) { MarkerState(position = d) },
                    title = "Destino"
                )
            }
        }

        // Botón volver
        Surface(
            shape = CircleShape,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }

        // Controles inferiores
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(onClick = { locate() }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
            }
            Spacer(Modifier.height(12.dp))

            val dest = destination
            when {
                !hasPermission -> InfoChip("Activa el permiso de ubicación para verte en el mapa.")
                dest == null -> InfoChip("Toca el mapa para elegir tu destino.")
                else -> Button(
                    onClick = { openNavigation(ctx, dest) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cómo llegar (mejor ruta)", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(14.dp)
        )
    }
}

/** Abre la navegación de Google Maps hacia el destino; si no está la app, usa el navegador. */
private fun openNavigation(ctx: Context, dest: LatLng) {
    val navIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}&mode=d")
    ).setPackage("com.google.android.apps.maps")
    try {
        ctx.startActivity(navIntent)
    } catch (e: Exception) {
        val web = Uri.parse(
            "https://www.google.com/maps/dir/?api=1" +
                "&destination=${dest.latitude},${dest.longitude}&travelmode=driving"
        )
        ctx.startActivity(Intent(Intent.ACTION_VIEW, web))
    }
}

// ---------------------------------------------------------------------------
// Créditos
// ---------------------------------------------------------------------------

@Composable
fun Credits(modifier: Modifier = Modifier) {
    Text(
        text = "VIAS hecho por P.E.P.I.N.A.ZO.S. - CTO: AJRP - 5D",
        fontSize = 11.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
