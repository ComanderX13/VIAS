# Cómo obtener el APK de VIAS usando GitHub (sin instalar Android Studio)

GitHub compila el proyecto en sus servidores (gratis) y te deja el `.apk` para descargar. Tarda unos 5–10 minutos la primera vez.

## Paso 0: API key de Google Maps

1. Entra a https://console.cloud.google.com y crea un proyecto.
2. En "APIs y servicios" > "Biblioteca", activa **Maps SDK for Android**.
3. En "Credenciales" > "Crear credenciales" > "Clave de API". Copia la clave.
4. **Importante:** en las restricciones de la clave, restringe solo por **API** (Maps SDK for Android).
   NO la restrinjas por "Apps Android" (huella SHA-1): GitHub genera una llave de depuración nueva en cada
   compilación, así que el SHA-1 cambia y el mapa saldría en gris.

## Paso 1: Crear el repositorio

1. Crea una cuenta en https://github.com si no tienes.
2. Arriba a la derecha: **+ > New repository**. Nombre: `vias`. Elige **Private** (para que tu clave no sea pública). Crea el repositorio.

## Paso 2: Guardar la API key como secreto

1. En el repositorio: **Settings > Secrets and variables > Actions > New repository secret**.
2. Nombre: `MAPS_API_KEY`  (exactamente así).
3. Valor: tu clave de API. Guarda.

## Paso 3: Subir el proyecto

Descomprime `VIAS-proyecto-android.zip`. Dentro hay una carpeta `VIAS`.

### Opción A: con git (recomendada)
Abre una terminal dentro de la carpeta `VIAS` y ejecuta (cambia `TU_USUARIO`):

```
git init
git add .
git commit -m "VIAS prototipo"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/vias.git
git push -u origin main
```

### Opción B: sin git, desde la web
1. En tu repositorio vacío: **uploading an existing file**.
2. Arrastra el **contenido** de la carpeta `VIAS` (no la carpeta en sí): `app`, `gradle`, `gradlew`, `gradlew.bat`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, etc. Confirma con **Commit changes**.
3. La carpeta oculta `.github` normalmente NO se sube al arrastrar. Créala a mano:
   **Add file > Create new file**, en el nombre escribe `.github/workflows/build-apk.yml`
   (al escribir las `/` se crean las carpetas), pega el contenido del archivo `build-apk.yml` de esa carpeta y haz commit.

## Paso 4: Compilar y descargar el APK

1. Ve a la pestaña **Actions** del repositorio. Si pide habilitar los workflows, acepta.
2. Verás la ejecución "Compilar APK" (círculo amarillo = en curso, verde = listo).
   Si no arrancó sola: entra a "Compilar APK" > **Run workflow**.
3. Cuando esté en verde, abre esa ejecución y baja hasta **Artifacts**. Descarga **VIAS-apk**.
4. Es un `.zip`: descomprímelo y adentro está **app-debug.apk**.

## Paso 5: Instalar en el Honor (Magic OS 8.0)

1. Pasa `app-debug.apk` al teléfono (cable, Drive, WhatsApp, etc.).
2. Ábrelo desde el administrador de archivos y permite "instalar apps de esta fuente" cuando lo pida.
3. Si Magic OS muestra un aviso de seguridad, elige "Instalar de todas formas".

## Si algo falla

- **Círculo rojo en Actions:** abre la ejecución, entra al paso que falló y copia el error; con eso se corrige.
- **El mapa sale gris:** revisa que Maps SDK for Android esté activado, que el secreto se llame exactamente `MAPS_API_KEY` y que la clave no esté restringida por SHA-1.
- **El mapa no carga o no te ubica:** el teléfono necesita Google Play Services.
