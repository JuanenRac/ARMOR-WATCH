<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="Banner de ARMOR-WATCH" width="100%">
</p>

# ⌚ ARMOR-WATCH

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  🇪🇸 <b>Español</b> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### El reloj mínimo de A.R.M.O.R.: avisos de alarma y un toque para confirmar una alarma

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 o posterior">
  <img src="https://img.shields.io/badge/Wear OS-Kotlin-7f52ff.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Tests-124%20passing-2ea44f.svg" alt="Pruebas">
  <img src="https://img.shields.io/badge/Version-0.0.3-0f6cbd.svg" alt="Versión">
  <img src="https://img.shields.io/badge/Maturity-scaffolding-FFB020.svg" alt="Madurez">
</p>

---

**Comprobación de honestidad - qué funciona hoy:** El contrato de mensajes, los núcleos Kotlin y Swift y la sesión del reloj se prueban en un ordenador, y la app de Wear OS, la app del Apple Watch y el lado que envía de las apps Android e iPhone se compilan y pasan sus pruebas en CI; **nada ha corrido en un reloj real ni en un emulador (solo un gemelo simulado del teléfono, el enlace y el reloj), y la app del Apple Watch, compilada dentro del proyecto del iPhone, nunca se ha emparejado con un iPhone real.**

---

## 📋 De un vistazo

| | |
|---|---|
| Versión | `0.0.3` |
| Madurez | Andamiaje |
| Rol | El cliente de reloj mínimo de A.R.M.O.R. (familia: Clients, padre: ARMOR-ANDROID-CONTROL) |
| Stack | Kotlin (núcleo JVM y la app de Wear OS con Compose, Gradle), Swift (núcleo y la app del Apple Watch con SwiftUI); Python 3.11+ para las comprobaciones |
| Pruebas | 124 (72 en Python (`python -m unittest discover -s tests`) y 52 en Kotlin (`./gradlew test`)) |
| Mensajes | El interruptor del relé, una alerta, el borrado de una alerta y la confirmación de vuelta |
| Funciona en | Cualquier ordenador con JDK 21 (núcleo Kotlin) o un Mac (núcleo Swift); un reloj Wear OS 3+ o un Apple Watch emparejado con el móvil (apps compiladas en CI, nunca ejecutadas) |
| En hardware real | Ninguno todavía: solo lo ha ejecutado un gemelo simulado; la prueba en un reloj real está pendiente |
| Simulación y APK | El gemelo simulado (`tools/watch_simulation.py`) une en memoria el teléfono, el enlace con fallos y el reloj; la app Wear OS (`wear/`) y la app Apple Watch (`watchos/`) viven en este mismo repositorio, y el APK de publicación de Wear OS se firma fuera de GitHub como `ARMOR-WATCH-release.apk` |
| Licencia | GPL-3.0-or-later |

