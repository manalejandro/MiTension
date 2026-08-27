# MiTensión

**MiTensión** es una aplicación Android para registrar la tensión arterial y la frecuencia cardíaca. Está diseñada para ser sencilla, intuitiva y accesible, con componentes grandes para personas con visión reducida.

> 🇬🇧 **English:** read the English documentation in [README.md](README.md).

## Características

- 📝 **Registro de tomas** — introduce la tensión máxima (sistólica) y mínima (diastólica) en mmHg (con decimales) y la frecuencia cardíaca. Dispone de un campo de nota opcional. Los valores se validan antes de guardar para que el usuario no pueda cometer errores.
- 💓 **Clasificación automática** — cada toma se clasifica según las categorías internacionales de presión arterial (normal, normal-alta, hipertensión grado 1/2, baja) con una insignia de color.
- 📈 **Histórico con gráfica** — todas las tomas del usuario activo se muestran en una gráfica de líneas (sistólica, diastólica y pulso) además de una lista con valores medios y la última toma.
- ✏️ **CRUD completo** — las tomas se pueden crear, editar y eliminar (con confirmación).
- ⏰ **Recordatorios** — avisos configurables por la mañana, al mediodía y por la tarde. Cada uno se puede activar/desactivar, cambiar su hora y personalizar su sonido de notificación.
- 👥 **Varios usuarios** — en el primer arranque se crea el usuario del teléfono. Puedes añadir más usuarios, cambiar el usuario activo, renombrarlos o eliminarlos. Cada usuario conserva su propio histórico.
- 🌐 **Bilingüe** — español por defecto, configurable a inglés en los Ajustes (sin reiniciar la app).
- 🔠 **Tamaño del texto** — ajustes de texto Normal, Grande y Muy grande para accesibilidad.
- 🔔 **Notificaciones** — solicita permiso para publicar notificaciones y permite alarmas exactas para que los avisos lleguen a su hora.
- 🎨 **Diseño cuidado** — tema médico (rojo/blanco) con icono de lanzador propio (corazón + línea de ECG).

## Pantallas

| Pantalla | Descripción |
| --- | --- |
| **Nueva** | Formulario para registrar una toma, resumen de la última toma y usuario activo |
| **Histórico** | Gráfica, estadísticas y lista editable/eliminable |
| **Avisos** | Activar, programar y elegir el sonido de los 3 recordatorios |
| **Usuarios** | Crear, cambiar, renombrar y eliminar usuarios |
| **Ajustes** | Idioma y tamaño del texto, ajustes de notificaciones e información |

## Tecnologías

- **Kotlin** 2.2 · **Jetpack Compose** (Material 3)
- **Room** para la persistencia local (usuarios, tomas, recordatorios)
- **DataStore** (Preferences) para los ajustes
- **AlarmManager** + **BroadcastReceivers** para los recordatorios diarios (se reprograman tras reiniciar el teléfono)
- Interfaz con barra de navegación inferior (sin librería de navegación)
- Inyección de dependencias manual a través de la clase `Application`

## Estructura del proyecto

```
app/src/main/java/com/manalejandro/mitension/
├── MiTensionApplication.kt      # Application + contenedor de dependencias
├── MainActivity.kt              # Composable raíz, tema e idioma
├── data/
│   ├── db/                      # Entidades Room, DAOs y base de datos
│   └── SettingsRepository.kt    # Ajustes con DataStore
├── notifications/               # Canales, programador y receptores
├── ui/
│   ├── MainViewModel.kt         # Estado compartido y operaciones
│   ├── MainScreen.kt            # Barra de navegación inferior
│   ├── components/              # Componentes accesibles reutilizables
│   ├── chart/                   # Gráfica de líneas con Canvas
│   ├── screens/                 # onboarding, home, history, reminders, users, settings
│   └── theme/                   # Paleta médica y tipografía
└── util/                        # Validación, idioma, tipos de recordatorio
```

## Compilación

Requisitos: JDK 17+ y el SDK de Android (compileSdk 37).

```bash
./gradlew :app:assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Ejecutar los tests unitarios:

```bash
./gradlew :app:testDebugUnitTest
```

## Permisos utilizados

| Permiso | Finalidad |
| --- | --- |
| `POST_NOTIFICATIONS` | Mostrar las notificaciones de los avisos (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Que los avisos lleguen a la hora exacta configurada (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | Restaurar los avisos después de reiniciar el teléfono |
| `VIBRATE` | Vibración en las notificaciones de los avisos |

## Datos y privacidad

Todos los datos se guardan localmente en el dispositivo (Room + DataStore). Ningún dato sale del dispositivo.

## Licencia

Proyecto personal de **Alejandro Manzanares**. Repositorio en GitHub: <https://github.com/manalejandro/MiTension>.

**Aviso:** MiTensión es una herramienta de registro y no ofrece consejo médico. Consulta siempre a un profesional sanitario sobre tu tensión arterial.