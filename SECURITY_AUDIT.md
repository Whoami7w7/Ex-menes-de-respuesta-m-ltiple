# Avance G-N — revisión de seguridad inicial

## Hallazgo corregido por este parche
La pantalla de historial construía HTML mediante `innerHTML` usando datos recuperados de `localStorage`. Si esos datos se manipularan, podrían inyectarse etiquetas o atributos HTML en el WebView. El parche reemplaza ese renderizado por nodos DOM y `textContent`, y valida la estructura básica del historial antes de usarlo.

## Verificaciones pendientes
- Revisar `AndroidManifest.xml`: permisos mínimos, `android:exported`, componentes y configuración de backup.
- Revisar configuración de WebView: JavaScript bridges, acceso a archivos, contenido mixto, navegación externa y depuración.
- Revisar dependencias, versiones de Gradle/Android Gradle Plugin y acciones de GitHub fijadas a versiones seguras.
- Evaluar cifrado del historial y borrador; `localStorage` no es almacenamiento cifrado.
- Comprobar la firma del APK y separar builds de depuración y de lanzamiento.
- Probar datos locales malformados, almacenamiento lleno y archivos grandes.

## Límites
Este parche solo corrige el renderizado del historial en el HTML incluido en el proyecto. No equivale a una auditoría completa ni certifica el APK como seguro. No se ha inspeccionado aquí el código Android completo.

## Inteligencia e importación
El APK inspeccionado contiene un banco integrado y no se observó una función de importación en la interfaz examinada. La importación PDF/DOCX requiere incorporar parsers compatibles, preferentemente empaquetados localmente. No se debe enviar el contenido de documentos a terceros sin consentimiento claro. La generación automática debe permitir revisar las preguntas y mostrar la fuente del texto.
