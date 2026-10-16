# Avance G-N — revisión de seguridad y alcance (v18)

## Cambios aplicados en el código fuente
- El historial valida la estructura básica de los registros recuperados de `localStorage`.
- La pantalla de historial se construye con nodos DOM y `textContent`, no interpolando contenido guardado dentro de `innerHTML`.
- Se deshabilitan el acceso del WebView al contenido de otros archivos, el acceso universal desde URLs `file:`, el contenido mixto, ventanas emergentes y navegación fuera de los recursos locales empaquetados.
- Se desactiva la copia de seguridad automática de datos de la app.
- La interfaz de importación acepta PDF, DOCX y TXT a través del selector de documentos de Android; no requiere permiso de almacenamiento general.
- El lector impone límite de 15 MB por archivo y limita el texto extraído a 1.5 millones de caracteres.
- El lector DOCX desactiva DTD y entidades externas para reducir riesgo de XXE.
- La exportación comparte el texto mediante `ACTION_SEND` sin exponer una URI `file://`.
- El parser de PDF se empaqueta como dependencia de la app; la lectura no requiere enviar documentos a un servidor.

## Limitaciones conocidas
- Los PDF escaneados que contienen únicamente imágenes necesitan OCR; esta versión extrae texto embebido.
- Las preguntas del documento se generan heurísticamente a partir de fragmentos y distractores de otros fragmentos del mismo documento. Es una ayuda de estudio, no una garantía de exactitud; debe revisarse cada reactivo y su respuesta contra la fuente.
- El compendio original de 100 reactivos se conserva.
- No se pudo ejecutar una compilación Android local en este entorno porque no hay Android SDK ni Gradle instalados. El flujo GitHub Actions está fijado a Gradle 8.9 para ejecutar la compilación en nube.
- No es una auditoría de seguridad certificada. Aún se requiere validar el artefacto compilado, probar documentos reales, comprobar el comportamiento en varios Android y realizar análisis estático de dependencias.
