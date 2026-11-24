# CineLibre

Aplicación Android para descubrir cine gratuito desde archivos audiovisuales abiertos.

## Primera versión
- Catálogo consultado desde Internet Archive.
- Búsqueda por título.
- Ficha de película y acceso a reproducción cuando hay un MP4 directo.
- Enlace a la ficha original para revisar detalles y opciones disponibles.
- Interfaz oscura en español.

## Compilar APK
Cada push a `main` ejecuta GitHub Actions. En la pestaña **Actions**, abre el flujo **Build CineLibre APK** y descarga el artefacto **CineLibre-debug** cuando termine correctamente.

También puedes ejecutarlo manualmente desde Actions con **Run workflow**.

## Nota sobre derechos
Internet Archive aloja materiales con distintos estados de derechos. CineLibre no debe asumir que todo su contenido es de dominio público. Antes de distribuir o promocionar una película, revisa la licencia y los derechos indicados en su ficha. El catálogo inicial es experimental y se irá ajustando para priorizar obras de dominio público o con permisos explícitos.

## Próximas mejoras
- Verificación más estricta de licencias y metadatos.
- Reproductor integrado y descargas administradas dentro de la app.
- Favoritos, historial y filtros por año/género.
- Fuentes legales adicionales para cine reciente y estrenos gratuitos.
