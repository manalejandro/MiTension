# MiTensión

**MiTensión** es una aplicación Android para registrar la tensión arterial y la frecuencia cardíaca. Está diseñada para ser sencilla, intuitiva y accesible, con componentes grandes para personas con visión reducida.

> 🇬🇧 **English:** read the English documentation in [README.md](README.md).

## Características

- 📝 **Registro de tomas** — introduce la tensión máxima (sistólica) y mínima (diastólica) en mmHg (con decimales) y la frecuencia cardíaca. Dispone de un campo de nota opcional. Los valores se validan antes de guardar para que el usuario no pueda cometer errores.
- 📷 **Lectura con la cámara en vivo** — apunta a la pantalla del tensiómetro y la app reconoce MÁXIMA/MÍNIMA/PULSO de la vista previa, sin hacer ni guardar fotos. Una imagen de referencia de un tensiómetro Omron ayuda a encuadrar; los valores detectados se muestran para que el usuario los revise y confirme antes de rellenar el formulario. Guardar sigue requiriendo la confirmación habitual.
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
| **Nueva** | Formulario para registrar una toma, resumen de la última toma, usuario activo y botón de lectura con cámara |
| **Histórico** | Gráfica, estadísticas y lista editable/eliminable |
| **Avisos** | Activar, programar y elegir el sonido de los 3 recordatorios |
| **Usuarios** | Crear, cambiar, renombrar y eliminar usuarios |
| **Ajustes** | Idioma y tamaño del texto, ajustes de notificaciones e información |

## Lectura con cámara

1. Abre **Nueva** y pulsa **Leer valores con la cámara**.
2. Concede el permiso de cámara cuando se solicite. Verás una vista previa en vivo y la imagen de referencia del tensiómetro Omron.
3. Coloca la pantalla del tensiómetro dentro del recuadro, de frente y sin reflejos. Mantén el móvil quieto; no se hace ninguna foto.
4. Cuando los valores se mantengan estables, comprueba la **máxima, la mínima y el pulso** detectados.
5. Pulsa **Usar estos valores** solo si son correctos. Se rellenará el formulario y todavía tendrás que guardar la toma normalmente.

El escáner es opcional: la introducción manual siempre está disponible y la app nunca guarda ni sube los fotogramas de la cámara.

## Tecnologías

- **Kotlin** 2.2 · **Jetpack Compose** (Material 3)
- **Room** para la persistencia local (usuarios, tomas, recordatorios)
- **DataStore** (Preferences) para los ajustes
- **CameraX** + **ML Kit text recognition** para reconocer dígitos en vivo en el dispositivo (sin capturar fotos)
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
│   ├── screens/                 # onboarding, home, history, reminders, users, settings, scan
│   └── theme/                   # Paleta médica y tipografía
└── util/                        # Validación, idioma, tipos de recordatorio, parser de OCR
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
| `CAMERA` | Vista previa y análisis de fotogramas para leer la pantalla del tensiómetro. La cámara es opcional; siempre se puede introducir los valores a mano. |

## Datos y privacidad

MiTensión guarda las tomas, los usuarios y los ajustes localmente en el dispositivo (Room + DataStore). La app no sube las tomas ni los fotogramas de la cámara a ningún servidor. Los fotogramas se analizan en el propio dispositivo en tiempo real; no se captura, guarda ni envía ninguna foto ni vídeo. La imagen de Omron incluida sirve solo como guía visual de encuadre. Si la copia de seguridad de Android está activada, el sistema operativo podría copiar los datos de la app según la configuración de copia del usuario.

## Licencia

Proyecto personal de **Alejandro Manzanares**. Repositorio en GitHub: <https://github.com/manalejandro/MiTension>.

**Aviso:** MiTensión es una herramienta de registro y no ofrece consejo médico. Consulta siempre a un profesional sanitario sobre tu tensión arterial.