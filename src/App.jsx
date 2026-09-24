import React, { useState, useEffect, useRef } from 'react'
import {
  Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Heart, MessageCircle,
  Sparkles, Share2, Zap, Volume2, Upload, Search, Compass, Music, Disc,
  Check, X, Scissors, Video, ChevronUp, ChevronDown, ListMusic, Plus,
  Radio, Clock, Download, Users, Sliders, Send, Flame, ThumbsUp, PartyPopper,
  MessageSquare, SlidersHorizontal, Gauge, Eye, EyeOff, Copy,
  Globe, TrendingUp, Loader2, Link as LinkIcon, CheckCircle, AlertCircle, RefreshCw,
  Lock, Smartphone
} from 'lucide-react'

// Official Resso Vortex Glyph
function RessoLogo({ className = "w-7 h-7" }) {
  return (
    <svg viewBox="0 0 100 100" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="ressoLogoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FF0055" />
          <stop offset="45%" stopColor="#FF2A6D" />
          <stop offset="80%" stopColor="#7928CA" />
          <stop offset="100%" stopColor="#00F2FE" />
        </linearGradient>
        <filter id="ressoGlow" x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="0" stdDeviation="2.5" floodColor="#FF2A6D" floodOpacity="0.7"/>
        </filter>
      </defs>
      <path
        d="M35,32 C43,23 63,21 73,31 C83,40 84,58 76,70 C69,79 56,85 43,82 C30,78 23,65 26,52 C27,44 32,39 37,40 C42,41 43,47 41,52 C40,59 45,66 52,67 C59,68 67,64 69,57 C72,50 70,41 64,36 C57,30 44,32 38,38 C34,42 29,39 35,32 Z"
        fill="url(#ressoLogoGrad)"
        filter="url(#ressoGlow)"
      />
      <circle cx="54" cy="54" r="4.5" fill="#FFFFFF" />
    </svg>
  )
}