**Ir a:** [Resumen](#-resumen) · [Arquitectura](#-arquitectura) · [Seguridad](#-seguridad-y-protección) · [Configuración](#-configuración) · [Inicio rápido](#-inicio-rápido) · [Pruebas](#-pruebas) · [Problemas](#-resolución-de-problemas) · [Estructura](#-estructura-del-repositorio) · [Relacionados](#-proyectos-relacionados)

## 🚦 Estado del proyecto

Cada línea es una parte de este repositorio y la luz dice hasta dónde llega hoy. Está escrito con lo que el repositorio puede demostrar (pruebas, compilaciones, ejecuciones), no con lo que está planeado.

🟢 Hecho y verificado · 🟡 Funciona, con límites conocidos · 🟠 Parcial o sin verificar todavía · 🔴 No hecho o no funciona

| Área | Estado | Qué significa aquí |
|---|---|---|
| Software | 🟡 | Los núcleos Kotlin y Swift leen y comprueban cada mensaje y guardan las alertas en memoria; la app de Wear OS, la app del Apple Watch y los emisores de las apps Android e iPhone están escritos. |
| Pruebas | 🟡 | 72 pruebas de Python y las de Kotlin ejecutan los mismos ejemplos aceptados y rechazados del contrato; las de Kotlin corren en CI. |
| Hardware | 🟠 | Ningún reloj real ni emulador las ha ejecutado; un gemelo simulado (`tools/watch_simulation.py`) ejecuta toda la conversación en memoria (evidencia: simulador), y la prueba en un reloj real sigue pendiente. |
| Documentación | 🟡 | Ocho idiomas, las notas del protocolo y el contrato están escritos y al día. |

## 🎯 Resumen

* **Un reloj mínimo, no una segunda app.** ARMOR-ANDROID-CONTROL y ARMOR-IOS-CONTROL siguen siendo el cliente completo; el reloj solo muestra las alarmas y avisos que el móvil elige enviar y deja confirmar una alarma con un toque.
* **El móvil es el único que habla con ARMOR-SERVER.** El reloj no guarda sesión, contraseña ni clave, y no guarda nada en el reloj: lo que muestra está en memoria y se olvida al cerrar la app.
* **Apagado por defecto.** Nada llega al reloj hasta que la persona activa la opción de alertas del reloj en la app del móvil; mientras el móvil no lo diga, el reloj descarta toda alerta que reciba.
* **Cuatro mensajes pequeños** (`relay`, `alert`, `alert_clear`, `alert_ack`) con límites y ejemplos aceptados y rechazados en `docs/watch_alert_contract.json`; el móvil escribe los textos en el idioma de la persona, así que el reloj no tiene nada que traducir.
* **Sin sonido ni vibración propios:** el reloj no añade ninguno más allá de la notificación estándar del sistema.

## 🧩 Papel en A.R.M.O.R.

ARMOR-WATCH es el extremo de muñeca de las apps del móvil. [ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL) y [ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL) mantienen la sesión con [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER); el reloj recibe una alerta corta del móvil y devuelve el identificador de una alarma que la persona confirmó, que el móvil comprueba y ejecuta.

## 🏛️ Arquitectura

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-WATCH architecture: the person, the Kotlin core and its contract, the phone app and the server" width="100%">
</p>

## 🔒 Seguridad y protección

| Amenaza | Defensa |
|---|---|
| Un reloj que sigue mostrando alertas con la opción apagada | El móvil envía `relay` con `enabled: false` y el reloj olvida lo que mostraba; la bandeja arranca apagada |
| Un mensaje falsificado o mal formado | Cada mensaje se comprueba contra el contrato (tipo, versión, límites, tipos exactos) y el que falla se descarta sin error; no se lee nada de más de 4096 caracteres |
| Un reloj robado con una sesión dentro | No la hay: el reloj no guarda sesión, contraseña ni clave y no escribe nada en el almacenamiento |
| Una confirmación enviada dos veces o de una alarma que no está | La confirmación sale una vez por alerta, solo si la alerta la pide y no ha caducado; el móvil ignora un identificador que no conoce |
| La alarma de un PIN de coacción mostrada en la muñeca | El móvil no la envía nunca (igual que el aviso de las apps del móvil no la muestra) |

## ⚙️ Configuración

| Ajuste | Por defecto | Qué hace |
|---|---|---|
| `titleMax`, `textMax` | 60, 160 | Título y texto más largos de una alerta, en caracteres |
| `ttlSeconds` | 5 - 600 | Cuánto dura una alerta antes de desaparecer sola |
| `pendingMax` | 8 | Alertas guardadas a la vez; una crítica nunca se descarta por una menor |
| `rawMax` | 4096 | Un mensaje más largo no se lee |
| `relay.enabled` | false | Lo fija el móvil; el reloj no muestra nada hasta que es verdadero |
| `bindSender` | false | Solo en Wear OS; se fija desde la app. Activado, el reloj escucha solo al móvil que encendió las alertas, hasta que se apagan; apagado, cualquier móvil al alcance puede enviar |

## 🚀 Inicio rápido

**Requisitos:** Python 3.11+ para las comprobaciones; un JDK 21 para el núcleo Kotlin (el wrapper de Gradle descarga el propio Gradle).

```powershell
python tools/watch_contract.py                  # the examples of the contract in Python
python -m unittest discover -s tests            # the contract, the Kotlin constants and the protocol notes
./gradlew test                                  # the Kotlin core (WatchProtocol and AlertInbox)
```

Mira las [notas del protocolo](docs/PROTOCOL.md) para los mensajes y lo que hace cada lado.

## 🧪 Pruebas

```powershell
python -m unittest discover -s tests
./gradlew test
python -I tools/armor_ci_validate.py
```

| Fichero de pruebas | Pruebas | Qué cubre |
|---|---|---|
| `test_watch_contract.py` | 13 | Cada ejemplo del contrato, los límites, las constantes de Kotlin y las notas del protocolo |
| `test_watch_simulation.py` | 16 | La conversación simulada entre el teléfono y el reloj con fallos en el enlace, con los mismos escenarios que los de Kotlin |
| `WatchProtocolTest.kt` | 6 | Los mismos ejemplos contra el lector Kotlin, los límites y la confirmación |
| `AlertInboxTest.kt` | 10 | Apagado por defecto, orden, sustitución, caducidad, borrado, una sola confirmación, el límite de ocho |
| `WatchSessionTest.kt` | 11 | La conversación del reloj contra un teléfono que solo existe en memoria |
| `SimulatedPairTest.kt` | 14 | El teléfono, el enlace y el reloj simulados con fallos a propósito, sobre un reloj que mueve la prueba |
| `WatchRuntimeTest.kt` | 5 | La sesión compartida de la app Wear OS, la ruta de las alertas y el botón |
| `ArmorWatchCoreTests.swift` | 18 | El núcleo Swift de la app del Apple Watch contra los mismos ejemplos del contrato |

Las pruebas de Kotlin corren en CI; ninguna prueba ha corrido en un reloj ni en un emulador. El gemelo simulado (`tools/watch_simulation.py`) ejecuta toda la conversación en memoria, con fallos en el enlace.

## 🩺 Resolución de problemas

| Ves | Porque | Haz |
|---|---|---|
| No aparece nada en el reloj | La opción de alertas del reloj está apagada en la app del móvil, o el móvil no ha enviado `relay` con `enabled: true` | Activa la opción en la app del móvil |
| Una alerta desaparece sola | Se acabó su `ttlSeconds` o el móvil la borró | Vuelve a lanzarla desde el móvil |
| La confirmación no hace nada | La alerta no la pide, caducó o ya se confirmó una vez | Confirma la alarma en la app del móvil |

## 📂 Estructura del repositorio

<p align="center">
  <img src="images/repository-structure.svg" alt="Estructura del repositorio de ARMOR-WATCH" width="100%">
</p>

<details>
<summary>Versión en texto del árbol</summary>

```text
ARMOR-WATCH/
├── src/
│   ├── main/                               WatchProtocol (los mensajes y sus comprobaciones) y AlertInbox (las alertas en pantalla)
│   └── test/                               los mismos ejemplos que el contrato y las reglas de la bandeja
├── wear/
│   ├── src/                                la pantalla, el servicio del Data Layer y el runtime
│   └── build.gradle.kts                    la compilación de la app (solo con -ParmorWear)
├── watchos/
│   ├── Sources/                            el núcleo Swift: mensajes, bandeja, sesión
│   └── App/                                la pantalla y WatchConnectivity
├── tests/                                  el contrato comprobado en Python
├── docs/                                   el contrato y el protocolo
├── tools/                                  comprobaciones y contrato
├── gradle/                                 wrapper de Gradle
├── .github/                                flujo de CI y plantillas de issues y pull requests
├── images/                                 banner, icono y los dos dibujos del README
├── armor.project.json                      manifiesto: versión, rol, madurez, comando de build
├── CHANGELOG.md                            qué cambió en cada versión
├── SECURITY.md SUPPORT.md LICENSE          política de seguridad, soporte y licencia
├── CONTRIBUTING.md CODE_OF_CONDUCT.md      cómo contribuir y el código de conducta
├── build.gradle.kts settings.gradle.kts    el build de Kotlin
├── gradle.properties gradlew gradlew.bat   ajustes de Gradle y scripts del wrapper
└── .gitattributes .gitignore               ajustes de Git
```

</details>

## 🗺️ Hoja de ruta y límites conocidos

**Límites conocidos hoy**

* La app de Wear OS (`wear/`, se compila con `-ParmorWear`), la app del Apple Watch (`watchos/`, solo se compila en un Mac) y los emisores de ARMOR-ANDROID-CONTROL y ARMOR-IOS-CONTROL (apagados por defecto) se compilan y pasan sus pruebas en CI, pero nada ha corrido en un reloj real, un emulador ni un simulador.
* La app del Apple Watch se compila dentro del proyecto del iPhone (incrustada en ARMOR-IOS-CONTROL y compilada para el simulador en CI), pero el emparejamiento de las dos está por probar en dispositivos reales; un iPhone no se empareja con un reloj Wear OS ni un móvil Android con un Apple Watch.
* El contrato vive aquí; el núcleo Kotlin, el núcleo Swift y las apps del móvil ejecutan los mismos ejemplos.

**Siguiente**

* Probarlo en un reloj real de cada tipo: emparejarlo, activar la opción, lanzar una alarma de prueba, verla en el reloj, confirmarla con un toque y ver la confirmación en ARMOR-SERVER; después apagar la opción y ver desaparecer el aviso.

## 🌍 Idiomas

El README y los textos existen en inglés, español, francés, italiano, alemán, chino, japonés y portugués de Portugal.

## 🔗 Proyectos relacionados

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) es un sistema de seguridad perimetral hecho de repositorios independientes. Cada uno tiene su propia versión, sus propias pruebas y su propio README; esta es la familia:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Contratos de mensajes, validadores, vectores de conformidad y tipos generados
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Firmware de nodo de campo para ESP32-S3 con tres radares y su propio panel web
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protocolos de inversores y baterías solares y los mensajes de un nodo pasarela
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Nodo eléctrico: medidores, el mensaje de las lecturas de la red y las reglas de maniobra
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Nodo y panel de alarma: zonas, armado, retardos, sirena y PIN, con el servidor o sin él
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Panel táctil: el estado del sistema en una pantalla de pared, armar y reconocer, y el hogar del asistente de voz
* **[ARMOR-NODE-SOLAR-HMI](https://github.com/JuanenRac/ARMOR-NODE-SOLAR-HMI)** - Pantalla táctil solar: las baterías y los inversores tal como los conoce ARMOR-SERVER (fase inicial: un firmware compila, nunca ejecutado en una placa)
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - La red local: sus dispositivos, internet y lo que cambia
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Coordinador central: telemetría, alarmas, dispositivos, lecturas solares y cámaras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Consola web: cámaras, radar, alarmas, energía solar y el diseñador 2D/3D del emplazamiento
* **[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL)** - Cliente de operador Android con radar 2D/3D en vivo
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - Cliente de operador para iPhone
* **ARMOR-WATCH** (este repositorio) - Cliente de reloj mínimo: avisos de alarma y confirmación de alarma
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Política de inferencia visual que explica sus decisiones y nunca actúa
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Zonas de cámara, máscaras y reglas de detección para la Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Intenciones de voz sin conexión con una confirmación que no se puede falsificar
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Cajas, electrónica y la matriz de aceptación en banco
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Despliegue, el banco de pruebas de la CM5, copias de seguridad y TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Simulador de telemetría sin conexión con fallos repetibles
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Detecta, instala y actualiza los repositorios del propio ecosistema
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Arquitectura, base de seguridad y la matriz de capacidades

## 📚 Documentación

* [Notas del protocolo](docs/PROTOCOL.md): cómo hablan el móvil y el reloj
* [Contrato de mensajes](docs/watch_alert_contract.json): los mensajes, los límites y los ejemplos aceptados y rechazados
* [Matriz de capacidades: qué está demostrado y qué no](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Catálogo de proyectos: versiones y cómo dependen unos repositorios de otros](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Registro de cambios de este repositorio](CHANGELOG.md)

## 🤝 Contribuir y comunidad

* Antes de un pull request ejecuta `python -m unittest discover -s tests` y `python -I tools/armor_ci_validate.py`: debe pasar. Mira [CONTRIBUTING.md](CONTRIBUTING.md) y el [código de conducta](CODE_OF_CONDUCT.md).
* El README existe en ocho idiomas y se genera desde las fuentes de ARMOR-DOCS: un cambio de texto se hace allí.
* Un cambio en un mensaje entre componentes empieza en ARMOR-COMMON.
* No describas como funcionando en una placa lo que solo se ha compilado.
* Informa de una vulnerabilidad en privado: [SECURITY.md](SECURITY.md). Preguntas, ideas e informes: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENCIA

GPL-3.0-or-later - véase [LICENSE](LICENSE).
