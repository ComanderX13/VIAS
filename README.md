# VIAS – prototipo Android

Kotlin + Jetpack Compose + Google Maps. Android 8.0+ (minSdk 26), pensado para Magic OS 8.0 (Android 14).

## 1. Conseguir una API key de Google Maps (obligatorio para que el mapa cargue)

1. Entra a https://console.cloud.google.com y crea un proyecto.
2. En "APIs y servicios" activa **Maps SDK for Android**.
3. En "Credenciales" crea una **clave de API**.
4. (Recomendado) Restringe la clave a apps Android: paquete `com.pepinazos.vias` + el SHA-1 de tu keystore de debug
   (`./gradlew signingReport` lo imprime).
5. Pega la clave en `gradle.properties`, en la línea `MAPS_API_KEY=...`.

Sin la clave, la app abre pero el mapa sale en gris.

## 2. Compilar el APK

### Opción A: Android Studio (la más fácil)
1. Instala Android Studio (https://developer.android.com/studio).
2. File > Open > selecciona esta carpeta `VIAS`. Espera a que termine el "Gradle sync".
3. Menú **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Cuando termine, pulsa "locate". El archivo es `app/build/outputs/apk/debug/app-debug.apk`.

### Opción B: línea de comandos
Necesitas JDK 17 y el Android SDK (variable `ANDROID_HOME`).

```
./gradlew assembleDebug
```

## 3. Instalar en el teléfono (Honor / Magic OS 8.0)

1. Pasa `app-debug.apk` al teléfono (cable, Drive, WhatsApp, etc.).
2. Ábrelo desde el administrador de archivos y permite "instalar apps de esta fuente" cuando lo pida.
3. Si Magic OS muestra un aviso de seguridad, elige "Instalar de todas formas".

## Qué hace

- **Login** (solo la primera vez): usuario, contraseña y Gmail. Se guardan solo en el teléfono (la contraseña como hash SHA-256).
  Es un login de prototipo: no hay servidor ni autenticación real con Google.
- **Inicio**: "A donde hoy?" y tres botones: Rutas (auto), Parqueos (señal de pare), Reportar (bandera).
- **Rutas**: mapa de Google con tu ubicación. Toca el mapa para elegir destino y pulsa "Cómo llegar" para abrir la navegación de Google Maps con la mejor ruta.
- **Parqueos** y **Reportar**: pantallas con el texto indicado.
- Texto pequeño de créditos al pie.

## Notas

- El mapa y la ubicación necesitan **Google Play Services** en el teléfono. Algunos Honor/Magic OS (por ejemplo, versión China) no lo traen.
- Para cerrar la sesión y volver a ver el login: Ajustes > Apps > VIAS > Almacenamiento > Borrar datos.