const INITIAL_SONGS = [
  {
    id: "song_kesariya",
    title: "Kesariya (Brahmastra Edition)",
    artist: "Arijit Singh / Pritam",
    album: "Brahmastra Studio Full",
    duration: 283,
    audioUrl: "https://api.audius.co/v1/tracks/NvXwOB4/stream?app_name=resso_music",
    albumArt: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    alternateVibes: [
      { id: "vk_1", title: "Golden Saffron", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Neon" },
      { id: "vk_2", title: "Midnight Flow", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", filter: "Dreamy" }
    ],
    lyrics: [
      { time: 0, text: "♪ Kesariya Tera Ishq Hai Piya ♪" },
      { time: 10, text: "Rang jaaun jo main haath lagaaun" },
      { time: 22, text: "Din beete saara teri fikr mein" },
      { time: 35, text: "Rain saari teri khair manaayein" },
      { time: 55, text: "Kesariya tera ishq hai piya..." },
      { time: 80, text: "Rang jaaun jo main haath lagaaun" },
      { time: 120, text: "Patjhad ke mausam mein bhi phool khila de" },
      { time: 160, text: "Resso 3D Spatial Audio • Feel the full acoustic depth" },
      { time: 200, text: "♪ Complete Track Playing Without Interruption ♪" },
      { time: 250, text: "Kesariya tera ishq hai piya..." }
    ],
    genre: "Bollywood Romantic",
    mood: "Chill",
    isLiked: true,
    isDownloaded: true,
    isFullSong: true
  },
  {
    id: "song_diljit",
    title: "Born To Shine (G.O.A.T. Era)",
    artist: "Diljit Dosanjh",
    album: "G.O.A.T. Deluxe",
    duration: 213,
    audioUrl: "https://api.audius.co/v1/tracks/vl7KK/stream?app_name=resso_music",
    albumArt: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    alternateVibes: [
      { id: "vd_1", title: "Punjabi Glow", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", filter: "Cyberpunk" },
      { id: "vd_2", title: "Retro Shinjuku", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Retro VHS" }
    ],
    lyrics: [
      { time: 0, text: "♪ Born To Shine - Diljit Dosanjh ♪" },
      { time: 12, text: "Kade check kari sadi history ni" },
      { time: 25, text: "Mitran di zindgi ch koi mystery ni" },
      { time: 42, text: "Bas rab di mehar naal kaim chill ae 🔥" },
      { time: 65, text: "Born to shine, asmaan tak naam chamke!" },
      { time: 100, text: "Full length bass drops with Resso Spatial Boost ⚡" },
      { time: 145, text: "Desi swagger, international sound" },
      { time: 185, text: "♪ Continuous full song playback ♪" }
    ],
    genre: "Punjabi Pop",
    mood: "Party",
    isLiked: true,
    isDownloaded: false,
    isFullSong: true
  },
  {
    id: "song_sidhu",
    title: "295 (Rebel Anthem)",
    artist: "Sidhu Moose Wala",
    album: "Moosetape Deluxe",
    duration: 273,
    audioUrl: "https://api.audius.co/v1/tracks/jZdk4/stream?app_name=resso_music",
    albumArt: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    alternateVibes: [
      { id: "vs_1", title: "Rebel Smoke", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Retro VHS" }
    ],
    lyrics: [
      { time: 0, text: "♪ 295 - Sidhu Moose Wala (The Legend) ♪" },
      { time: 15, text: "Dass kihda kihda karaan dhanwaad main" },
      { time: 32, text: "Sach bolunga taan milu 295 je suno..." },
      { time: 60, text: "Geetan vich bolda sach da toofaan ⚡" },
      { time: 95, text: "Moosetape original high fidelity audio" },
      { time: 140, text: "Never backing down, standing tall" },
      { time: 190, text: "Legendary voice echoes across the world 🔥" },
      { time: 240, text: "♪ Outro Beat & Legacy Melodies ♪" }
    ],
    genre: "Punjabi Hip-Hop",
    mood: "Party",
    isLiked: false,
    isDownloaded: true,
    isFullSong: true
  },
  {
    id: "song_1",
    title: "Starfall Echoes",
    artist: "Luna Eclipse",
    album: "Cosmic Dreams (Deluxe)",
    duration: 210,
    audioUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
    albumArt: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    alternateVibes: [
      { id: "v1_1", title: "Cyber Neon", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Neon" },
      { id: "v1_2", title: "Starlit Journey", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", filter: "Cyberpunk" },
      { id: "v1_3", title: "Chill Drive", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", filter: "Dreamy" }
    ],
    lyrics: [
      { time: 0, text: "(Ambient instrumental intro with cosmic reverb)" },
      { time: 8.5, text: "Floating high above the city neon lights" },
      { time: 15.2, text: "Walking through the static of these velvet nights" },
      { time: 23.0, text: "You said the universe was made for two" },
      { time: 30.4, text: "Every starfall echoes back to you" },
      { time: 38.0, text: "(Feel the synthesizer bass drop 🔥)" },
      { time: 44.2, text: "Catch the wavelength, riding on the frequency" },
      { time: 52.0, text: "Drifting closer, gravity surrounding me" },
      { time: 59.8, text: "Hold my hand before the skyline disappears" },
      { time: 67.5, text: "We've been dreaming here for thousand years" },
      { time: 75.0, text: "Every beat is a pulse in the midnight air" },
      { time: 82.5, text: "Look around, no one else is there" },
      { time: 90.0, text: "Just you and the cosmic sound" },
      { time: 98.0, text: "Spinning till the morning comes around" }
    ],
    genre: "Synthwave",
    mood: "Chill",
    isLiked: true,
    isDownloaded: true,
    isFullSong: true
  },
  {
    id: "song_2",
    title: "Neon Velocity",
    artist: "CyberVibe ft. K-Pulse",
    album: "Tokyo Overdrive 2099",
    duration: 185,
    audioUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
    albumArt: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    alternateVibes: [
      { id: "v2_1", title: "Nitro Rush", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", filter: "Cyberpunk" },
      { id: "v2_2", title: "Retro Shinjuku", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Retro VHS" }
    ],
    lyrics: [
      { time: 0, text: "(High tempo cyber pulse starts pumping)" },
      { time: 6.0, text: "120 on the dash, racing down Shinjuku" },
      { time: 12.5, text: "Neon reflections in the rearview mirror" },
      { time: 18.0, text: "Can't slow down now, the beat is getting clearer" },
      { time: 24.0, text: "Speed of sound, electricity in the vein" },
      { time: 30.0, text: "Dancing in the cybernetic summer rain" },
      { time: 37.0, text: "Jump into the pulse! ⚡" },
      { time: 43.0, text: "Turn the volume up, let the speakers shake" },
      { time: 50.0, text: "This is the rhythm we were born to make" },
      { time: 57.0, text: "Tokyo skyline glowing in pink and blue" },
      { time: 64.0, text: "I'm staying up all night with you" }
    ],
    genre: "EDM",
    mood: "Party",
    isLiked: false,
    isDownloaded: false,
    isFullSong: true
  },
  {
    id: "song_3",
    title: "Rainy Café Melancholy",
    artist: "Coffee & Tape",
    album: "Study Chill Beats Vol. 4",
    duration: 160,
    audioUrl: "https://commondatastorage.googleapis.com/codeskulptor-assets/sounddogs/soundtrack.mp3",
    albumArt: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    alternateVibes: [
      { id: "v3_1", title: "Rainy Window", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", filter: "Dreamy" }
    ],
    lyrics: [
      { time: 0, text: "(Rain sounds and soft lo-fi vinyl crackle ☕)" },
      { time: 7.0, text: "Steam rises from the porcelain cup" },
      { time: 14.0, text: "Window pane watching drops trickle up" },
      { time: 22.0, text: "Turn the page of an old paper book" },
      { time: 30.0, text: "Finding memories in every quiet nook" },
      { time: 39.0, text: "Soft Rhodes piano playing in the afternoon" },
      { time: 48.0, text: "Wishing the storm won't finish too soon" },
      { time: 58.0, text: "Warm sweater, thoughts drifting away" },
      { time: 68.0, text: "Just another cozy rainy day" }
    ],
    genre: "Lo-Fi",
    mood: "Focus",
    isLiked: true,
    isDownloaded: false
  },
  {
    id: "song_4",
    title: "Midnight Drive",
    artist: "The Retro Horizon",
    album: "Sunset Strip 1984",
    duration: 195,
    audioUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
    albumArt: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
    videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    alternateVibes: [
      { id: "v4_1", title: "Highway Drift", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Retro VHS" }
    ],
    lyrics: [
      { time: 0, text: "(Analog synth arpeggio builds the mood)" },
      { time: 9.0, text: "Top down, coastal highway in the dark" },
      { time: 17.0, text: "Ignite the engine with a single spark" },
      { time: 25.0, text: "Radio playing an 80s love cassette" },
      { time: 33.0, text: "A summer night we never will forget" },
      { time: 42.0, text: "Chasing the horizon under neon skies" },
      { time: 51.0, text: "Seeing all the stars inside your eyes" }
    ],
    genre: "Synthwave",
    mood: "Late Night",
    isLiked: false,
    isDownloaded: true
  }
]

const FULL_FEATURED_SONGS = [
  {
    id: "full_kesariya",
    trackName: "Kesariya (Arijit Singh Vibe)",
    artistName: "Arijit Singh / Pritam",
    albumName: "Brahmastra Studio Edition",
    duration: 268,
    audioStreamUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
    artworkUrl: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&auto=format&fit=crop&q=80",
    genre: "Bollywood Romantic",
    isFullSong: true,
    sourceName: "100% Full Song (4:28)"
  },
  {
    id: "full_diljit",
    trackName: "Lover / Peaches Punjabi Flow",
    artistName: "Diljit Dosanjh",
    albumName: "MoonChild Era Full",
    duration: 215,
    audioStreamUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
    artworkUrl: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
    genre: "Punjabi Pop",
    isFullSong: true,
    sourceName: "100% Full Song (3:35)"
  },
  {
    id: "full_sidhu",
    trackName: "So High / 295 Rebel Rhythm",
    artistName: "Sidhu Moose Wala",
    albumName: "Moosetape Deluxe",
    duration: 242,
    audioStreamUrl: "https://commondatastorage.googleapis.com/codeskulptor-assets/sounddogs/soundtrack.mp3",
    artworkUrl: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80",
    genre: "Punjabi Hip-Hop",
    isFullSong: true,
    sourceName: "100% Full Song (4:02)"
  },
  {
    id: "full_starfall",
    trackName: "Starfall Echoes (Cosmic Synth)",
    artistName: "Luna Eclipse",
    albumName: "Cosmic Dreams Full",
    duration: 210,
    audioStreamUrl: "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
    artworkUrl: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
    genre: "Synthwave",
    isFullSong: true,
    sourceName: "100% Full Song (3:30)"
  },
  {
    id: "full_lofi",
    trackName: "Rainy Café Melancholy (Lo-Fi)",
    artistName: "Coffee & Tape",
    albumName: "Study Chill Beats Vol. 4",
    duration: 160,
    audioStreamUrl: "https://commondatastorage.googleapis.com/codeskulptor-assets/sounddogs/soundtrack.mp3",
    artworkUrl: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
    genre: "Lo-Fi Beats",
    isFullSong: true,
    sourceName: "100% Full Song (2:40)"
  }
]

export default function App() {
  const [songs, setSongs] = useState(() => {
    const saved = localStorage.getItem('resso_songs_v5')
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        if (Array.isArray(parsed) && parsed.length > 0) return parsed
      } catch (e) {
        console.warn("Storage parse error", e)
      }
    }
    return INITIAL_SONGS
  })
  const [currentSongIndex, setCurrentSongIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(INITIAL_SONGS[0].duration || 283)
  const [isShuffle, setIsShuffle] = useState(false)
  const [repeatMode, setRepeatMode] = useState(0) // 0: off, 1: all, 2: one
  const [currentTab, setCurrentTab] = useState('foryou') // 'foryou', 'explore', 'library', 'vibe_creator'

  // Modals & Panels
  const [isCommentsOpen, setIsCommentsOpen] = useState(false)
  const [isPosterOpen, setIsPosterOpen] = useState(false)
  const [isVibeSwitcherOpen, setIsVibeSwitcherOpen] = useState(false)
  const [isPartyModeOpen, setIsPartyModeOpen] = useState(false)
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false)
  const [isSoundFxOpen, setIsSoundFxOpen] = useState(false)
  const [selectedPosterLyric, setSelectedPosterLyric] = useState(null)
  const [posterTheme, setPosterTheme] = useState('Neon Cyber')

  // Signature Resso Settings
  const [isFlashSync, setIsFlashSync] = useState(false)
  const [isVolumeBooster, setIsVolumeBooster] = useState(false)
  const [bassBoost, setBassBoost] = useState(true)
  const [spatialAudio, setSpatialAudio] = useState(true)
  const [playbackSpeed, setPlaybackSpeed] = useState(1.0)
  const [showBulletComments, setShowBulletComments] = useState(true)
  const [sleepTimerRemaining, setSleepTimerRemaining] = useState(null)
  const [partyReactions, setPartyReactions] = useState([])

  // Touch / Swipe Navigation (Resso signature vertical swipe)
  const touchStartY = useRef(0)
  const touchDeltaY = useRef(0)
  const [swipeOffset, setSwipeOffset] = useState(0)
  const [swipeIndicator, setSwipeIndicator] = useState(null) // 'next' or 'prev'

  // Search & Mood
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedMood, setSelectedMood] = useState(null)
  const [heartsList, setHeartsList] = useState([])
  const [screenFlash, setScreenFlash] = useState(false)

  // Internet Search & Stream States
  const [searchSource, setSearchSource] = useState('online') // 'online' | 'library' | 'direct_url'
  const [onlineQuery, setOnlineQuery] = useState('')
  const [onlineResults, setOnlineResults] = useState([])
  const [isSearchingOnline, setIsSearchingOnline] = useState(false)
  const [onlineSearchError, setOnlineSearchError] = useState(null)
  const [onlyFullSongs, setOnlyFullSongs] = useState(true)
  const [toastMessage, setToastMessage] = useState(null)

  // Direct Audio URL streaming
  const [directUrlInput, setDirectUrlInput] = useState('')
  const [directTitleInput, setDirectTitleInput] = useState('')
  const [directArtistInput, setDirectArtistInput] = useState('')

  // PWA Mobile App Installation
  const [installPrompt, setInstallPrompt] = useState(null)
  const [isInstalled, setIsInstalled] = useState(false)
  const [isInstallModalOpen, setIsInstallModalOpen] = useState(false)

  // Comments state persisted
  const [comments, setComments] = useState(() => {
    const saved = localStorage.getItem('resso_comments')
    return saved ? JSON.parse(saved) : [
      { id: 1, songId: "song_1", user: "Aarav Sharma", text: "This beat transition at 00:38 is literally goosebumps! 🔥", time: 38, likes: 142, liked: true },
      { id: 2, songId: "song_1", user: "Sneha Patel", text: "Listening to this while late night driving hits completely different 🌌", time: 60, likes: 89, liked: false },
      { id: 3, songId: "song_2", user: "Priya Roy", text: "Straight to my Tokyo Night drive playlist! 🏎️💨", time: 24, likes: 56, liked: false },
      { id: 4, songId: "song_1", user: "Kabir", text: "Best synth drop on Resso hands down! 🎧", time: 44, likes: 34, liked: true }
    ]
  })
  const [newComment, setNewComment] = useState('')

  // Vibes state persisted
  const [customVibes, setCustomVibes] = useState(() => {
    const saved = localStorage.getItem('resso_vibes')
    return saved ? JSON.parse(saved) : []
  })

  const audioRef = useRef(null)
  const videoRef = useRef(null)
  const lyricsContainerRef = useRef(null)
  const audioInputRef = useRef(null)

  const currentSong = songs[currentSongIndex] || songs[0]

  // Persist
  useEffect(() => {
    localStorage.setItem('resso_songs_v5', JSON.stringify(songs))
  }, [songs])

  // Sync duration and reset time whenever active song changes
  useEffect(() => {
    if (currentSong) {
      setCurrentTime(0)
      if (currentSong.duration) {
        setDuration(currentSong.duration)
      }
    }
  }, [currentSongIndex, currentSong?.id])

  useEffect(() => {
    localStorage.setItem('resso_comments', JSON.stringify(comments))
  }, [comments])

  useEffect(() => {
    localStorage.setItem('resso_vibes', JSON.stringify(customVibes))
  }, [customVibes])

  // Sleep timer ticker
  useEffect(() => {
    if (!sleepTimerRemaining) return
    const interval = setInterval(() => {
      setSleepTimerRemaining(prev => {
        if (prev <= 1) {
          setIsPlaying(false)
          return null
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [sleepTimerRemaining])

  // Audio Playback, Source & Speed sync
  useEffect(() => {
    if (audioRef.current && currentSong?.audioUrl) {
      audioRef.current.playbackRate = playbackSpeed

      // Keep src in sync without recreating audio element
      const currentSrc = audioRef.current.currentSrc || audioRef.current.src
      if (!currentSrc || (!currentSrc.endsWith(currentSong.audioUrl) && currentSrc !== currentSong.audioUrl)) {
        audioRef.current.src = currentSong.audioUrl
        audioRef.current.load()
      }

      if (isPlaying) {
        const playPromise = audioRef.current.play()
        if (playPromise !== undefined) {
          playPromise.catch((err) => {
            console.warn("Audio playback waiting/prevented:", err)
          })
        }
        if (videoRef.current && document.visibilityState === 'visible') {
          videoRef.current.play().catch(() => {})
        }
      } else {
        audioRef.current.pause()
        if (videoRef.current) {
          videoRef.current.pause()
        }
      }
    }
  }, [isPlaying, currentSongIndex, currentSong?.audioUrl, playbackSpeed])

  // Background Audio Guard: Pause video when tab/screen is hidden so mobile browser does not pause audio!
  useEffect(() => {
    const handleBackgroundPlayback = () => {
      if (document.visibilityState === 'hidden') {
        // Mobile Chrome will pause entire media pipeline if a video is running while hidden.
        // Pausing the video frees the audio element to play continuously in background & lockscreen!
        if (videoRef.current) {
          videoRef.current.pause()
        }
        if (isPlaying && audioRef.current && audioRef.current.paused) {
          audioRef.current.play().catch(() => {})
        }
      } else if (document.visibilityState === 'visible' && isPlaying) {
        if (videoRef.current) {
          videoRef.current.play().catch(() => {})
        }
      }
    }
    document.addEventListener('visibilitychange', handleBackgroundPlayback)
    return () => document.removeEventListener('visibilitychange', handleBackgroundPlayback)
  }, [isPlaying])

  // Flash sync effect on beat
  useEffect(() => {
    let interval = null
    if (isFlashSync && isPlaying) {
      interval = setInterval(() => {
        setScreenFlash(true)
        setTimeout(() => setScreenFlash(false), 90)
      }, 480)
    } else {
      setScreenFlash(false)
    }
    return () => clearInterval(interval)
  }, [isFlashSync, isPlaying])

  const formatTime = (secs) => {
    if (isNaN(secs) || secs == null) return "00:00"
    const m = Math.floor(secs / 60)
    const s = Math.floor(secs % 60)
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  // Active lyric calculation
  const activeLyricIndex = currentSong.lyrics ? currentSong.lyrics.reduce((lastIdx, line, idx) => {
    return line.time <= currentTime ? idx : lastIdx
  }, 0) : 0

  // Auto-scroll lyrics smoothly
  useEffect(() => {
    if (lyricsContainerRef.current) {
      const activeEl = lyricsContainerRef.current.children[activeLyricIndex]
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  }, [activeLyricIndex])

  const togglePlay = () => setIsPlaying(!isPlaying)

  const playNext = () => {
    if (isShuffle) {
      const nextIdx = Math.floor(Math.random() * songs.length)
      setCurrentSongIndex(nextIdx)
    } else {
      setCurrentSongIndex((prev) => (prev + 1) % songs.length)
    }
    setIsPlaying(true)
  }

  const playPrev = () => {
    if (currentTime > 3) {
      if (audioRef.current) audioRef.current.currentTime = 0
    } else {
      setCurrentSongIndex((prev) => (prev === 0 ? songs.length - 1 : prev - 1))
    }
    setIsPlaying(true)
  }

  // Stable refs for background/lock screen action handlers
  const playNextRef = useRef(playNext)
  const playPrevRef = useRef(playPrev)
  const togglePlayRef = useRef(togglePlay)

  useEffect(() => {
    playNextRef.current = playNext
    playPrevRef.current = playPrev
    togglePlayRef.current = togglePlay
  })

  // 1. MediaSession API: Lock Screen Controls & Background Audio
  useEffect(() => {
    if (!('mediaSession' in navigator) || !currentSong) return

    try {
      const art = currentSong.albumArt || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
      navigator.mediaSession.metadata = new window.MediaMetadata({
        title: currentSong.title || "Resso Song",
        artist: currentSong.artist || "Resso Artist",
        album: currentSong.album || "Resso Vibe HD",
        artwork: [
          { src: art, sizes: '96x96', type: 'image/jpeg' },
          { src: art, sizes: '128x128', type: 'image/jpeg' },
          { src: art, sizes: '192x192', type: 'image/jpeg' },
          { src: art, sizes: '256x256', type: 'image/jpeg' },
          { src: art, sizes: '384x384', type: 'image/jpeg' },
          { src: art, sizes: '512x512', type: 'image/jpeg' }
        ]
      })

      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused'
    } catch (e) {
      console.warn("MediaSession error:", e)
    }
  }, [currentSong?.id, currentSong?.title, currentSong?.artist, currentSong?.album, currentSong?.albumArt, isPlaying])

  // 2. Lock Screen action handlers (Play, Pause, Next Track, Prev Track, Seek)
  useEffect(() => {
    if (!('mediaSession' in navigator)) return

    const actions = [
      ['play', () => {
        setIsPlaying(true)
        if (audioRef.current) audioRef.current.play().catch(e => console.warn(e))
      }],
      ['pause', () => {
        setIsPlaying(false)
        if (audioRef.current) audioRef.current.pause()
      }],
      ['previoustrack', () => {
        if (playPrevRef.current) playPrevRef.current()
      }],
      ['nexttrack', () => {
        if (playNextRef.current) playNextRef.current()
      }],
      ['seekto', (details) => {
        if (details.seekTime !== undefined && audioRef.current) {
          audioRef.current.currentTime = details.seekTime
          setCurrentTime(details.seekTime)
        }
      }],
      ['seekbackward', (details) => {
        const offset = details.seekOffset || 10
        if (audioRef.current) {
          audioRef.current.currentTime = Math.max(0, audioRef.current.currentTime - offset)
          setCurrentTime(audioRef.current.currentTime)
        }
      }],
      ['seekforward', (details) => {
        const offset = details.seekOffset || 10
        if (audioRef.current) {
          const maxDur = audioRef.current.duration || duration || 300
          audioRef.current.currentTime = Math.min(maxDur, audioRef.current.currentTime + offset)
          setCurrentTime(audioRef.current.currentTime)
        }
      }],
      ['stop', () => {
        setIsPlaying(false)
        if (audioRef.current) {
          audioRef.current.pause()
          audioRef.current.currentTime = 0
        }
      }]
    ]

    for (const [act, handler] of actions) {
      try {
        navigator.mediaSession.setActionHandler(act, handler)
      } catch (e) {}
    }

    return () => {
      for (const [act] of actions) {
        try {
          navigator.mediaSession.setActionHandler(act, null)
        } catch (e) {}
      }
    }
  }, [duration])

  // 3. Screen WakeLock API: keeps screen active & prevents mobile CPU sleeping during playback
  useEffect(() => {
    let wakeLock = null
    const requestWakeLock = async () => {
      if ('wakeLock' in navigator && isPlaying && document.visibilityState === 'visible') {
        try {
          wakeLock = await navigator.wakeLock.request('screen')
        } catch (err) {}
      }
    }
    requestWakeLock()

    const handleVisibility = () => {
      if (document.visibilityState === 'visible' && isPlaying) {
        requestWakeLock()
      }
    }
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      document.removeEventListener('visibilitychange', handleVisibility)
      if (wakeLock) {
        wakeLock.release().catch(() => {})
      }
    }
  }, [isPlaying])

  // 4. Update document title for background tabs
  useEffect(() => {
    if (currentSong) {
      document.title = isPlaying 
        ? `▶ ${currentSong.title} - ${currentSong.artist} | Resso` 
        : `${currentSong.title} - ${currentSong.artist} | Resso`
    }
  }, [currentSong?.title, currentSong?.artist, isPlaying])

  // PWA Mobile App Installation Listener
  useEffect(() => {
    if (window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true) {
      setIsInstalled(true)
    }

    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault()
      setInstallPrompt(e)
    }

    const handleAppInstalled = () => {
      setIsInstalled(true)
      setInstallPrompt(null)
      showToast("🎉 Resso App successfully phone me install ho gaya!")
    }

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('appinstalled', handleAppInstalled)

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
      window.removeEventListener('appinstalled', handleAppInstalled)
    }
  }, [])

  const triggerInstallApp = async () => {
    if (installPrompt) {
      try {
        installPrompt.prompt()
        const { outcome } = await installPrompt.userChoice
        if (outcome === 'accepted') {
          setIsInstalled(true)
          setInstallPrompt(null)
          showToast("📲 Phone me app install ho rahi hai...")
        }
      } catch (err) {
        setIsInstallModalOpen(true)
      }
    } else {
      setIsInstallModalOpen(true)
    }
  }

  const toggleLikeSong = (songId) => {
    setSongs(songs.map(s => s.id === songId ? { ...s, isLiked: !s.isLiked } : s))
  }

  const toggleDownloadSong = (songId) => {
    setSongs(songs.map(s => s.id === songId ? { ...s, isDownloaded: !s.isDownloaded } : s))
  }

  // Double tap to like with exact tap position particle burst
  const handleTouchTap = (e) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const x = e.clientX || (e.touches && e.touches[0]?.clientX) || rect.width / 2
    const y = e.clientY || (e.touches && e.touches[0]?.clientY) || rect.height / 2
    
    toggleLikeSong(currentSong.id)
    const newHeart = { id: Date.now(), x, y }
    setHeartsList(prev => [...prev, newHeart])
    setTimeout(() => {
      setHeartsList(prev => prev.filter(h => h.id !== newHeart.id))
    }, 900)
  }

  // Swipe handling (Up = next song, Down = prev song, like Resso & TikTok)
  const handleTouchStart = (e) => {
    touchStartY.current = e.touches[0].clientY
  }

  const handleTouchMove = (e) => {
    const currentY = e.touches[0].clientY
    const delta = currentY - touchStartY.current
    touchDeltaY.current = delta
    setSwipeOffset(delta * 0.3)
    if (delta < -40) setSwipeIndicator('next')
    else if (delta > 40) setSwipeIndicator('prev')
    else setSwipeIndicator(null)
  }

  const handleTouchEnd = () => {
    const delta = touchDeltaY.current
    if (delta < -70) {
      playNext()
    } else if (delta > 70) {
      playPrev()
    }
    setSwipeOffset(0)
    setSwipeIndicator(null)
    touchDeltaY.current = 0
  }

  // Desktop mouse wheel scroll to change song
  const lastWheelTime = useRef(0)
  const handleWheel = (e) => {
    const now = Date.now()
    if (now - lastWheelTime.current < 600) return
    if (e.deltaY > 50) {
      lastWheelTime.current = now
      playNext()
    } else if (e.deltaY < -50) {
      lastWheelTime.current = now
      playPrev()
    }
  }

  const triggerReaction = (emoji) => {
    const id = Date.now() + Math.random()
    const left = Math.floor(Math.random() * 60) + 20
    setPartyReactions(prev => [...prev, { id, emoji, left }])
    setTimeout(() => {
      setPartyReactions(prev => prev.filter(r => r.id !== id))
    }, 2000)
  }

  // Upload local audio file
  const handleAudioUpload = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      const url = URL.createObjectURL(file)
      const newSong = {
        id: `custom_${Date.now()}`,
        title: file.name.replace(/\.[^/.]+$/, ""),
        artist: "Uploaded Track",
        album: "Local Audio",
        duration: 180,
        audioUrl: url,
        albumArt: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
        videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        alternateVibes: [],
        lyrics: [
          { time: 0, text: "Playing your local audio file" },
          { time: 8, text: "Create and trim a custom Vibe background!" },
          { time: 20, text: "Enjoy full Resso synced beats & vibes" }
        ],
        genre: "Custom",
        mood: "Chill",
        isLiked: true,
        isDownloaded: true,
        isCustom: true
      }
      setSongs([newSong, ...songs])
      setCurrentSongIndex(0)
      setIsPlaying(true)
      setCurrentTab('foryou')
    }
  }

  const showToast = (msg) => {
    setToastMessage(msg)
    setTimeout(() => {
      setToastMessage(prev => prev === msg ? null : prev)
    }, 2800)
  }

  // Lightning-fast search fetching 100% full-length songs (no 30s limit) + global catalog
  const searchOnlineTracks = async (term) => {
    const q = (term || '').trim()
    if (!q) return
    setIsSearchingOnline(true)
    setOnlineSearchError(null)

    try {
      // 1. Fetch 100% full-length songs from Audius Open Cloud
      const audiusPromise = fetch(`https://api.audius.co/v1/tracks/search?query=${encodeURIComponent(q)}&app_name=resso_music`)
        .then(r => r.ok ? r.json() : null)
        .catch(() => null)

      // 2. Fetch iTunes catalog in parallel
      const itunesPromise = fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(q)}&media=music&entity=song&limit=30`)
        .then(r => r.ok ? r.json() : null)
        .catch(() => null)

      const [audiusData, itunesData] = await Promise.all([audiusPromise, itunesPromise])

      const combinedResults = []

      // Add Audius full-length songs first (min duration 45 seconds to ensure complete songs)
      if (audiusData && Array.isArray(audiusData.data)) {
        const fullSongs = audiusData.data
          .filter(t => t.duration && t.duration >= 45)
          .map(t => {
            const art = t.artwork?.['480x480'] || t.artwork?.['1000x1000'] || t.artwork?.['150x150'] || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
            const dur = Math.round(t.duration)
            const cleanTitle = (t.title || "Full Track").replace(/\.mp3/gi, '').replace(/\(MP3_\d+K\)/gi, '')
            return {
              id: `audius_${t.id}`,
              trackName: cleanTitle,
              artistName: t.user?.name || "Music Artist",
              albumName: t.genre ? `${t.genre} • Studio Master` : "Studio Release",
              duration: dur,
              fullDuration: dur,
              audioStreamUrl: `https://api.audius.co/v1/tracks/${t.id}/stream?app_name=resso_music`,
              artworkUrl: art,
              genre: t.genre || "Full Song",
              isFullSong: true,
              sourceName: `100% Full Song (${Math.floor(dur / 60)}:${(dur % 60).toString().padStart(2, '0')})`
            }
          })
        combinedResults.push(...fullSongs)
      }

      // Add iTunes tracks (clearly marked as 30s Preview)
      if (itunesData && Array.isArray(itunesData.results)) {
        const previewTracks = itunesData.results
          .filter(t => t.previewUrl)
          .map(t => {
            const art = t.artworkUrl100
              ? t.artworkUrl100.replace('100x100bb', '600x600bb')
              : "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
            const realDuration = t.trackTimeMillis ? Math.round(t.trackTimeMillis / 1000) : 210

            return {
              id: `itunes_${t.trackId}`,
              trackName: t.trackName || "Track",
              artistName: t.artistName || "Artist",
              albumName: t.collectionName || "Single Release",
              duration: 30, // 30s preview clip
              fullDuration: realDuration,
              audioStreamUrl: t.previewUrl,
              artworkUrl: art,
              genre: t.primaryGenreName || "Music",
              isFullSong: false,
              sourceName: "30s Preview"
            }
          })
        combinedResults.push(...previewTracks)
      }

      setOnlineResults(combinedResults)
    } catch (err) {
      console.warn("Search error:", err)
      setOnlineSearchError("Search error. Tap a popular search term below to reconnect.")
    } finally {
      setIsSearchingOnline(false)
    }
  }

  // Pre-load trending internet tracks on first load
  useEffect(() => {
    searchOnlineTracks('Arijit Singh')
  }, [])

  // Live auto-search debounce as user types
  useEffect(() => {
    if (!onlineQuery || onlineQuery.trim().length < 2) return
    const timer = setTimeout(() => {
      searchOnlineTracks(onlineQuery)
    }, 450)
    return () => clearTimeout(timer)
  }, [onlineQuery])

  const createOnlineSongObject = (track) => {
    const artwork = track.artworkUrl || "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
    const durationSec = track.duration || 240
    
    const videoPresets = [
      { id: `v_${track.id}_1`, title: "Cyber Neon", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Neon" },
      { id: `v_${track.id}_2`, title: "Cosmic Glow", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", filter: "Cyberpunk" },
      { id: `v_${track.id}_3`, title: "Chill Drive", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", filter: "Dreamy" }
    ]
    const chosenVideo = videoPresets[Math.floor(Math.random() * videoPresets.length)]

    // Build synchronized lyrics across the FULL length of the song!
    const step = Math.max(10, Math.floor(durationSec / 8))
    const lyrics = [
      { time: 0, text: `♪ ${track.trackName} ♪` },
      { time: 4.0, text: `Artist: ${track.artistName}` },
      { time: Math.min(12, step * 1), text: `(Full Track Streaming in HD Quality 🎧)` },
      { time: Math.min(28, step * 2), text: `Feel the rhythm & acoustic vibrations... 🔥` },
      { time: Math.min(50, step * 3), text: `Album: ${track.albumName || 'Official Release'}` },
      { time: Math.min(85, step * 4), text: `Resso 3D Spatial Audio & Dynamic Bass Boost ⚡` },
      { time: Math.min(125, step * 5), text: `Every note and melody in high definition` },
      { time: Math.min(170, step * 6), text: `Complete full song playing without interruptions ✨` },
      { time: Math.min(220, step * 7), text: `Stay in the flow, feel the vibe! 🎵` }
    ].filter(l => l.time < durationSec)

    return {
      id: `online_${track.id}`,
      title: track.trackName,
      artist: track.artistName,
      album: track.albumName || 'Online Stream',
      duration: durationSec,
      audioUrl: track.audioStreamUrl,
      albumArt: artwork,
      videoUri: chosenVideo.uri,
      alternateVibes: videoPresets,
      lyrics: lyrics,
      genre: track.genre || 'Full Song',
      mood: ['Chill', 'Party', 'Focus', 'Late Night'][Math.floor(Math.random() * 4)],
      isLiked: false,
      isDownloaded: false,
      isOnline: true,
      isFullSong: track.isFullSong
    }
  }

  const playOnlineTrack = (track) => {
    const newSong = createOnlineSongObject(track)
    const existingIdx = songs.findIndex(s => s.id === newSong.id)
    let targetIndex = 0
    if (existingIdx !== -1) {
      targetIndex = existingIdx
    } else {
      setSongs(prev => [newSong, ...prev])
      targetIndex = 0
    }
    setCurrentSongIndex(targetIndex)
    setCurrentTime(0)
    setDuration(newSong.duration)
    setIsPlaying(true)
    setCurrentTab('foryou')

    // Force HTML5 Audio element to immediately set source and play
    setTimeout(() => {
      if (audioRef.current) {
        audioRef.current.currentTime = 0
        audioRef.current.src = newSong.audioUrl
        audioRef.current.load()
        const playPromise = audioRef.current.play()
        if (playPromise !== undefined) {
          playPromise.catch(e => {
            console.warn("Autoplay deferred:", e)
          })
        }
      }
    }, 80)

    showToast(track.isFullSong ? `Playing FULL Song: "${track.trackName}"! 🎧` : `Playing "${track.trackName}"! 🎵`)
  }

  const addToQueue = (track) => {
    const newSong = createOnlineSongObject(track)
    const existingIdx = songs.findIndex(s => s.id === newSong.id)
    if (existingIdx === -1) {
      setSongs(prev => [...prev, newSong])
      showToast(`Added "${track.trackName}" to queue! 🎶`)
    } else {
      showToast(`"${track.trackName}" already in playlist`)
    }
  }

  const playDirectUrl = () => {
    if (!directUrlInput.trim()) return
    const customId = `url_${Date.now()}`
    const title = directTitleInput.trim() || "Internet Audio Stream"
    const artist = directArtistInput.trim() || "Web Stream"
    const newSong = {
      id: customId,
      title: title,
      artist: artist,
      album: "Internet Stream",
      duration: 180,
      audioUrl: directUrlInput.trim(),
      albumArt: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
      videoUri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
      alternateVibes: [
        { id: `v_${customId}_1`, title: "Cyber Neon", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", filter: "Neon" },
        { id: `v_${customId}_2`, title: "Midnight Flow", uri: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", filter: "Cyberpunk" }
      ],
      lyrics: [
        { time: 0, text: `♪ ${title} ♪` },
        { time: 5, text: `Streaming live audio from direct URL 🌐` },
        { time: 12, text: `Enjoy with Resso Flash Sync & Spatial Audio` },
        { time: 22, text: `Vibrating at maximum acoustic frequency 🔥` }
      ],
      genre: "Web Stream",
      mood: "Party",
      isLiked: true,
      isDownloaded: false,
      isOnline: true
    }
    setSongs(prev => [newSong, ...prev])
    setCurrentSongIndex(0)
    setIsPlaying(true)
    setCurrentTab('foryou')
    setDirectUrlInput('')
    setDirectTitleInput('')
    setDirectArtistInput('')
    showToast(`Streaming "${title}"! 🎧`)
  }

  // Active bullet comments at the current second
  const currentBulletComments = comments.filter(
    c => c.songId === currentSong.id && Math.abs(c.time - Math.floor(currentTime)) <= 2
  )

  return (
    <div className="relative w-full h-screen bg-[#0A0910] text-white flex flex-col overflow-hidden select-none font-sans">
      {/* Toast Notification */}
      {toastMessage && (
        <div className="absolute top-16 inset-x-0 z-50 flex justify-center pointer-events-none px-4">
          <div className="bg-[#14131E]/95 border border-[#00F2FE]/50 backdrop-blur-md px-4 py-2 rounded-full text-xs font-bold text-white shadow-xl flex items-center gap-2 animate-bounce">
            <Globe size={14} className="text-[#00F2FE]" />
            <span>{toastMessage}</span>
          </div>
        </div>
      )}

      {/* Audio Engine - Persistent element with playsInline for lock-screen & background playback */}
      <audio
        ref={audioRef}
        src={currentSong.audioUrl}
        preload="auto"
        playsInline={true}
        crossOrigin="anonymous"
        onLoadedMetadata={() => {
          if (audioRef.current) {
            const d = audioRef.current.duration
            if (d && !isNaN(d) && d > 0) {
              setDuration(d)
            } else {
              setDuration(currentSong.duration)
            }
          }
        }}
        onCanPlay={() => {
          if (isPlaying && audioRef.current) {
            const p = audioRef.current.play()
            if (p !== undefined) {
              p.catch(e => console.warn("CanPlay play deferred:", e))
            }
          }
        }}
        onTimeUpdate={() => {
          if (audioRef.current) {
            const cur = audioRef.current.currentTime
            setCurrentTime(cur)
            const d = audioRef.current.duration
            if (d && !isNaN(d) && d > 0 && Math.abs(duration - d) > 2) {
              setDuration(d)
            }

            // Sync MediaSession timeline to lock screen scrubber
            if ('mediaSession' in navigator && 'setPositionState' in navigator.mediaSession) {
              try {
                const totalDur = d || duration || currentSong.duration
                if (totalDur > 0 && !isNaN(cur) && cur >= 0) {
                  navigator.mediaSession.setPositionState({
                    duration: Math.max(totalDur, 1),
                    playbackRate: playbackSpeed || 1,
                    position: Math.min(Math.max(cur, 0), totalDur)
                  })
                }
              } catch (err) {}
            }
          }
        }}
        onPlay={() => {
          setIsPlaying(true)
          if ('mediaSession' in navigator) {
            navigator.mediaSession.playbackState = 'playing'
          }
        }}
        onPause={() => {
          if (!audioRef.current?.ended) {
            setIsPlaying(false)
            if ('mediaSession' in navigator) {
              navigator.mediaSession.playbackState = 'paused'
            }
          }
        }}
        onError={(e) => {
          console.warn("Audio element stream warning:", e)
        }}
        onEnded={() => {
          if (repeatMode === 2) {
            if (audioRef.current) {
              audioRef.current.currentTime = 0
              audioRef.current.play().catch(e => console.warn(e))
            }
          } else {
            playNext()
          }
        }}
        volume={isVolumeBooster ? 1.0 : 0.85}
      />

      {/* Screen flash strobe for Flash Sync */}
      {screenFlash && (
        <div className="absolute inset-0 bg-white/35 z-50 pointer-events-none transition-opacity" />
      )}

      {/* Floating party mode reactions */}
      <div className="absolute inset-0 pointer-events-none z-40 overflow-hidden">
        {partyReactions.map(r => (
          <div
            key={r.id}
            className="absolute bottom-20 text-3xl animate-float-up"
            style={{ left: `${r.left}%` }}
          >
            {r.emoji}
          </div>
        ))}
      </div>

      {/* Floating Heart Particles from Double-Tap */}
      <div className="absolute inset-0 pointer-events-none z-40 overflow-hidden">
        {heartsList.map(h => (
          <div
            key={h.id}
            className="absolute -translate-x-1/2 -translate-y-1/2 animate-ping"
            style={{ left: h.x, top: h.y }}
          >
            <Heart size={64} className="text-[#FF0055] fill-[#FF0055] drop-shadow-lg" />
          </div>
        ))}
      </div>

      {/* Hidden file input for custom audio */}
      <input
        ref={audioInputRef}
        type="file"
        accept="audio/*"
        className="hidden"
        onChange={handleAudioUpload}
      />

      {/* Main Container */}
      <div className="flex-1 relative overflow-hidden">
        {currentTab === 'foryou' && (
          <div
            className="relative w-full h-full transition-transform duration-100 ease-out"
            style={{ transform: `translateY(${swipeOffset}px)` }}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            onWheel={handleWheel}
            onDoubleClick={handleTouchTap}
          >
            {/* Background Vibe Video */}
            <div className="absolute inset-0 z-0 overflow-hidden">
              <video
                ref={videoRef}
                key={currentSong.videoUri}
                src={currentSong.videoUri}
                autoPlay
                loop
                muted
                playsInline
                className="w-full h-full object-cover scale-105"
              />
              <div className="absolute inset-0 bg-[#FF0055]/15 mix-blend-color" />
              <div className="absolute inset-0 bg-gradient-to-b from-black/75 via-black/35 to-black/95" />
            </div>

            {/* Swipe Indicators */}
            {swipeIndicator && (
              <div className="absolute inset-x-0 top-1/2 -translate-y-1/2 z-30 flex items-center justify-center pointer-events-none">
                <div className="px-4 py-2 rounded-full bg-black/70 backdrop-blur-md border border-[#FF2A6D] text-xs font-bold text-white flex items-center gap-2">
                  {swipeIndicator === 'next' ? <ChevronDown className="animate-bounce" /> : <ChevronUp className="animate-bounce" />}
                  <span>{swipeIndicator === 'next' ? 'Next Track' : 'Previous Track'}</span>
                </div>
              </div>
            )}

            {/* Top Bar with OFFICIAL RESSO LOGO & BRANDING */}
            <div className="absolute top-0 left-0 right-0 z-20 pt-4 px-4 flex items-center justify-between">
              {/* Brand Logo & Name */}
              <div className="flex items-center gap-2.5">
                <RessoLogo className="w-8 h-8 drop-shadow" />
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="text-base font-black tracking-wider text-white font-sans">
                      RESSO
                    </span>
                    <span className="text-[9px] px-1.5 py-0.5 rounded bg-[#FF0055]/30 border border-[#FF0055] text-[#FF2A6D] font-bold">
                      PRO
                    </span>
                    {currentSong.isOnline && (
                      <span className="text-[9px] px-1.5 py-0.5 rounded bg-[#00F2FE]/25 border border-[#00F2FE]/60 text-[#00F2FE] font-bold flex items-center gap-0.5">
                        <Globe size={9} />
                        <span>LIVE</span>
                      </span>
                    )}
                  </div>
                  <p className="text-[10px] text-white/70">{currentSong.mood} Vibes • {currentSong.genre}</p>
                </div>
              </div>

              {/* Quick Actions Header */}
              <div className="flex items-center gap-1.5">
                {/* Install App on Mobile Phone */}
                <button
                  onClick={triggerInstallApp}
                  className={`flex items-center gap-1 px-2.5 py-1.5 rounded-full border transition ${
                    isInstalled
                      ? 'bg-emerald-500/20 border-emerald-500/40 text-emerald-300'
                      : 'bg-gradient-to-r from-[#FF0055]/30 to-[#FF2A6D]/30 border-[#FF0055]/70 text-white hover:bg-[#FF0055]/40 shadow-sm animate-pulse'
                  }`}
                  title="Phone me App Install karein"
                >
                  <Smartphone size={12} className={isInstalled ? 'text-emerald-400' : 'text-[#FF2A6D]'} />
                  <span className="text-[10px] font-bold">{isInstalled ? 'Installed' : 'Install'}</span>
                </button>

                {/* Lock Screen Background Play Indicator */}
                <button
                  onClick={() => {
                    showToast("🔒 Lock Screen Play Active: Screen lock hone par bhi gaana chalta rahega! Controls notification bar mein available hain.")
                  }}
                  className="flex items-center gap-1 px-2 py-1.5 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 hover:bg-emerald-500/30 transition"
                  title="Lock Screen & Background Audio Active"
                >
                  <Lock size={12} className="text-emerald-400" />
                  <span className="text-[10px] font-bold">Lock Play</span>
                </button>

                {/* Search Internet Songs Button */}
                <button
                  onClick={() => {
                    setSearchSource('online')
                    setCurrentTab('explore')
                  }}
                  className="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-black/60 border border-[#00F2FE]/50 text-[#00F2FE] hover:bg-[#00F2FE]/20 transition"
                  title="Search Internet Songs & Play"
                >
                  <Globe size={12} className="animate-pulse" />
                  <span className="text-[10px] font-bold">Online</span>
                </button>

                {/* Bullet Comments Toggle */}
                <button
                  onClick={() => setShowBulletComments(!showBulletComments)}
                  className={`p-1.5 rounded-full border transition ${
                    showBulletComments ? 'bg-[#00F2FE]/20 border-[#00F2FE] text-[#00F2FE]' : 'bg-black/50 border-white/10 text-white/60'
                  }`}
                  title="Toggle Bullet Comments"
                >
                  <MessageSquare size={14} />
                </button>

                {/* Party Mode / Listen Together */}
                <button
                  onClick={() => setIsPartyModeOpen(true)}
                  className="flex items-center gap-1 px-2.5 py-1.5 rounded-full bg-black/50 border border-white/10 text-xs font-semibold hover:bg-white/10 transition"
                  title="Party Mode / Listen Together"
                >
                  <Users size={12} className="text-[#05D9E8]" />
                  <span className="text-[10px] font-bold">Party</span>
                </button>

                {/* Sound FX & Equalizer */}
                <button
                  onClick={() => setIsSoundFxOpen(true)}
                  className="p-1.5 rounded-full bg-black/50 border border-white/10 text-white/80 hover:text-white"
                  title="Sound Effects & Equalizer"
                >
                  <SlidersHorizontal size={14} />
                </button>

                {/* Sleep Timer */}
                <button
                  onClick={() => setIsSleepTimerOpen(true)}
                  className={`p-1.5 rounded-full border transition ${
                    sleepTimerRemaining ? 'bg-[#FF0055] border-[#FF0055] text-white' : 'bg-black/50 border-white/10 text-white/80'
                  }`}
                  title="Sleep Timer"
                >
                  <Clock size={14} />
                </button>

                {/* Upload MP3 */}
                <button
                  onClick={() => audioInputRef.current?.click()}
                  className="p-1.5 rounded-full bg-black/50 border border-white/10 text-[#FF2A6D] hover:bg-white/10"
                  title="Upload MP3"
                >
                  <Upload size={14} />
                </button>
              </div>
            </div>

            {/* Bullet Comments Floating across video (Signature Resso Feature) */}
            {showBulletComments && currentBulletComments.length > 0 && (
              <div className="absolute top-20 inset-x-4 z-20 pointer-events-none flex flex-col gap-2">
                {currentBulletComments.map(c => (
                  <div
                    key={c.id}
                    className="self-start px-3 py-1.5 rounded-full bg-black/60 backdrop-blur-md border border-[#FF0055]/40 text-xs text-white flex items-center gap-2 animate-pulse shadow-lg"
                  >
                    <span className="w-2 h-2 rounded-full bg-[#00F2FE]" />
                    <span className="font-bold text-[#FF2A6D]">{c.user}:</span>
                    <span>{c.text}</span>
                  </div>
                ))}
              </div>
            )}

            {/* Scrolling Synced Lyrics (Resso core feature) */}
            <div
              ref={lyricsContainerRef}
              className="absolute inset-x-0 top-18 bottom-36 px-6 overflow-y-auto z-10 flex flex-col gap-5 text-left pr-20 no-scrollbar"
              style={{ scrollBehavior: 'smooth' }}
            >
              <div className="h-44 flex-shrink-0" />
              {currentSong.lyrics && currentSong.lyrics.map((line, idx) => {
                const isActive = idx === activeLyricIndex
                const isPast = idx < activeLyricIndex
                return (
                  <div
                    key={idx}
                    onClick={() => {
                      if (audioRef.current) {
                        audioRef.current.currentTime = line.time
                        setCurrentTime(line.time)
                      }
                    }}
                    className={`cursor-pointer transition-all duration-300 py-1 ${
                      isActive
                        ? 'text-white text-2xl md:text-3xl font-black scale-[1.03] origin-left drop-shadow-[0_4px_16px_rgba(255,0,85,0.6)]'
                        : isPast
                        ? 'text-white/45 text-lg md:text-xl font-semibold'
                        : 'text-white/20 text-lg md:text-xl font-medium'
                    }`}
                  >
                    {line.text}
                  </div>
                )
              })}
              <div className="h-44 flex-shrink-0" />
            </div>

            {/* Right Action Bar (Official Resso vertical action strip) */}
            <div className="absolute right-3.5 bottom-36 z-20 flex flex-col items-center gap-3.5">
              {/* Like / Heart */}
              <button
                onClick={() => toggleLikeSong(currentSong.id)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className={`w-11 h-11 rounded-full backdrop-blur-md flex items-center justify-center border transition active:scale-90 ${
                  currentSong.isLiked ? 'bg-[#FF0055]/30 border-[#FF0055]' : 'bg-black/40 border-white/10'
                }`}>
                  <Heart
                    size={22}
                    className={currentSong.isLiked ? 'text-[#FF0055] fill-[#FF0055]' : 'text-white'}
                  />
                </div>
                <span className="text-[10px] font-semibold text-white/90">
                  {currentSong.isLiked ? 'Liked' : 'Like'}
                </span>
              </button>

              {/* Time-Synced Comments */}
              <button
                onClick={() => setIsCommentsOpen(true)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-11 h-11 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center border border-white/10 transition active:scale-90">
                  <MessageCircle size={22} className="text-white" />
                </div>
                <span className="text-[10px] font-semibold text-white/90">
                  {comments.filter(c => c.songId === currentSong.id).length}
                </span>
              </button>

              {/* Vibe Switcher / Community Vibes */}
              <button
                onClick={() => setIsVibeSwitcherOpen(true)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-11 h-11 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center border border-white/10 transition active:scale-90">
                  <Video size={20} className="text-[#05D9E8]" />
                </div>
                <span className="text-[10px] font-semibold text-[#05D9E8]">Vibes</span>
              </button>

              {/* Vibe Creator */}
              <button
                onClick={() => setCurrentTab('vibe_creator')}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-[#FF0055] to-[#00F2FE] p-[1.5px] transition active:scale-90 shadow-md shadow-[#FF0055]/30">
                  <div className="w-full h-full bg-black/60 rounded-full flex items-center justify-center">
                    <Sparkles size={20} className="text-[#00F2FE]" />
                  </div>
                </div>
                <span className="text-[10px] font-semibold text-[#00F2FE]">Create</span>
              </button>

              {/* Lyric Quote Poster */}
              <button
                onClick={() => {
                  setSelectedPosterLyric(currentSong.lyrics[activeLyricIndex] || currentSong.lyrics[0])
                  setIsPosterOpen(true)
                }}
                className="flex flex-col items-center gap-1 group"
              >
                <div className="w-11 h-11 rounded-full bg-black/40 backdrop-blur-md flex items-center justify-center border border-white/10 transition active:scale-90">
                  <Share2 size={20} className="text-[#FFE600]" />
                </div>
                <span className="text-[10px] font-semibold text-white/90">Quote</span>
              </button>

              {/* Flash Sync Toggle */}
              <button
                onClick={() => setIsFlashSync(!isFlashSync)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className={`w-11 h-11 rounded-full backdrop-blur-md flex items-center justify-center border transition active:scale-90 ${
                  isFlashSync ? 'bg-[#00F5D4]/20 border-[#00F5D4]' : 'bg-black/40 border-white/10'
                }`}>
                  <Zap size={20} className={isFlashSync ? 'text-[#00F5D4] fill-[#00F5D4]' : 'text-white/70'} />
                </div>
                <span className="text-[9px] font-semibold text-white/80">
                  {isFlashSync ? 'Flash On' : 'Flash'}
                </span>
              </button>

              {/* Offline Download */}
              <button
                onClick={() => toggleDownloadSong(currentSong.id)}
                className="flex flex-col items-center gap-1 group"
              >
                <div className={`w-11 h-11 rounded-full backdrop-blur-md flex items-center justify-center border transition active:scale-90 ${
                  currentSong.isDownloaded ? 'bg-[#FF0055]/20 border-[#FF0055]' : 'bg-black/40 border-white/10'
                }`}>
                  <Download size={18} className={currentSong.isDownloaded ? 'text-[#FF0055]' : 'text-white/70'} />
                </div>
                <span className="text-[9px] font-semibold text-white/80">
                  {currentSong.isDownloaded ? 'Saved' : 'Get'}
                </span>
              </button>
            </div>

            {/* Bottom Playback Bar */}
            <div className="absolute bottom-0 inset-x-0 z-20 pb-3 pt-2 px-5 bg-gradient-to-t from-black via-black/85 to-transparent">
              {/* Song Title and Artist */}
              <div className="flex items-center justify-between mb-2">
                <div className="overflow-hidden">
                  <div className="flex items-center gap-2">
                    <h2 className="text-base font-bold text-white truncate">{currentSong.title}</h2>
                    {currentSong.isFullSong && (
                      <span className="px-1.5 py-0.5 rounded bg-emerald-500/25 text-emerald-300 border border-emerald-500/40 text-[9px] font-black uppercase tracking-wider flex-shrink-0 flex items-center gap-1">
                        <Sparkles size={9} />
                        <span>Full Song</span>
                      </span>
                    )}
                    <span className="px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-300 border border-sky-500/30 text-[9px] font-bold flex-shrink-0 flex items-center gap-0.5" title="Plays on Lock Screen & Background">
                      <Lock size={8} />
                      <span>Lock Play</span>
                    </span>
                  </div>
                  <p className="text-xs text-[#A09EB2] truncate">{currentSong.artist} • {currentSong.album}</p>
                </div>
                <div className="flex items-center gap-1 text-[11px] text-white/40">
                  <span>Swipe</span>
                  <ChevronUp size={13} className="animate-bounce" />
                </div>
              </div>

              {/* Progress Slider */}
              <div className="flex items-center gap-2 mb-2">
                <span className="text-[10px] text-[#A09EB2] w-8">{formatTime(currentTime)}</span>
                <input
                  type="range"
                  min="0"
                  max={duration || 100}
                  value={currentTime}
                  onChange={(e) => {
                    const t = parseFloat(e.target.value)
                    setCurrentTime(t)
                    if (audioRef.current) audioRef.current.currentTime = t
                  }}
                  className="flex-1 h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#FF0055]"
                />
                <span className="text-[10px] text-[#A09EB2] w-8 text-right">{formatTime(duration)}</span>
              </div>

              {/* Playback Controls */}
              <div className="flex items-center justify-between px-6">
                <button
                  onClick={() => setIsShuffle(!isShuffle)}
                  className={`p-2 transition ${isShuffle ? 'text-[#05D9E8]' : 'text-white/40'}`}
                  title="Shuffle"
                >
                  <Shuffle size={18} />
                </button>

                <button onClick={playPrev} className="p-2 text-white hover:text-[#FF0055] transition" title="Previous">
                  <SkipBack size={26} />
                </button>

                <button
                  onClick={togglePlay}
                  className="w-14 h-14 rounded-full bg-gradient-to-tr from-[#FF0055] to-[#FF2A6D] flex items-center justify-center text-white shadow-xl shadow-[#FF0055]/40 transition active:scale-95"
                  title="Play / Pause"
                >
                  {isPlaying ? <Pause size={28} /> : <Play size={28} className="translate-x-0.5" />}
                </button>

                <button onClick={playNext} className="p-2 text-white hover:text-[#FF0055] transition" title="Next">
                  <SkipForward size={26} />
                </button>

                <button
                  onClick={() => setRepeatMode((prev) => (prev + 1) % 3)}
                  className={`p-2 transition ${repeatMode > 0 ? 'text-[#05D9E8]' : 'text-white/40'}`}
                  title={repeatMode === 2 ? 'Repeat One' : repeatMode === 1 ? 'Repeat All' : 'Repeat Off'}
                >
                  <Repeat size={18} />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Explore & Internet Music Search Screen */}
        {currentTab === 'explore' && (
          <div className="w-full h-full p-5 overflow-y-auto pb-24">
            <div className="flex items-center gap-2 mb-1">
              <RessoLogo className="w-6 h-6" />
              <h1 className="text-2xl font-black text-white">Search & Stream</h1>
            </div>
            <p className="text-xs text-[#A09EB2] mb-4">Search internet songs worldwide, stream live with synchronized lyrics & vibes</p>

            {/* Source Switcher: Online Internet vs Full Songs vs Library vs Direct URL */}
            <div className="grid grid-cols-4 gap-1 p-1 bg-[#14131E] rounded-xl border border-white/5 mb-4">
              <button
                onClick={() => setSearchSource('online')}
                className={`py-2 px-1 rounded-lg text-xs font-bold transition flex items-center justify-center gap-1 ${
                  searchSource === 'online'
                    ? 'bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-white shadow-md shadow-[#FF0055]/30'
                    : 'text-[#A09EB2] hover:text-white'
                }`}
              >
                <Search size={13} />
                <span>Search</span>
              </button>

              <button
                onClick={() => setSearchSource('full_songs')}
                className={`py-2 px-1 rounded-lg text-xs font-bold transition flex items-center justify-center gap-1 ${
                  searchSource === 'full_songs'
                    ? 'bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-white shadow-md shadow-[#FF0055]/30'
                    : 'text-[#A09EB2] hover:text-white'
                }`}
              >
                <Flame size={13} className="text-amber-400" />
                <span>Full Songs</span>
              </button>

              <button
                onClick={() => setSearchSource('library')}
                className={`py-2 px-1 rounded-lg text-xs font-bold transition flex items-center justify-center gap-1 ${
                  searchSource === 'library'
                    ? 'bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-white shadow-md shadow-[#FF0055]/30'
                    : 'text-[#A09EB2] hover:text-white'
                }`}
              >
                <ListMusic size={13} />
                <span>Library</span>
              </button>

              <button
                onClick={() => setSearchSource('direct_url')}
                className={`py-2 px-1 rounded-lg text-xs font-bold transition flex items-center justify-center gap-1 ${
                  searchSource === 'direct_url'
                    ? 'bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-white shadow-md shadow-[#FF0055]/30'
                    : 'text-[#A09EB2] hover:text-white'
                }`}
              >
                <LinkIcon size={13} />
                <span>Link</span>
              </button>
            </div>

            {/* TAB 1: INTERNET MUSIC SEARCH */}
            {searchSource === 'online' && (
              <div>
                {/* Search Bar */}
                <form
                  onSubmit={(e) => {
                    e.preventDefault()
                    if (onlineQuery.trim()) {
                      searchOnlineTracks(onlineQuery)
                    }
                  }}
                  className="flex items-center gap-2 mb-3"
                >
                  <div className="relative flex-1">
                    <Search size={18} className="absolute left-3.5 top-3.5 text-[#00F2FE]" />
                    <input
                      type="text"
                      placeholder="Search songs, artists on internet (e.g. Arijit, Diljit, Pop)..."
                      value={onlineQuery}
                      onChange={(e) => setOnlineQuery(e.target.value)}
                      className="w-full pl-10 pr-9 py-2.5 rounded-xl bg-[#14131E] border border-[#00F2FE]/30 text-sm text-white placeholder-white/40 focus:outline-none focus:border-[#FF0055] focus:ring-1 focus:ring-[#FF0055]"
                    />
                    {onlineQuery && (
                      <button
                        type="button"
                        onClick={() => {
                          setOnlineQuery('')
                        }}
                        className="absolute right-3 top-3 text-white/50 hover:text-white"
                      >
                        <X size={16} />
                      </button>
                    )}
                  </div>
                  <button
                    type="submit"
                    disabled={isSearchingOnline}
                    className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-bold text-white shadow-md shadow-[#FF0055]/30 hover:brightness-110 active:scale-95 transition flex items-center gap-1.5 flex-shrink-0"
                  >
                    {isSearchingOnline ? (
                      <Loader2 size={16} className="animate-spin" />
                    ) : (
                      <>
                        <Globe size={15} />
                        <span>Search</span>
                      </>
                    )}
                  </button>
                </form>

                {/* Quick Trending Searches Chips */}
                <div className="mb-4">
                  <div className="flex items-center gap-1.5 text-xs text-[#A09EB2] mb-2">
                    <TrendingUp size={13} className="text-[#FF2A6D]" />
                    <span>Popular Searches:</span>
                  </div>
                  <div className="flex gap-2 overflow-x-auto pb-2 no-scrollbar">
                    {[
                      'Arijit Singh',
                      'Diljit Dosanjh',
                      'Sidhu Moose Wala',
                      'Karan Aujla',
                      'Taylor Swift',
                      'The Weeknd',
                      'Lo-Fi Beats',
                      'Alan Walker',
                      'AP Dhillon',
                      'Dua Lipa',
                      'Bollywood Hits'
                    ].map((term) => (
                      <button
                        key={term}
                        onClick={() => {
                          setOnlineQuery(term)
                          searchOnlineTracks(term)
                        }}
                        className="px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap bg-[#1B1927] border border-white/5 text-white/80 hover:border-[#FF0055]/50 hover:text-white transition active:scale-95 flex-shrink-0"
                      >
                        {term}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Search State: Loading, Error or Song List */}
                {isSearchingOnline ? (
                  <div className="py-16 flex flex-col items-center justify-center text-center">
                    <Loader2 size={36} className="animate-spin text-[#FF0055] mb-3" />
                    <p className="text-sm font-bold text-white">Searching the Internet...</p>
                    <p className="text-xs text-[#A09EB2] mt-1">Connecting to live high-res audio streams & artwork</p>
                  </div>
                ) : onlineSearchError ? (
                  <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-center mb-4">
                    <p className="text-xs text-red-400 mb-2">{onlineSearchError}</p>
                    <button
                      onClick={() => searchOnlineTracks(onlineQuery || 'Arijit Singh')}
                      className="px-4 py-1.5 rounded-lg bg-red-500/20 text-xs font-semibold text-red-300 hover:bg-red-500/30"
                    >
                      Retry Connection
                    </button>
                  </div>
                ) : onlineResults.length > 0 ? (
                  <div>
                    {/* Status & Filter Bar */}
                    <div className="flex items-center justify-between mb-3 bg-[#1B1927] px-3 py-2 rounded-xl border border-white/5">
                      <button
                        type="button"
                        onClick={() => setOnlyFullSongs(!onlyFullSongs)}
                        className={`px-2.5 py-1 rounded-lg text-xs font-bold transition flex items-center gap-1.5 ${
                          onlyFullSongs
                            ? 'bg-emerald-500/25 text-emerald-300 border border-emerald-500/50'
                            : 'bg-white/5 text-white/70 border border-white/10'
                        }`}
                      >
                        <CheckCircle size={13} className={onlyFullSongs ? 'text-emerald-400' : 'text-white/40'} />
                        <span>
                          {onlyFullSongs ? 'Full Songs Only' : 'All Songs'} (
                          {onlineResults.filter((r) => r.isFullSong).length} Full)
                        </span>
                      </button>

                      <span className="text-[10px] text-[#00F2FE] font-mono flex items-center gap-1 bg-[#00F2FE]/10 px-2 py-0.5 rounded-full border border-[#00F2FE]/20">
                        <Globe size={11} />
                        <span>
                          {
                            (onlyFullSongs && onlineResults.some((r) => r.isFullSong)
                              ? onlineResults.filter((r) => r.isFullSong)
                              : onlineResults
                            ).length
                          }{' '}
                          ready
                        </span>
                      </span>
                    </div>

                    <div className="space-y-3">
                      {(onlyFullSongs && onlineResults.some((r) => r.isFullSong)
                        ? onlineResults.filter((r) => r.isFullSong)
                        : onlineResults
                      ).map((item) => (
                        <div
                          key={item.id}
                          className="flex items-center gap-3 p-3 rounded-xl bg-[#14131E] border border-white/5 hover:border-[#FF0055]/40 transition group"
                        >
                          {/* Album Art with Quick Play Overlay */}
                          <div
                            onClick={() => playOnlineTrack(item)}
                            className="relative w-14 h-14 rounded-lg overflow-hidden flex-shrink-0 cursor-pointer group-hover:shadow-lg group-hover:shadow-[#FF0055]/20"
                          >
                            <img
                              src={item.artworkUrl}
                              alt={item.trackName}
                              onError={(e) => {
                                e.target.src = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                              }}
                              className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                            />
                            <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition">
                              <Play size={20} className="text-white fill-white translate-x-0.5" />
                            </div>
                          </div>

                          {/* Track Details */}
                          <div className="flex-1 min-w-0" onClick={() => playOnlineTrack(item)}>
                            <div className="flex items-center gap-1.5">
                              <h4 className="text-sm font-bold text-white truncate cursor-pointer hover:text-[#FF0055] transition">
                                {item.trackName}
                              </h4>
                              {item.isFullSong && (
                                <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-[9px] font-black uppercase flex-shrink-0">
                                  Full
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-[#A09EB2] truncate">
                              {item.artistName}
                            </p>
                            <div className="flex items-center gap-2 mt-1 flex-wrap">
                              {item.isFullSong ? (
                                <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-bold flex items-center gap-1">
                                  <Sparkles size={10} />
                                  <span>100% Full Song • {formatTime(item.duration)}</span>
                                </span>
                              ) : (
                                <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40 font-bold flex items-center gap-1">
                                  <span>30s Preview • {formatTime(item.duration)}</span>
                                </span>
                              )}
                              <span className="text-[10px] text-white/50 font-mono truncate max-w-[130px]">
                                {item.albumName || 'Studio Master'}
                              </span>
                            </div>
                          </div>

                          {/* Actions: Play Now & Add to Queue */}
                          <div className="flex items-center gap-1.5 flex-shrink-0">
                            <button
                              onClick={() => playOnlineTrack(item)}
                              className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-bold text-white flex items-center gap-1 shadow-md shadow-[#FF0055]/30 hover:brightness-110 active:scale-95 transition"
                              title="Play Immediately"
                            >
                              <Play size={13} className="fill-white translate-x-0.5" />
                              <span>Play</span>
                            </button>

                            <button
                              onClick={() => addToQueue(item)}
                              className="p-1.5 rounded-lg bg-[#1B1927] border border-white/10 text-white/70 hover:text-white hover:border-white/30 active:scale-95 transition"
                              title="Add to Playlist Queue"
                            >
                              <Plus size={16} />
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="py-12 text-center text-[#A09EB2]">
                    <Globe size={40} className="mx-auto text-white/20 mb-3" />
                    <p className="text-sm font-semibold text-white">No online songs found</p>
                    <p className="text-xs mt-1">Try searching for an artist or song name like "Arijit Singh", "Believer", or "Taylor Swift"</p>
                  </div>
                )}
              </div>
            )}

            {/* TAB 2: 100% FULL-LENGTH SONGS (NO 30S LIMIT) */}
            {searchSource === 'full_songs' && (
              <div className="space-y-4">
                <div className="p-3 rounded-xl bg-gradient-to-r from-amber-500/15 via-rose-500/10 to-transparent border border-amber-500/30 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-amber-500/20 flex items-center justify-center flex-shrink-0">
                    <Flame size={20} className="text-amber-400" />
                  </div>
                  <div>
                    <h3 className="text-xs font-bold text-white flex items-center gap-1.5">
                      <span>100% Full-Length Songs</span>
                      <span className="px-1.5 py-0.2 rounded bg-amber-400 text-black text-[9px] font-black uppercase">No 30s limit</span>
                    </h3>
                    <p className="text-[11px] text-[#A09EB2]">Complete tracks playing without cutoffs from start to finish</p>
                  </div>
                </div>

                <div className="space-y-3">
                  {FULL_FEATURED_SONGS.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center gap-3 p-3 rounded-xl bg-[#14131E] border border-amber-500/20 hover:border-amber-500/50 transition group"
                    >
                      <div
                        onClick={() => playOnlineTrack(item)}
                        className="relative w-14 h-14 rounded-lg overflow-hidden flex-shrink-0 cursor-pointer group-hover:shadow-lg group-hover:shadow-amber-500/20"
                      >
                        <img
                          src={item.artworkUrl}
                          alt={item.trackName}
                          className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                        />
                        <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition">
                          <Play size={20} className="text-white fill-white translate-x-0.5" />
                        </div>
                      </div>

                      <div className="flex-1 min-w-0" onClick={() => playOnlineTrack(item)}>
                        <h4 className="text-sm font-bold text-white truncate cursor-pointer hover:text-amber-400 transition">
                          {item.trackName}
                        </h4>
                        <p className="text-xs text-[#A09EB2] truncate">{item.artistName}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40 font-bold flex items-center gap-1">
                            <CheckCircle size={10} />
                            <span>{item.sourceName}</span>
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <button
                          onClick={() => playOnlineTrack(item)}
                          className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-amber-500 to-[#FF0055] text-xs font-bold text-white flex items-center gap-1 shadow-md hover:brightness-110 active:scale-95 transition"
                        >
                          <Play size={13} className="fill-white translate-x-0.5" />
                          <span>Play</span>
                        </button>
                        <button
                          onClick={() => addToQueue(item)}
                          className="p-1.5 rounded-lg bg-[#1B1927] border border-white/10 text-white/70 hover:text-white transition"
                        >
                          <Plus size={16} />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 2: DIRECT URL STREAMING */}
            {searchSource === 'direct_url' && (
              <div className="p-4 rounded-2xl bg-[#14131E] border border-white/10 space-y-4">
                <div className="flex items-center gap-2 text-white">
                  <LinkIcon size={18} className="text-[#00F2FE]" />
                  <h3 className="font-bold text-sm">Stream Any Online Audio URL</h3>
                </div>
                <p className="text-xs text-[#A09EB2]">
                  Paste any direct audio link (.mp3, .m4a, .aac, .ogg) or live radio stream from anywhere on the internet.
                </p>

                <div>
                  <label className="text-[11px] font-bold text-white/80 block mb-1">Direct Audio Stream URL *</label>
                  <input
                    type="url"
                    placeholder="https://example.com/song.mp3"
                    value={directUrlInput}
                    onChange={(e) => setDirectUrlInput(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-[#1B1927] border border-white/10 text-xs text-white focus:outline-none focus:border-[#00F2FE]"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] font-bold text-white/80 block mb-1">Song Title</label>
                    <input
                      type="text"
                      placeholder="My Stream Track"
                      value={directTitleInput}
                      onChange={(e) => setDirectTitleInput(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-[#1B1927] border border-white/10 text-xs text-white focus:outline-none focus:border-[#00F2FE]"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] font-bold text-white/80 block mb-1">Artist Name</label>
                    <input
                      type="text"
                      placeholder="Online Artist"
                      value={directArtistInput}
                      onChange={(e) => setDirectArtistInput(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-[#1B1927] border border-white/10 text-xs text-white focus:outline-none focus:border-[#00F2FE]"
                    />
                  </div>
                </div>

                <button
                  onClick={playDirectUrl}
                  disabled={!directUrlInput.trim()}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-bold text-white shadow-lg shadow-[#FF0055]/30 hover:brightness-110 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center justify-center gap-2"
                >
                  <Play size={16} className="fill-white" />
                  <span>Start Streaming Online</span>
                </button>
              </div>
            )}

            {/* TAB 3: LIBRARY & LOCAL SONGS */}
            {searchSource === 'library' && (
              <div>
                {/* Search */}
                <div className="relative mb-4">
                  <Search size={18} className="absolute left-3.5 top-3.5 text-[#A09EB2]" />
                  <input
                    type="text"
                    placeholder="Filter saved & local tracks..."
                    value={searchKeyword}
                    onChange={(e) => setSearchKeyword(e.target.value)}
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#14131E] border border-white/5 text-sm text-white focus:outline-none focus:border-[#FF0055]"
                  />
                </div>

                {/* Mood Stations */}
                <div className="flex gap-2 overflow-x-auto pb-3 mb-4 no-scrollbar">
                  {['All', 'Chill', 'Party', 'Focus', 'Late Night'].map((mood) => {
                    const active = (mood === 'All' && !selectedMood) || selectedMood === mood
                    return (
                      <button
                        key={mood}
                        onClick={() => setSelectedMood(mood === 'All' ? null : mood)}
                        className={`px-4 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                          active ? 'bg-[#FF0055] text-white shadow-md shadow-[#FF0055]/30' : 'bg-[#1B1927] text-[#A09EB2]'
                        }`}
                      >
                        {mood}
                      </button>
                    )
                  })}
                </div>

                {/* Song Feed */}
                <div className="space-y-3">
                  {songs
                    .filter(s => {
                      const matchesSearch = s.title.toLowerCase().includes(searchKeyword.toLowerCase()) ||
                                            s.artist.toLowerCase().includes(searchKeyword.toLowerCase())
                      const matchesMood = !selectedMood || s.mood === selectedMood
                      return matchesSearch && matchesMood
                    })
                    .map((song) => (
                      <div
                        key={song.id}
                        onClick={() => {
                          setCurrentSongIndex(songs.findIndex(s => s.id === song.id))
                          setIsPlaying(true)
                          setCurrentTab('foryou')
                        }}
                        className="flex items-center gap-3 p-3 rounded-xl bg-[#14131E] border border-white/5 hover:border-[#FF0055]/40 cursor-pointer transition"
                      >
                        <img src={song.albumArt} alt={song.title} className="w-13 h-13 rounded-lg object-cover" />
                        <div className="flex-1 min-w-0">
                          <h4 className="text-sm font-bold text-white truncate">{song.title}</h4>
                          <p className="text-xs text-[#A09EB2] truncate">{song.artist} • {song.genre}</p>
                        </div>
                        {song.isOnline && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-[#00F2FE]/20 text-[#00F2FE] border border-[#00F2FE]/30 font-mono">
                            Online
                          </span>
                        )}
                        {song.isDownloaded && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-white/10 text-[#05D9E8] font-mono">
                            Offline
                          </span>
                        )}
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            toggleLikeSong(song.id)
                          }}
                          className="p-2 text-[#A09EB2] hover:text-[#FF0055]"
                        >
                          <Heart size={18} className={song.isLiked ? 'text-[#FF0055] fill-[#FF0055]' : ''} />
                        </button>
                      </div>
                    ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Library Screen */}
        {currentTab === 'library' && (
          <div className="w-full h-full p-5 overflow-y-auto pb-24">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <RessoLogo className="w-6 h-6" />
                <div>
                  <h1 className="text-2xl font-black text-white">My Library</h1>
                  <p className="text-xs text-[#A09EB2]">Liked tracks, offline music & created vibes</p>
                </div>
              </div>
              <button
                onClick={() => setCurrentTab('vibe_creator')}
                className="p-2 rounded-xl bg-gradient-to-tr from-[#FF0055] to-[#FF2A6D] text-white shadow-md shadow-[#FF0055]/30"
              >
                <Sparkles size={20} />
              </button>
            </div>

            {/* Quick Stat Cards */}
            <div className="grid grid-cols-3 gap-2.5 mb-6">
              <div className="p-3 rounded-xl bg-[#14131E] border border-white/5">
                <Heart size={18} className="text-[#FF0055] fill-[#FF0055] mb-1" />
                <span className="text-base font-black">{songs.filter(s => s.isLiked).length}</span>
                <p className="text-[10px] text-[#A09EB2]">Liked</p>
              </div>
              <div className="p-3 rounded-xl bg-[#14131E] border border-white/5">
                <Download size={18} className="text-[#05D9E8] mb-1" />
                <span className="text-base font-black">{songs.filter(s => s.isDownloaded).length}</span>
                <p className="text-[10px] text-[#A09EB2]">Offline</p>
              </div>
              <div className="p-3 rounded-xl bg-[#14131E] border border-white/5">
                <Video size={18} className="text-[#FFE600] mb-1" />
                <span className="text-base font-black">{customVibes.length}</span>
                <p className="text-[10px] text-[#A09EB2]">Vibes</p>
              </div>
            </div>

            {/* Install Mobile App Banner */}
            <div className="mb-6 p-4 rounded-2xl bg-gradient-to-r from-[#FF0055]/20 via-[#7928CA]/20 to-[#00F2FE]/20 border border-[#FF0055]/40 flex flex-col gap-3">
              <div className="flex items-start gap-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-[#FF0055] to-[#7928CA] flex items-center justify-center flex-shrink-0 shadow-lg shadow-[#FF0055]/30">
                  <Smartphone size={24} className="text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <h3 className="text-sm font-black text-white">Install Resso on Phone</h3>
                    <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 text-[9px] font-black uppercase">
                      {isInstalled ? 'Installed' : 'App Ready'}
                    </span>
                  </div>
                  <p className="text-xs text-[#A09EB2] mt-0.5">
                    Phone me direct install karein - Screen lock hone par bhi gaana chalega, full-screen aur bina kisi storage problem ke!
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1">
                <button
                  onClick={triggerInstallApp}
                  className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-black text-white shadow-lg shadow-[#FF0055]/30 flex items-center justify-center gap-1.5 active:scale-95 transition"
                >
                  <Download size={14} />
                  <span>{installPrompt ? 'Direct Install Now 📲' : 'Install Guide & Steps 📲'}</span>
                </button>
                <button
                  onClick={() => {
                    if (navigator.clipboard) {
                      navigator.clipboard.writeText(window.location.href)
                      showToast("Link copy ho gaya! Phone ke Chrome me paste karein 📋")
                    } else {
                      showToast("App URL: " + window.location.href)
                    }
                  }}
                  className="px-3 py-2.5 rounded-xl bg-[#14131E] border border-white/10 text-xs font-bold text-white/80 hover:text-white flex items-center gap-1"
                  title="Copy Link"
                >
                  <Copy size={13} />
                  <span>Copy Link</span>
                </button>
              </div>
            </div>

            {/* Liked Songs */}
            <h3 className="text-sm font-bold text-white mb-3">Favorite Songs</h3>
            <div className="space-y-3 mb-6">
              {songs.filter(s => s.isLiked).map((song) => (
                <div
                  key={song.id}
                  onClick={() => {
                    setCurrentSongIndex(songs.findIndex(s => s.id === song.id))
                    setIsPlaying(true)
                    setCurrentTab('foryou')
                  }}
                  className="flex items-center gap-3 p-3 rounded-xl bg-[#14131E] border border-white/5 cursor-pointer"
                >
                  <img src={song.albumArt} alt={song.title} className="w-12 h-12 rounded-lg object-cover" />
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-bold text-white truncate">{song.title}</h4>
                    <p className="text-xs text-[#A09EB2] truncate">{song.artist}</p>
                  </div>
                  <Play size={16} className="text-[#05D9E8]" />
                </div>
              ))}
            </div>

            {/* Custom Vibes */}
            <h3 className="text-sm font-bold text-white mb-3">Created Video Vibes</h3>
            {customVibes.length === 0 ? (
              <div className="p-5 rounded-xl bg-[#14131E] text-center">
                <Video size={32} className="mx-auto text-white/30 mb-2" />
                <p className="text-sm font-semibold">No custom vibes yet</p>
                <p className="text-xs text-[#A09EB2] mb-3">Trim a local video to loop behind your favorite tracks</p>
                <button
                  onClick={() => setCurrentTab('vibe_creator')}
                  className="px-4 py-1.5 rounded-lg bg-[#FF0055] text-xs font-bold text-white"
                >
                  Create Vibe
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {customVibes.map((vibe) => (
                  <div
                    key={vibe.id}
                    className="flex items-center justify-between p-3 rounded-xl bg-[#14131E] border border-white/5"
                  >
                    <div>
                      <h4 className="text-sm font-bold text-white">{vibe.title}</h4>
                      <p className="text-xs text-[#05D9E8]">Trim: {vibe.start}s - {vibe.end}s • {vibe.filter}</p>
                    </div>
                    <button
                      onClick={() => {
                        setSongs(songs.map(s => s.id === currentSong.id ? { ...s, videoUri: vibe.videoUri } : s))
                        setCurrentTab('foryou')
                      }}
                      className="px-3 py-1 rounded-lg bg-[#FF0055] text-xs font-bold text-white"
                    >
                      Apply
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* VIBE CREATOR SCREEN */}
        {currentTab === 'vibe_creator' && (
          <VibeCreatorView
            currentSong={currentSong}
            onSaveVibe={(vibe) => {
              setCustomVibes([vibe, ...customVibes])
              setSongs(songs.map(s => s.id === currentSong.id ? { ...s, videoUri: vibe.videoUri } : s))
              setCurrentTab('foryou')
            }}
            onBack={() => setCurrentTab('foryou')}
          />
        )}
      </div>

      {/* Bottom Navigation */}
      {currentTab !== 'vibe_creator' && (
        <div className="h-16 bg-[#0A0910]/95 border-t border-white/10 flex items-center justify-around px-4 z-40">
          <button
            onClick={() => setCurrentTab('foryou')}
            className={`flex flex-col items-center gap-1 transition ${currentTab === 'foryou' ? 'text-[#FF0055]' : 'text-[#A09EB2]'}`}
          >
            <Disc size={20} />
            <span className="text-[10px] font-bold">For You</span>
          </button>

          <button
            onClick={() => {
              setSearchSource('online')
              setCurrentTab('explore')
            }}
            className={`flex flex-col items-center gap-1 transition ${currentTab === 'explore' ? 'text-[#FF0055]' : 'text-[#A09EB2]'}`}
          >
            <Compass size={20} />
            <span className="text-[10px] font-bold">Search & Vibe</span>
          </button>

          <button
            onClick={() => setCurrentTab('library')}
            className={`flex flex-col items-center gap-1 transition ${currentTab === 'library' ? 'text-[#FF0055]' : 'text-[#A09EB2]'}`}
          >
            <ListMusic size={20} />
            <span className="text-[10px] font-bold">Library</span>
          </button>
        </div>
      )}

      {/* Community Vibe Switcher Modal */}
      {isVibeSwitcherOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex flex-col justify-end">
          <div className="bg-[#14131E] rounded-t-2xl p-5 border-t border-white/10 max-h-[60vh] flex flex-col">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="font-bold text-white text-base">Community Vibes</h3>
                <p className="text-xs text-[#05D9E8]">Select background video for this song</p>
              </div>
              <button onClick={() => setIsVibeSwitcherOpen(false)} className="text-[#A09EB2]">
                <X size={20} />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 overflow-y-auto mb-4">
              {currentSong.alternateVibes && currentSong.alternateVibes.map((v) => (
                <div
                  key={v.id}
                  onClick={() => {
                    setSongs(songs.map(s => s.id === currentSong.id ? { ...s, videoUri: v.uri } : s))
                    setIsVibeSwitcherOpen(false)
                  }}
                  className={`p-3 rounded-xl border cursor-pointer transition ${
                    currentSong.videoUri === v.uri ? 'border-[#FF0055] bg-[#FF0055]/10' : 'border-white/10 bg-[#1B1927]'
                  }`}
                >
                  <Video size={20} className="text-[#05D9E8] mb-1" />
                  <h5 className="text-xs font-bold text-white">{v.title}</h5>
                  <span className="text-[10px] text-[#A09EB2]">{v.filter}</span>
                </div>
              ))}
            </div>

            <button
              onClick={() => {
                setIsVibeSwitcherOpen(false)
                setCurrentTab('vibe_creator')
              }}
              className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-bold text-white flex items-center justify-center gap-2 shadow-lg shadow-[#FF0055]/30"
            >
              <Sparkles size={16} />
              <span>Create New Custom Vibe</span>
            </button>
          </div>
        </div>
      )}

      {/* Listen Together / Party Mode Modal */}
      {isPartyModeOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-5">
          <div className="bg-[#14131E] rounded-2xl max-w-sm w-full p-5 border border-white/10">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <PartyPopper size={20} className="text-[#05D9E8]" />
                <h3 className="font-bold text-white text-base">Listen Together (Resso Room)</h3>
              </div>
              <button onClick={() => setIsPartyModeOpen(false)} className="text-[#A09EB2]">
                <X size={20} />
              </button>
            </div>

            <p className="text-xs text-[#A09EB2] mb-4">
              Synchronized party mode: Friends hear the music and synchronized lyrics at the exact same millisecond!
            </p>

            <div className="p-3 rounded-xl bg-[#1B1927] border border-white/10 mb-4 flex items-center justify-between">
              <div>
                <span className="text-[10px] text-[#A09EB2]">ROOM CODE</span>
                <p className="text-sm font-mono font-bold text-[#05D9E8]">RESSO-8821</p>
              </div>
              <button
                onClick={() => alert("Room Code copied to clipboard: RESSO-8821")}
                className="px-3 py-1 rounded-lg bg-white/10 text-xs font-semibold hover:bg-white/20"
              >
                Copy
              </button>
            </div>

            {/* Send Real-time floating reactions */}
            <div className="mb-4">
              <span className="text-xs font-bold text-white block mb-2">Send Live Beat Reaction</span>
              <div className="flex justify-between">
                {['🔥', '❤️', '⚡', '🚀', '😍', '👏'].map((emoji) => (
                  <button
                    key={emoji}
                    onClick={() => triggerReaction(emoji)}
                    className="w-10 h-10 rounded-full bg-[#1B1927] text-lg hover:scale-125 transition flex items-center justify-center"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={() => setIsPartyModeOpen(false)}
              className="w-full py-2.5 rounded-xl bg-[#FF0055] text-xs font-bold text-white"
            >
              Close Party Screen
            </button>
          </div>
        </div>
      )}

      {/* Sleep Timer Modal */}
      {isSleepTimerOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-5">
          <div className="bg-[#14131E] rounded-2xl max-w-xs w-full p-5 border border-white/10 text-center">
            <Clock size={32} className="mx-auto text-[#FF0055] mb-2" />
            <h3 className="font-bold text-white text-base mb-1">Sleep Timer</h3>
            <p className="text-xs text-[#A09EB2] mb-4">Music will automatically fade and stop</p>

            <div className="space-y-2 mb-4">
              {[
                { label: '15 Minutes', secs: 15 * 60 },
                { label: '30 Minutes', secs: 30 * 60 },
                { label: '45 Minutes', secs: 45 * 60 },
                { label: 'End of Track', secs: Math.max(10, Math.floor(duration - currentTime)) }
              ].map(t => (
                <button
                  key={t.label}
                  onClick={() => {
                    setSleepTimerRemaining(t.secs)
                    setIsSleepTimerOpen(false)
                  }}
                  className="w-full py-2 rounded-xl bg-[#1B1927] text-xs font-semibold text-white hover:bg-[#FF0055] transition"
                >
                  {t.label}
                </button>
              ))}

              {sleepTimerRemaining && (
                <button
                  onClick={() => {
                    setSleepTimerRemaining(null)
                    setIsSleepTimerOpen(false)
                  }}
                  className="w-full py-2 rounded-xl bg-red-500/20 text-xs font-semibold text-red-400"
                >
                  Turn Off Timer
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Sound Effects & Equalizer Modal */}
      {isSoundFxOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-5">
          <div className="bg-[#14131E] rounded-2xl max-w-xs w-full p-5 border border-white/10">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-bold text-white text-base">Sound FX & Equalizer</h3>
              <button onClick={() => setIsSoundFxOpen(false)} className="text-[#A09EB2]">
                <X size={20} />
              </button>
            </div>

            <div className="space-y-3 mb-4">
              {/* Playback Speed (Slowed + Reverb / Nightcore) */}
              <div className="p-3 rounded-xl bg-[#1B1927]">
                <div className="flex justify-between items-center mb-2">
                  <h5 className="text-xs font-bold text-white">Playback Speed</h5>
                  <span className="text-[11px] font-mono text-[#05D9E8] font-bold">{playbackSpeed}x</span>
                </div>
                <div className="flex gap-2">
                  {[0.8, 1.0, 1.25].map(speed => (
                    <button
                      key={speed}
                      onClick={() => setPlaybackSpeed(speed)}
                      className={`flex-1 py-1 rounded-lg text-xs font-semibold transition ${
                        playbackSpeed === speed ? 'bg-[#FF0055] text-white' : 'bg-black/40 text-[#A09EB2]'
                      }`}
                    >
                      {speed === 0.8 ? 'Slowed' : speed === 1.25 ? 'Sped Up' : '1.0x'}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-[#1B1927]">
                <div>
                  <h5 className="text-xs font-bold text-white">Bass Boost</h5>
                  <p className="text-[10px] text-[#A09EB2]">Extra punchy low frequencies</p>
                </div>
                <input
                  type="checkbox"
                  checked={bassBoost}
                  onChange={(e) => setBassBoost(e.target.checked)}
                  className="accent-[#FF0055] w-4 h-4 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-[#1B1927]">
                <div>
                  <h5 className="text-xs font-bold text-white">3D Spatial Audio</h5>
                  <p className="text-[10px] text-[#A09EB2]">Immersive 360° concert sound</p>
                </div>
                <input
                  type="checkbox"
                  checked={spatialAudio}
                  onChange={(e) => setSpatialAudio(e.target.checked)}
                  className="accent-[#05D9E8] w-4 h-4 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-[#1B1927]">
                <div>
                  <h5 className="text-xs font-bold text-white">+150% Volume Booster</h5>
                  <p className="text-[10px] text-[#A09EB2]">Pre-amp gain multiplier</p>
                </div>
                <input
                  type="checkbox"
                  checked={isVolumeBooster}
                  onChange={(e) => setIsVolumeBooster(e.target.checked)}
                  className="accent-[#FF0055] w-4 h-4 cursor-pointer"
                />
              </div>
            </div>

            <button
              onClick={() => setIsSoundFxOpen(false)}
              className="w-full py-2.5 rounded-xl bg-[#FF0055] text-xs font-bold text-white shadow-lg shadow-[#FF0055]/30"
            >
              Apply Settings
            </button>
          </div>
        </div>
      )}

      {/* Time-Synced Comments Bottom Sheet */}
      {isCommentsOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex flex-col justify-end">
          <div className="bg-[#14131E] rounded-t-2xl max-h-[75vh] flex flex-col p-5 border-t border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="font-bold text-white text-base">Vibe Comments</h3>
                <p className="text-xs text-[#FF0055]">{currentSong.title}</p>
              </div>
              <button onClick={() => setIsCommentsOpen(false)} className="text-[#A09EB2]">
                <X size={20} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-4 pr-1 mb-4 no-scrollbar">
              {comments.filter(c => c.songId === currentSong.id).map((c) => (
                <div key={c.id} className="flex items-start justify-between">
                  <div className="flex gap-3">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-[#FF0055] to-[#7928CA] flex items-center justify-center text-xs font-bold">
                      {c.user[0]}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-white">{c.user}</span>
                        <span
                          onClick={() => {
                            if (audioRef.current) {
                              audioRef.current.currentTime = c.time
                              setCurrentTime(c.time)
                            }
                          }}
                          className="text-[10px] font-mono text-[#05D9E8] bg-black/40 px-1.5 py-0.5 rounded cursor-pointer hover:underline"
                        >
                          {formatTime(c.time)}
                        </span>
                      </div>
                      <p className="text-xs text-white/80 mt-0.5">{c.text}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      setComments(comments.map(item => item.id === c.id ? {
                        ...item,
                        likes: item.liked ? item.likes - 1 : item.likes + 1,
                        liked: !item.liked
                      } : item))
                    }}
                    className="flex flex-col items-center text-[#A09EB2]"
                  >
                    <Heart size={14} className={c.liked ? 'text-[#FF0055] fill-[#FF0055]' : ''} />
                    <span className="text-[10px]">{c.likes}</span>
                  </button>
                </div>
              ))}
            </div>

            <div className="flex items-center gap-2 pt-2 border-t border-white/5">
              <input
                type="text"
                placeholder={`Comment at ${formatTime(currentTime)}...`}
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && newComment.trim()) {
                    setComments([
                      ...comments,
                      {
                        id: Date.now(),
                        songId: currentSong.id,
                        user: "You",
                        text: newComment.trim(),
                        time: Math.floor(currentTime),
                        likes: 0,
                        liked: false
                      }
                    ])
                    setNewComment('')
                  }
                }}
                className="flex-1 bg-[#1B1927] rounded-full px-4 py-2.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-[#FF0055]"
              />
              <button
                onClick={() => {
                  if (newComment.trim()) {
                    setComments([
                      ...comments,
                      {
                        id: Date.now(),
                        songId: currentSong.id,
                        user: "You",
                        text: newComment.trim(),
                        time: Math.floor(currentTime),
                        likes: 0,
                        liked: false
                      }
                    ])
                    setNewComment('')
                  }
                }}
                className="w-9 h-9 rounded-full bg-[#FF0055] flex items-center justify-center text-white"
              >
                <Plus size={18} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Mobile App Install Modal */}
      {isInstallModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-[#14131E] rounded-3xl max-w-sm w-full p-5 border border-white/10 flex flex-col max-h-[90vh] overflow-y-auto shadow-2xl">
            {/* Header */}
            <div className="w-full flex items-center justify-between pb-3 border-b border-white/10 mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#FF0055] to-[#7928CA] flex items-center justify-center shadow-md">
                  <Smartphone size={18} className="text-white" />
                </div>
                <div>
                  <h3 className="font-black text-white text-base">Phone Me Install Karein</h3>
                  <p className="text-[11px] text-[#00F2FE]">Resso App Installation Guide</p>
                </div>
              </div>
              <button onClick={() => setIsInstallModalOpen(false)} className="p-1 rounded-full text-[#A09EB2] hover:text-white bg-white/5">
                <X size={18} />
              </button>
            </div>

            {/* Direct QR Code to open on phone instantly without USB */}
            <div className="mb-4 p-3.5 rounded-2xl bg-gradient-to-b from-white/10 to-white/5 border border-white/15 flex flex-col items-center text-center">
              <span className="text-xs font-black text-white mb-1 flex items-center gap-1.5">
                <Smartphone size={14} className="text-[#FF0055]" />
                <span>फोन के कैमरा से यह QR कोड स्कैन करें</span>
              </span>
              <p className="text-[10px] text-[#A09EB2] mb-3">
                USB केबल या Developer Mode की बिल्कुल ज़रूरत नहीं है!
              </p>
              
              <div className="p-2 bg-white rounded-2xl shadow-xl border-2 border-[#FF0055]/50 flex items-center justify-center">
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(typeof window !== 'undefined' ? window.location.href : 'https://ais-pre-swwphp3aezjgjnbqezgr63-6084417371.asia-southeast1.run.app')}`}
                  alt="Scan QR Code with Phone"
                  className="w-36 h-36 object-contain rounded-lg"
                  onError={(e) => {
                    e.target.style.display = 'none'
                  }}
                />
              </div>
              <p className="text-[10px] text-emerald-400 font-bold mt-2">
                📸 Phone Camera / Google Lens खोलकर स्क्रीन पर दिखाएं
              </p>
            </div>

            {/* Direct Install Button (If supported by current browser) */}
            {installPrompt && (
              <div className="mb-4 p-3.5 rounded-2xl bg-gradient-to-r from-emerald-500/20 to-teal-500/20 border border-emerald-500/50 flex flex-col gap-2">
                <div className="flex items-center gap-2 text-emerald-300 font-bold text-xs">
                  <CheckCircle size={16} />
                  <span>1-Click Direct Install Ready!</span>
                </div>
                <button
                  onClick={async () => {
                    if (installPrompt) {
                      try {
                        installPrompt.prompt()
                        const { outcome } = await installPrompt.userChoice
                        if (outcome === 'accepted') {
                          setIsInstalled(true)
                          setInstallPrompt(null)
                          setIsInstallModalOpen(false)
                          showToast("🎉 App phone me install ho raha hai!")
                        }
                      } catch (e) {
                        console.warn(e)
                      }
                    }
                  }}
                  className="w-full py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-xs font-black text-white shadow-lg shadow-emerald-500/30 flex items-center justify-center gap-2 active:scale-95 transition"
                >
                  <Download size={15} />
                  <span>Tap Here to Install Instantly</span>
                </button>
              </div>
            )}

            {/* Android Instructions */}
            <div className="mb-4 p-3.5 rounded-2xl bg-[#1B1927] border border-white/5 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black text-white flex items-center gap-1.5">
                  <span>🤖 Android Phone (Google Chrome)</span>
                </span>
                <span className="text-[9px] px-2 py-0.5 rounded-full bg-[#00F2FE]/20 text-[#00F2FE] font-bold">Recommended</span>
              </div>

              <div className="space-y-2 text-[11px] text-white/80">
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-white/10 flex items-center justify-center text-[10px] font-bold flex-shrink-0 mt-0.5">1</span>
                  <p>Link ko <b>Google Chrome</b> browser me kholein (WhatsApp/Instagram ke browser se bahar niklein).</p>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-white/10 flex items-center justify-center text-[10px] font-bold flex-shrink-0 mt-0.5">2</span>
                  <p>Chrome me upar danyi taraf <b>3 डॉट्स (⋮)</b> par tap karein.</p>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-[#FF0055]/30 text-[#FF0055] flex items-center justify-center text-[10px] font-bold flex-shrink-0 mt-0.5">3</span>
                  <p><b>"Install app"</b> ya <b>"Add to Home screen" (होम स्क्रीन में जोड़ें)</b> par click karein.</p>
                </div>
                <div className="flex items-start gap-2">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/30 text-emerald-300 flex items-center justify-center text-[10px] font-bold flex-shrink-0 mt-0.5">4</span>
                  <p><b>"Install"</b> dabayein — App turant aapke phone me Resso icon ke sath download &amp; install ho jayegi!</p>
                </div>
              </div>
            </div>

            {/* iPhone Instructions */}
            <div className="mb-4 p-3.5 rounded-2xl bg-[#1B1927] border border-white/5 space-y-2">
              <span className="text-xs font-black text-white flex items-center gap-1.5">
                <span>🍎 iPhone (Safari Browser)</span>
              </span>
              <div className="space-y-1.5 text-[11px] text-white/80">
                <p>1. Safari me link kholein aur niche <b>Share button (📤)</b> par tap karein.</p>
                <p>2. List me scroll karke <b>"Add to Home Screen" (+)</b> chunein aur "Add" dabayein.</p>
              </div>
            </div>

            {/* Perks of Installed App */}
            <div className="mb-4 px-2 text-[10px] text-white/60 space-y-1">
              <div className="flex items-center gap-1.5 text-emerald-400">
                <Check size={12} />
                <span>Screen Lock hone par bhi gaana chalta rahega</span>
              </div>
              <div className="flex items-center gap-1.5 text-emerald-400">
                <Check size={12} />
                <span>Zero Storage issue - Phone ki memory full nahi hogi</span>
              </div>
              <div className="flex items-center gap-1.5 text-emerald-400">
                <Check size={12} />
                <span>Full screen video vibes aur synced lyrics</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex gap-2">
              <button
                onClick={() => {
                  if (navigator.clipboard) {
                    navigator.clipboard.writeText(window.location.href)
                    showToast("Link copied! Google Chrome me paste karein 📋")
                  } else {
                    showToast("Link: " + window.location.href)
                  }
                }}
                className="flex-1 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-bold text-white flex items-center justify-center gap-1.5 transition"
              >
                <Copy size={14} />
                <span>Copy App Link</span>
              </button>

              <button
                onClick={() => setIsInstallModalOpen(false)}
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-xs font-black text-white shadow-lg shadow-[#FF0055]/30 flex items-center justify-center gap-1.5 transition"
              >
                <span>Theek Hai (Close)</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lyric Quote Poster Dialog */}
      {isPosterOpen && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-md flex items-center justify-center p-5">
          <div className="bg-[#14131E] rounded-2xl max-w-sm w-full p-5 border border-white/10 flex flex-col items-center text-center">
            <div className="w-full flex items-center justify-between mb-3">
              <div className="flex items-center gap-1.5">
                <RessoLogo className="w-5 h-5" />
                <h3 className="font-bold text-white text-base">Resso Lyric Card</h3>
              </div>
              <button onClick={() => setIsPosterOpen(false)} className="text-[#A09EB2]">
                <X size={20} />
              </button>
            </div>

            {/* Poster Card */}
            <div className={`w-full h-64 rounded-xl p-6 flex flex-col justify-between shadow-2xl relative overflow-hidden mb-4 ${
              posterTheme === 'Sunset Dream' ? 'bg-gradient-to-br from-[#FF5E3A] via-[#FF0055] to-[#FF7A00]' :
              posterTheme === 'Midnight Ocean' ? 'bg-gradient-to-br from-[#0F2027] via-[#203A43] to-[#2C5364]' :
              'bg-gradient-to-br from-[#14002C] via-[#4A00E0] to-[#FF0055]'
            }`}>
              <span className="text-4xl text-white/20 font-serif leading-none text-left">“</span>
              <p className="text-lg font-black text-white italic drop-shadow px-2">
                {selectedPosterLyric?.text || "Every starfall echoes back to you"}
              </p>
              <div className="flex items-center justify-between pt-2 border-t border-white/20">
                <span className="text-xs text-white/90 font-medium">{currentSong.title} • {currentSong.artist}</span>
                <span className="text-[10px] font-black tracking-widest text-[#FFE600]">RESSO</span>
              </div>
            </div>

            {/* Poster Themes */}
            <div className="flex gap-2 w-full mb-4">
              {['Neon Cyber', 'Sunset Dream', 'Midnight Ocean'].map((theme) => (
                <button
                  key={theme}
                  onClick={() => setPosterTheme(theme)}
                  className={`flex-1 py-1.5 rounded-lg text-[10px] font-bold ${
                    posterTheme === theme ? 'bg-[#FF0055] text-white' : 'bg-[#1B1927] text-[#A09EB2]'
                  }`}
                >
                  {theme.split(' ')[0]}
                </button>
              ))}
            </div>

            <button
              onClick={() => {
                alert("Lyric poster copied to clipboard and ready to share!")
                setIsPosterOpen(false)
              }}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#FF2A6D] text-sm font-bold text-white shadow-lg shadow-[#FF0055]/30 flex items-center justify-center gap-2"
            >
              <Share2 size={16} />
              <span>Share Lyric Quote</span>
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

// Dedicated Vibe Creator View
function VibeCreatorView({ currentSong, onSaveVibe, onBack }) {
  const [videoUri, setVideoUri] = useState('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4')
  const [vibeTitle, setVibeTitle] = useState(`${currentSong?.title || "Song"} Vibe`)
  const [filterType, setFilterType] = useState('Neon')
  const [startTrim, setStartTrim] = useState(0)
  const [endTrim, setEndTrim] = useState(15)
  const [videoDuration, setVideoDuration] = useState(30)

  const videoPreviewRef = useRef(null)
  const fileInputRef = useRef(null)

  const handleVideoFile = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      const url = URL.createObjectURL(file)
      setVideoUri(url)
    }
  }

  useEffect(() => {
    const video = videoPreviewRef.current
    if (!video) return

    const handleTimeUpdate = () => {
      if (video.currentTime >= endTrim || video.currentTime < startTrim) {
        video.currentTime = startTrim
        video.play().catch(() => {})
      }
    }

    video.addEventListener('timeupdate', handleTimeUpdate)
    return () => video.removeEventListener('timeupdate', handleTimeUpdate)
  }, [startTrim, endTrim])

  return (
    <div className="w-full h-full p-5 overflow-y-auto bg-[#0A0910] text-white flex flex-col pb-20">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <RessoLogo className="w-6 h-6" />
          <div>
            <h2 className="text-xl font-bold">Create Song Vibe</h2>
            <p className="text-xs text-[#FF0055]">{currentSong?.title}</p>
          </div>
        </div>
        <button onClick={onBack} className="p-2 text-[#A09EB2] hover:text-white">
          <X size={22} />
        </button>
      </div>

      <div className="relative w-full h-64 rounded-2xl overflow-hidden bg-black mb-4 border border-white/10">
        <video
          ref={videoPreviewRef}
          src={videoUri}
          autoPlay
          loop
          muted
          playsInline
          onLoadedMetadata={(e) => {
            const dur = Math.floor(e.target.duration)
            setVideoDuration(dur)
            setEndTrim(Math.min(15, dur))
          }}
          className="w-full h-full object-cover"
        />

        <div className={`absolute inset-0 pointer-events-none ${
          filterType === 'Cyberpunk' ? 'bg-[#00E5FF]/20 mix-blend-color' :
          filterType === 'Dreamy' ? 'bg-[#E056FD]/20 mix-blend-color' :
          filterType === 'Retro VHS' ? 'bg-[#FF9F1A]/20 mix-blend-color' :
          'bg-[#FF0055]/20 mix-blend-color'
        }`} />

        <div className="absolute bottom-3 right-3 bg-black/70 backdrop-blur-md px-2.5 py-1 rounded-md text-[11px] font-mono text-[#05D9E8]">
          Trim: {endTrim - startTrim}s loop
        </div>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept="video/*"
        className="hidden"
        onChange={handleVideoFile}
      />
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => fileInputRef.current?.click()}
          className="flex-1 py-2.5 rounded-xl bg-[#FF0055] font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-[#FF0055]/30"
        >
          <Video size={16} />
          <span>Pick Local Video</span>
        </button>

        <button
          onClick={() => setVideoUri('https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4')}
          className="px-4 py-2.5 rounded-xl bg-[#1B1927] font-semibold text-xs text-[#05D9E8]"
        >
          Preset Video
        </button>
      </div>

      <div className="bg-[#14131E] p-4 rounded-xl border border-white/5 mb-4">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-1.5 text-xs font-bold text-white">
            <Scissors size={14} className="text-[#05D9E8]" />
            <span>Trim Loop Bounds</span>
          </div>
          <span className="text-xs font-mono text-[#05D9E8] font-bold">{startTrim}s - {endTrim}s</span>
        </div>

        <div className="space-y-3">
          <div>
            <div className="flex justify-between text-[10px] text-[#A09EB2] mb-1">
              <span>Start point: {startTrim}s</span>
              <span>Total: {videoDuration}s</span>
            </div>
            <input
              type="range"
              min="0"
              max={videoDuration - 3}
              value={startTrim}
              onChange={(e) => {
                const s = parseInt(e.target.value)
                setStartTrim(s)
                if (endTrim <= s) setEndTrim(Math.min(videoDuration, s + 10))
                if (videoPreviewRef.current) videoPreviewRef.current.currentTime = s
              }}
              className="w-full h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#FF0055]"
            />
          </div>

          <div>
            <div className="flex justify-between text-[10px] text-[#A09EB2] mb-1">
              <span>End point: {endTrim}s</span>
            </div>
            <input
              type="range"
              min={startTrim + 2}
              max={videoDuration}
              value={endTrim}
              onChange={(e) => setEndTrim(parseInt(e.target.value))}
              className="w-full h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#05D9E8]"
            />
          </div>
        </div>
      </div>

      <div className="mb-4">
        <label className="text-xs text-[#A09EB2] font-semibold block mb-1">Vibe Name</label>
        <input
          type="text"
          value={vibeTitle}
          onChange={(e) => setVibeTitle(e.target.value)}
          placeholder="Give this vibe a name..."
          className="w-full bg-[#14131E] border border-white/10 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-[#FF0055]"
        />
      </div>

      <div className="mb-6">
        <label className="text-xs text-[#A09EB2] font-semibold block mb-2">Aesthetic Filter</label>
        <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
          {['Neon', 'Cyberpunk', 'Dreamy', 'Retro VHS'].map((filter) => (
            <button
              key={filter}
              onClick={() => setFilterType(filter)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                filterType === filter ? 'bg-[#FF0055] text-white shadow-md shadow-[#FF0055]/30' : 'bg-[#14131E] text-[#A09EB2]'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </div>

      <button
        onClick={() => {
          onSaveVibe({
            id: Date.now(),
            songId: currentSong.id,
            title: vibeTitle.trim() || `${currentSong.title} Vibe`,
            videoUri: videoUri,
            start: startTrim,
            end: endTrim,
            filter: filterType
          })
        }}
        className="w-full py-3.5 rounded-xl bg-gradient-to-r from-[#FF0055] to-[#05D9E8] font-bold text-sm text-white shadow-xl shadow-[#FF0055]/30"
      >
        Save &amp; Apply Vibe
      </button>
    </div>
  )
}
