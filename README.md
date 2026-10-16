# Avance G-N — Aplicación Android v18

Aplicación de estudio para Sargento Segundo GN 2026. Conserva el banco original de 100 reactivos, modo examen/estudio, calificación, historial local y revisión de respuestas.

## Importación de compendios
- PDF con texto seleccionable.
- Word moderno `.docx`.
- Texto `.txt`.

La extracción se realiza localmente. Los PDF que son escaneos sin capa de texto requieren OCR y no se procesan en esta versión. Las preguntas creadas automáticamente deben revisarse contra el compendio; se guardan en el dispositivo y se mezclan con el banco original.

## Compilar APK
1. Sube el contenido de esta carpeta a la raíz de tu repositorio (no la carpeta contenedora).
2. En GitHub abre **Actions → Build Avance G-N APK → Run workflow**.
3. Espera a que el flujo termine en verde.
4. Descarga el artefacto `Avance-GN-debug-APK`; contiene `app-debug.apk`.

El código fuente no se pudo compilar localmente en este entorno porque no hay Android SDK ni Gradle instalados. La compilación en Actions debe considerarse pendiente hasta que termine correctamente y la APK resultante se pruebe en el dispositivo.
