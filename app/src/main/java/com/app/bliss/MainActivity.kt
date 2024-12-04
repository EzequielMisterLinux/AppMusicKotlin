package com.app.bliss

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.bliss.databinding.ActivityMainBinding
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var adapter: MusicPlayerAdapter
    private var reqCode = 10

    companion object {
        var audioList: ArrayList<SongData> = arrayListOf()
        var favList: ArrayList<SongData> = arrayListOf()
        var isClicked: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (requestRuntimePermission()) {
            initialized()

            favList = ArrayList()
            val liked = getSharedPreferences("LIKED", MODE_PRIVATE)
            val jsonString = liked.getString("LikedSongs", null)
            val typeToken = object : TypeToken<ArrayList<SongData>>() {}.type
            if (jsonString != null) {
                val data: ArrayList<SongData> = GsonBuilder().create().fromJson(jsonString, typeToken)
                favList.addAll(data)
            }

            try {
                binding.fav.setOnClickListener {
                    if (isClicked) {
                        isClicked = false
                        initialized()
                    } else {
                        isClicked = true
                        favList = checkPlayList(favList)
                        val rv = binding.songView
                        rv.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false)
                        val adapter2 = MusicPlayerAdapter(favList, this)
                        adapter2.notifyItemChanged(favList.size)
                        adapter2.notifyItemRangeChanged(PlayerActivity.fIndex, favList.size)
                        rv.adapter = adapter2
                        if (favList.isEmpty()) {
                            binding.msg.isVisible = true
                            binding.head.isVisible = false
                        }
                        if (favList.size > 1) {
                            binding.shuffle.visibility = View.VISIBLE
                            binding.removeAll.visibility = View.VISIBLE
                            binding.shuffle.setOnClickListener {
                                val intent2 = Intent(this, PlayerActivity::class.java)
                                intent2.putExtra("index", 0)
                                intent2.putExtra("class", "shuffle")
                                startActivity(intent2)
                            }
                            binding.removeAll.setOnClickListener {
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Warning")
                                builder.setMessage("All liked songs will be removed on next start!")
                                builder.setIcon(android.R.drawable.ic_dialog_alert)

                                builder.setPositiveButton("OK") { _, _ ->
                                    favList.clear()
                                }

                                builder.setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }

                                val dialog = builder.create()
                                dialog.show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return
            }
        }
    }

    private fun initialized() {
        val rv = binding.songView
        audioList = getAllAudio()
        if (audioList.isEmpty()) {
            Toast.makeText(this, "No se encontraron canciones en el dispositivo.", Toast.LENGTH_SHORT).show()
            binding.msg.isVisible = true
            binding.head.isVisible = false
        } else {
            binding.msg.isVisible = false
            binding.head.isVisible = true
        }
        audioList = checkPlayList(audioList)
        rv.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false)
        adapter = MusicPlayerAdapter(audioList, this)
        rv.adapter = adapter
        adapter.notifyDataSetChanged()
        adapter.notifyItemChanged(audioList.size)
        binding.shuffle.visibility = View.INVISIBLE
        binding.removeAll.visibility = View.INVISIBLE
        binding.msg.isVisible = false
        binding.head.isVisible = true
    }

    private fun getAllAudio(): ArrayList<SongData> {
        val tempList = ArrayList<SongData>()
        val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Filtra archivos que están en la carpeta Downloads
        val selection = "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%/Download/%")

        val cursor = contentResolver.query(audioUri, projection, selection, selectionArgs, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUri = Uri.withAppendedPath(uri, albumId.toString()).toString()

                    // Verifica si el archivo existe
                    val file = File(path)
                    if (file.exists()) {
                        tempList.add(SongData(title, artist, artUri, path, duration, ""))
                    } else {
                        Log.w("getAllAudio", "Archivo no encontrado: $path")
                    }
                } while (cursor.moveToNext())
            } else {
                Log.w("getAllAudio", "No se encontraron canciones en la carpeta Downloads.")
            }
            cursor.close()
        } else {
            Log.e("getAllAudio", "El cursor de MediaStore es nulo.")
        }

        // Log para verificar si la lista contiene canciones
        Log.d("getAllAudio", "Número de canciones encontradas: ${tempList.size}")
        return tempList
    }



    @SuppressLint("InlinedApi")
    private fun requestRuntimePermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ),
                    reqCode
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_MEDIA_AUDIO,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ),
                    reqCode
                )
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == reqCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                initialized()
            } else {
                requestRuntimePermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val liked = getSharedPreferences("LIKED", MODE_PRIVATE).edit()
        val jsonLiked = GsonBuilder().create().toJson(favList)
        liked.putString("LikedSongs", jsonLiked)
        liked.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!PlayerActivity.isPlaying || (PlayerActivity.isPlaying && PlayerActivity.musicService != null)) {
            PlayerActivity.musicService!!.stopSelf()
            PlayerActivity.musicService!!.mediaPlayer!!.release()
            PlayerActivity.musicService = null
            exitProcess(1)
        }
    }
}
