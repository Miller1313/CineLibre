package com.cinelibre.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF101116)
private val Panel = Color(0xFF1B1D25)
private val Gold = Color(0xFFFFC857)

data class Film(val id: String, val title: String, val year: String, val description: String, val poster: String, val page: String, val video: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CineLibreApp { url -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    }
}

@Composable
fun CineLibreApp(openUrl: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var films by remember { mutableStateOf<List<Film>>(emptyList()) }
    var selected by remember { mutableStateOf<Film?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun search(text: String) {
        query = text
        scope.launch {
            loading = true; error = null
            try { films = withContext(Dispatchers.IO) { searchArchive(text) } }
            catch (e: Exception) { error = "No se pudo cargar el catálogo. Revisa tu conexión e inténtalo de nuevo." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { search("") }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CINE", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("LIBRE", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    Text("CATÁLOGO ABIERTO", color = Color.LightGray, fontSize = 10.sp)
                }
                Spacer(Modifier.height(18.dp))
                if (selected == null) {
                    Text("Historias que merecen\nvolver a verse.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar películas clásicas…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        trailingIcon = { TextButton(onClick = { search(query) }) { Text("Buscar") } },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("PELÍCULAS PARA DESCUBRIR", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Gold) }
                        error != null -> Text(error!!, color = Color.LightGray, modifier = Modifier.padding(16.dp))
                        films.isEmpty() -> Text("No encontramos resultados. Prueba otra búsqueda.", color = Color.LightGray)
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(films) { film ->
                                Row(
                                    Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp))
                                        .clickable { selected = film }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = film.poster, contentDescription = film.title, modifier = Modifier.size(width = 82.dp, height = 112.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(film.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(film.year, color = Gold, fontSize = 12.sp)
                                        Text(film.description.ifBlank { "Una película del archivo audiovisual abierto." }, color = Color.LightGray, fontSize = 11.sp, maxLines = 3)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { selected = null }) { Icon(Icons.Default.ArrowBack, null); Text(" Volver al catálogo") }
                    val film = selected!!
                    Text(film.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(film.year, color = Gold)
                    Spacer(Modifier.height(16.dp))
                    AsyncImage(model = film.poster, contentDescription = film.title, modifier = Modifier.fillMaxWidth().height(230.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(film.description.ifBlank { "Consulta la ficha original para conocer más detalles de esta película." }, color = Color.LightGray)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { openUrl(film.video ?: film.page) }, enabled = film.video != null, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink)) {
                        Icon(Icons.Default.PlayArrow, null); Text("  REPRODUCIR")
                    }
                    OutlinedButton(onClick = { openUrl(film.page) }) { Text("Ver ficha y opciones de descarga") }
                    if (film.video == null) Text("No encontramos un archivo de vídeo directo. Puedes revisar la ficha del archivo.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun searchArchive(term: String): List<Film> {
    val q = if (term.isBlank()) "mediatype:movies AND (collection:feature_films OR collection:classic_cartoons)" else "mediatype:movies AND title:(${term.replace(" ", " AND ")})"
    val api = "https://archive.org/advancedsearch.php?q=" + java.net.URLEncoder.encode(q, "UTF-8") +
        "&fl[]=identifier,title,year,description&rows=30&page=1&output=json&sort[]=downloads+desc"
    val connection = URL(api).openConnection() as HttpURLConnection
    connection.connectTimeout = 15000; connection.readTimeout = 15000
    try {
        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val docs = root.getJSONObject("response").getJSONArray("docs")
        return (0 until docs.length()).mapNotNull { i ->
            val d = docs.getJSONObject(i)
            val id = d.optString("identifier")
            if (id.isBlank()) null else {
                val title = d.optString("title", id).replace(Regex("<[^>]*>"), "")
                val year = d.optString("year", "Clásico")
                val desc = d.optString("description", "").let { if (it.length > 260) it.take(257) + "…" else it }
                Film(id, title, year, desc, "https://archive.org/services/img/$id", "https://archive.org/details/$id", findVideo(id))
            }
        }
    } finally { connection.disconnect() }
}

private fun findVideo(id: String): String? {
    return try {
        val url = URL("https://archive.org/metadata/$id")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        val files = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONArray("files")
        c.disconnect()
        (0 until files.length()).map { files.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".mp4", true) && it.optString("source") != "metadata" }
            ?.let { "https://archive.org/download/$id/" + Uri.encode(it.optString("name")) }
    } catch (_: Exception) { null }
}
