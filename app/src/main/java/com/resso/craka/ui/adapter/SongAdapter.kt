package com.resso.craka.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.resso.craka.R
import com.resso.craka.data.model.Song

class SongAdapter(
    private val onSongClick: (Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private val songs = mutableListOf<Song>()
    private var playingSongIndex: Int = -1
    private var isPlaying: Boolean = false

    fun submitList(newSongs: List<Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    fun setPlayingIndex(index: Int, playing: Boolean) {
        val prevIndex = playingSongIndex
        playingSongIndex = index
        isPlaying = playing
        if (prevIndex != -1 && prevIndex < songs.size) {
            notifyItemChanged(prevIndex)
        }
        if (playingSongIndex != -1 && playingSongIndex < songs.size) {
            notifyItemChanged(playingSongIndex)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, position == playingSongIndex, isPlaying)
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivSongThumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSongTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvSongArtist)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvSongDuration)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSongClick(pos)
                }
            }
        }

        fun bind(song: Song, isCurrent: Boolean, isCurrentlyPlaying: Boolean) {
            tvTitle.text = song.title
            tvArtist.text = song.artist.ifBlank { song.channel.ifBlank { "Unknown Artist" } }
            tvDuration.text = song.duration.ifBlank { "3:30" }

            // Highlight title with Spotify Green when actively playing
            if (isCurrent) {
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.spotify_green))
            } else {
                tvTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            // Load high-res thumbnail with Glide
            val thumbUrl = song.thumbnail.ifBlank {
                "https://i.ytimg.com/vi/${song.id}/hqdefault.jpg"
            }

            Glide.with(itemView.context)
                .load(thumbUrl)
                .transform(RoundedCorners(16))
                .placeholder(R.color.spotify_card)
                .error(R.drawable.ic_play_white)
                .into(ivThumbnail)
        }
    }
}
