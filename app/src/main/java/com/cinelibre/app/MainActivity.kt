package com.cinelibre.app
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Ink=Color(0xFF090B10)
private val Panel=Color(0xFF171B24)
private val Accent=Color(0xFF52D6B5)
data class Film(val id:String,val title:String,val year:String,val description:String,val poster:String,val page:String,val video:String?)
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{CineLibreApp()}}}

@Composable fun CineLibreApp(){
 val context=LocalContext.current
 var query by remember{mutableStateOf("")}
 var films by remember{mutableStateOf<List<Film>>(emptyList())}
 var selected by remember{mutableStateOf<Film?>(null)}
 var loading by remember{mutableStateOf(false)}
 var error by remember{mutableStateOf<String?>(null)}
 var showingFavorites by remember{mutableStateOf(false)}
 var favorites by remember{mutableStateOf(context.getSharedPreferences("cine_libre",Context.MODE_PRIVATE).getStringSet("favorites",emptySet())?.toSet()?:emptySet())}
 var playing by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 fun search(text:String){query=text;loading=true;error=null;showingFavorites=false;scope.launch{try{films=withContext(Dispatchers.IO){searchArchive(text)}}catch(e:Exception){error="No se pudo cargar. Revisa tu conexión."};loading=false}}
 LaunchedEffect(Unit){search("")}
 MaterialTheme(colorScheme=darkColorScheme(primary=Accent,background=Ink,surface=Panel,onSurface=Color.White)){
  Surface(Modifier.fillMaxSize(),color=Ink){Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
   Spacer(Modifier.height(18.dp));Row(verticalAlignment=Alignment.CenterVertically){Text("CINE",fontSize=25.sp,fontWeight=FontWeight.Black,color=Color.White);Text("LIBRE",fontSize=25.sp,fontWeight=FontWeight.Black,color=Accent);Spacer(Modifier.weight(1f));Text("CINE ABIERTO",fontSize=10.sp,color=Color.LightGray)}
   Spacer(Modifier.height(12.dp))
   if(selected==null){
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=!showingFavorites,onClick={showingFavorites=false},label={Text("Descubrir")});FilterChip(selected=showingFavorites,onClick={showingFavorites=true},label={Text("Favoritos")})}
    if(!showingFavorites){Text("Historias para descubrir.",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Color.White);OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Buscar película…")},singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={IconButton(onClick={search(query)}{Text("Buscar")}});Spacer(Modifier.height(10.dp))}
    when{loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Accent)};error!=null->Text(error!!,color=Color.LightGray);else->LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)){
     val shown=if(showingFavorites)films.filter{favorites.contains(it.id)}else films
     items(shown){film->Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(14.dp)).clickable{selected=film}.padding(10.dp),verticalAlignment=Alignment.CenterVertically){AsyncImage(model=film.poster,contentDescription=film.title,modifier=Modifier.size(90.dp,120.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(film.title,color=Color.White,fontWeight=FontWeight.Bold);Text(film.year,color=Accent,fontSize=12.sp);Text(film.description,color=Color.LightGray,fontSize=12.sp,maxLines=3)};Icon(if(favorites.contains(film.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,contentDescription="Favorito",tint=Accent)}}
     if(showingFavorites&&shown.isEmpty())item{Text("Todavía no tienes favoritos.",color=Color.LightGray)}
    }}
   }else{
    val film=selected!!
    TextButton(onClick={playing=false;selected=null}){Icon(Icons.Default.ArrowBack,null);Text(" Volver")}
    LazyColumn(contentPadding=PaddingValues(bottom=24.dp)){item{
     AsyncImage(model=film.poster,contentDescription=film.title,modifier=Modifier.fillMaxWidth().height(250.dp));Spacer(Modifier.height(12.dp));Text(film.title,color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold);Text(film.year,color=Accent);Spacer(Modifier.height(12.dp));Text(film.description.ifBlank{"Sinopsis no disponible."},color=Color.LightGray);Spacer(Modifier.height(16.dp))
     Button(onClick={playing=!playing},enabled=film.video!=null,colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Ink)){Icon(Icons.Default.PlayArrow,null);Text(if(playing)" Ocultar reproductor" else " Reproducir aquí")}
     if(playing&&film.video!=null)InternalPlayer(film.video)
     Spacer(Modifier.height(8.dp));OutlinedButton(onClick={downloadFilm(context,film)},enabled=film.video!=null){Icon(Icons.Default.Download,null);Text(" Descargar")}
     OutlinedButton(onClick={favorites=if(favorites.contains(film.id))favorites-film.id else favorites+film.id;context.getSharedPreferences("cine_libre",Context.MODE_PRIVATE).edit().putStringSet("favorites",favorites.toSet()).apply()}){Icon(if(favorites.contains(film.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,null);Text(if(favorites.contains(film.id))" Quitar de favoritos" else " Guardar en favoritos")}
     if(film.video==null)Text("No se detectó archivo MP4.",color=Color.LightGray,fontSize=12.sp)
     Text("Verifica derechos y condiciones de cada obra antes de descargar o compartir.",color=Color.Gray,fontSize=11.sp)
    }}
   }
  }}
 }
}
@Composable private fun InternalPlayer(url:String){
 val ctx=LocalContext.current
 val player=remember(url){ExoPlayer.Builder(ctx).build().apply{setMediaItem(androidx.media3.common.MediaItem.fromUri(url));prepare();playWhenReady=true}}
 DisposableEffect(player){onDispose{player.release()}}
 AndroidView(factory={ctx->PlayerView(ctx).apply{player.also{this.player=it}}},modifier=Modifier.fillMaxWidth().height(230.dp),update={it.player=player})
}
private fun downloadFilm(context:Context,film:Film){val url=film.video?:return;try{val request=DownloadManager.Request(Uri.parse(url)).setTitle(film.title).setDescription("CineLibre").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalFilesDir(context,Environment.DIRECTORY_MOVIES,film.id+".mp4").setAllowedOverMetered(true).setAllowedOverRoaming(false);(context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)}catch(_:Exception){}}
private fun searchArchive(term:String):List<Film>{
 val q=if(term.isBlank())"collection:publicdomainmovies OR collection:feature_films" else "title:("+term.replace(" "," AND ")+") AND mediatype:movies"
 val api="https://archive.org/advancedsearch.php?q="+java.net.URLEncoder.encode(q,"UTF-8")+"&fl[]=identifier,title,year,description&rows=35&page=1&output=json&sort[]=downloads+desc"
 val c=URL(api).openConnection() as HttpURLConnection;c.connectTimeout=15000;c.readTimeout=15000
 try{val docs=JSONObject(c.inputStream.bufferedReader().use{it.readText()}).getJSONObject("response").getJSONArray("docs");return (0 until docs.length()).mapNotNull{i->val d=docs.getJSONObject(i);val id=d.optString("identifier");if(id.isBlank())null else{val title=d.optString("title",id).replace(Regex("<[^>]*>"),"");val desc=d.optString("description","").replace(Regex("<[^>]*>"),"").let{if(it.length>350)it.take(347)+"…" else it};Film(id,title,d.optString("year",""),desc,"https://archive.org/services/img/$id","https://archive.org/details/$id",findVideo(id))}}}finally{c.disconnect()}
}
private fun findVideo(id:String):String?=try{val c=URL("https://archive.org/metadata/$id").openConnection() as HttpURLConnection;c.connectTimeout=9000;c.readTimeout=9000;val files=JSONObject(c.inputStream.bufferedReader().use{it.readText()}).getJSONArray("files");c.disconnect();(0 until files.length()).map{files.getJSONObject(it)}.firstOrNull{it.optString("name").endsWith(".mp4",true)&&it.optString("source")!="metadata"}?.let{"https://archive.org/download/$id/"+Uri.encode(it.optString("name"))}}catch(_:Exception){null}
