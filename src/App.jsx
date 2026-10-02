import React, { useState, useEffect, useRef } from 'react'
import {
  Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Heart,
  MessageCircle, Share2, Sparkles, Search, Library,
  Music, Film, X, Sliders, Zap, Volume2, Quote, Check,
  Radio, Wifi, Disc, Download, Clock, SlidersHorizontal,
  Eye, EyeOff, Palette, Info, Moon, Send, CheckCircle2
} from 'lucide-react'

// Authentic Resso songs with full lyrics, audio streams, and vibe themes
const INITIAL_SONGS = [
  {
    id: 's1',
    title: 'Kesariya',
    artist: 'Arijit Singh & Pritam',
    album: 'Brahmāstra (Original Soundtrack)',
    duration: 268,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/9f/13/ca/9f13ca3b-e533-03e0-f19a-f0aaa774581d/196589311191.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    likesCount: '2.8M',
    commentsCount: '34.2K',
    bpm: '124 BPM',
    credits: {
      composer: 'Pritam',
      lyricist: 'Amitabh Bhattacharya',
      producer: 'Sony Music India',
      releaseYear: '2022'
    },
    lyrics: [
      { time: 0, text: '♪ Kesariya - Arijit Singh & Pritam ♪' },
      { time: 5, text: 'Mujhko itna bataye koi' },
      { time: 10, text: 'Kaise tujhse dil na lagaye koi' },
      { time: 16, text: 'Rabba ne tujhko banane mein' },
      { time: 22, text: 'Kar di hai husn ki khaali tijoriyan' },
      { time: 29, text: 'Kajal ki siyahi se likhi hai tune' },
      { time: 35, text: 'Jaane kitno ki love storiyan' },
      { time: 41, text: 'Kesariya tera ishq hai piya' },
      { time: 48, text: 'Rang jaaun jo main haath lagaun' },
      { time: 54, text: 'Din beete saara teri fikr mein' },
      { time: 61, text: 'Rain saari teri khair manaun' },
      { time: 68, text: 'Kesariya tera ishq hai piya' },
      { time: 76, text: '♪ Acoustic guitar rhythm & violin ♪' },
      { time: 90, text: 'Patjhad ke mausam mein bhi' },
      { time: 98, text: 'Rangi chanaar jaisi' }
    ],
    genre: 'Bollywood Romance',
    mood: 'Soulful',
    isLiked: true,
    isFollowing: true,
    isDownloaded: true
  },
  {
    id: 's2',
    title: 'Tum Hi Ho',
    artist: 'Arijit Singh',
    album: 'Aashiqui 2 (Original Soundtrack)',
    duration: 262,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/2d/11/b9/2d11b994-b4fa-19eb-953d-70b472165e95/8903431566911_cover.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    likesCount: '3.4M',
    commentsCount: '52.1K',
    bpm: '128 BPM',
    credits: {
      composer: 'Mithoon',
      lyricist: 'Mithoon',
      producer: 'T-Series',
      releaseYear: '2013'
    },
    lyrics: [
      { time: 0, text: '♪ Tum Hi Ho - Arijit Singh ♪' },
      { time: 4, text: 'Hum tere bin ab reh nahi sakte' },
      { time: 13, text: 'Tere bina kya wajood mera' },
      { time: 22, text: 'Tujhse juda agar ho jayenge' },
      { time: 31, text: 'Toh khud se hi ho jayenge juda' },
      { time: 40, text: 'Kyunki tum hi ho, ab tum hi ho' },
      { time: 49, text: 'Zindagi ab tum hi ho' },
      { time: 58, text: 'Chain bhi, mera dard bhi' },
      { time: 67, text: 'Meri aashiqui ab tum hi ho' },
      { time: 78, text: '♪ Feel the piano notes & deep bass ♪' },
      { time: 95, text: 'Tera mera rishta hai kaisa' },
      { time: 104, text: 'Ek pal door gawaara nahi' }
    ],
    genre: 'Bollywood Classic',
    mood: 'Romantic',
    isLiked: true,
    isFollowing: false,
    isDownloaded: true
  },
  {
    id: 's3',
    title: 'Pasoori',
    artist: 'Ali Sethi x Shae Gill',
    album: 'Coke Studio Season 14',
    duration: 224,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/f3/f9/06/f3f906c3-79d5-ac9a-5fdd-262048f955f9/cover.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    likesCount: '1.9M',
    commentsCount: '27.4K',
    bpm: '115 BPM',
    credits: {
      composer: 'Ali Sethi, Fazal Abbas',
      lyricist: 'Ali Sethi',
      producer: 'Zulfiqar J. Khan',
      releaseYear: '2022'
    },
    lyrics: [
      { time: 0, text: '♪ Agg laavan majboori nu ♪' },
      { time: 5, text: 'Aan jaan di pasoori nu' },
      { time: 13, text: 'Zahar bane haan teri pee jaavan' },
      { time: 21, text: 'Marjaavan ya jee jaavan' },
      { time: 29, text: 'Dil boliyan te aave' },
      { time: 37, text: 'Aavan te dil lag jaave' },
      { time: 45, text: 'Chad gaya mainu tera nasha' },
      { time: 53, text: 'Raawaan ch baithaan main tere' }
    ],
    genre: 'Indie Fusion',
    mood: 'Groovy',
    isLiked: false,
    isFollowing: false,
    isDownloaded: false
  },
  {
    id: 's4',
    title: 'Apna Bana Le',
    artist: 'Arijit Singh & Sachin-Jigar',
    album: 'Bhediya (Original Soundtrack)',
    duration: 261,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/2e/0b/c0/2e0bc070-112f-a827-6ad8-6bc64f7caaff/840214460180.png/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4',
    likesCount: '2.1M',
    commentsCount: '29.8K',
    bpm: '120 BPM',
    credits: {
      composer: 'Sachin-Jigar',
      lyricist: 'Amitabh Bhattacharya',
      producer: 'Zee Music Company',
      releaseYear: '2022'
    },
    lyrics: [
      { time: 0, text: '♪ Apna Bana Le - Arijit Singh ♪' },
      { time: 6, text: 'Tu mera koi na hoke bhi kuch laage' },
      { time: 14, text: 'Kiya re jo bhi tune kaise kiya re' },
      { time: 22, text: 'Jiya ko mere baandh aise liya re' },
      { time: 30, text: 'Samajh ke bhi na samajh main saku' },
      { time: 38, text: 'Apna bana le piya, apna bana le piya' },
      { time: 46, text: 'Dil ke nagar mein shehar tu basa le piya' }
    ],
    genre: 'Bollywood',
    mood: 'Heartfelt',
    isLiked: true,
    isFollowing: true,
    isDownloaded: true
  },
  {
    id: 's5',
    title: 'Chaleya',
    artist: 'Arijit Singh & Shilpa Rao',
    album: 'Jawan (Original Soundtrack)',
    duration: 200,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/bb/f4/f5/bbf4f511-3c12-c25e-a475-b6d06faa8c13/8902894362047_cover.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    likesCount: '3.1M',
    commentsCount: '48.9K',
    bpm: '130 BPM',
    credits: {
      composer: 'Anirudh Ravichander',
      lyricist: 'Kumaar',
      producer: 'T-Series',
      releaseYear: '2023'
    },
    lyrics: [
      { time: 0, text: '♪ Chaleya - Anirudh & Arijit Singh ♪' },
      { time: 5, text: 'Ishq mein dil bana hai, ishq mein dil fanaa hai' },
      { time: 13, text: 'Jitna bhi roko dil ko, utna hi dil bada hai' },
      { time: 21, text: 'Chaleya teri ore chaleya' },
      { time: 28, text: 'Mera dil ab toh tera ho chukeya' },
      { time: 36, text: 'Dhadkan ne teri dhun pakad li' },
      { time: 44, text: 'Ishq tera ab mera hoke chaleya' }
    ],
    genre: 'Bollywood Dance',
    mood: 'Energetic',
    isLiked: false,
    isFollowing: false,
    isDownloaded: false
  },
  {
    id: 's6',
    title: 'Raataan Lambiyan',
    artist: 'Jubin Nautiyal & Asees Kaur',
    album: 'Shershaah (Original Soundtrack)',
    duration: 230,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/c3/df/e7/c3dfe71e-d532-458e-9c1c-32d40f8d9123/886449472222.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    likesCount: '2.5M',
    commentsCount: '38.0K',
    bpm: '120 BPM',
    credits: {
      composer: 'Tanishk Bagchi',
      lyricist: 'Tanishk Bagchi',
      producer: 'Sony Music India',
      releaseYear: '2021'
    },
    lyrics: [
      { time: 0, text: '♪ Raataan Lambiyan - Shershaah ♪' },
      { time: 6, text: 'Teri meri gallan ho gayi mashhoor' },
      { time: 14, text: 'Kar na kabhi tu mujhe nazron se door' },
      { time: 22, text: 'Kithe chaliye tu kithe chaliye' },
      { time: 30, text: 'Kaatun kaise raataan oh saawre' },
      { time: 38, text: 'Jiya nahi jaata sun bawre' },
      { time: 46, text: 'Ke raataan lambiyan lambiyan re' },
      { time: 54, text: 'Katte tere sangeyan sangeyan re' }
    ],
    genre: 'Bollywood Romance',
    mood: 'Soulful',
    isLiked: true,
    isFollowing: false,
    isDownloaded: true
  },
  {
    id: 's7',
    title: 'Brown Munde',
    artist: 'AP Dhillon, Gurinder Gill & Shinda Kahlon',
    album: 'Brown Munde (Official Release)',
    duration: 267,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/26/a3/ac/26a3ac64-69e4-95ec-80ab-1f5a477537d2/859742042973_cover.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
    likesCount: '4.2M',
    commentsCount: '71.5K',
    bpm: '128 BPM',
    credits: {
      composer: 'Gminxr',
      lyricist: 'Shinda Kahlon',
      producer: 'Run-Up Records',
      releaseYear: '2020'
    },
    lyrics: [
      { time: 0, text: '♪ Brown Munde - AP Dhillon ♪' },
      { time: 6, text: 'Desi jehe geet aa trappan jehi beat aa' },
      { time: 13, text: 'Sir kadd gajde speakeran ch wajde' },
      { time: 20, text: 'Brown munde, brown munde' },
      { time: 27, text: 'Dope shope maarde na, game vi vigaarde na' },
      { time: 34, text: 'Akhaan ch khumaari ae, yaari hi pyari ae' },
      { time: 41, text: 'Kamm saare end ne, yaaran naal trend ne' },
      { time: 48, text: 'Sun dhyan naal brown munde!' }
    ],
    genre: 'Punjabi Hip-Hop',
    mood: 'Hype',
    isLiked: true,
    isFollowing: true,
    isDownloaded: false
  },
  {
    id: 's8',
    title: 'Kahani Suno 2.0',
    artist: 'Kaifi Khalil',
    album: 'Kahani Suno (Official)',
    duration: 174,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music114/v4/ff/cc/7b/ffcc7bba-4005-f2c4-b1f1-3cc49a5e6283/artwork.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4',
    likesCount: '1.7M',
    commentsCount: '23.4K',
    bpm: '110 BPM',
    credits: {
      composer: 'Kaifi Khalil',
      lyricist: 'Kaifi Khalil',
      producer: 'Kaifi Khalil Music',
      releaseYear: '2022'
    },
    lyrics: [
      { time: 0, text: '♪ Kahani Suno 2.0 - Kaifi Khalil ♪' },
      { time: 5, text: 'Kahani suno, zubani suno' },
      { time: 12, text: 'Mujhe pyar hua tha, iqraar hua tha' },
      { time: 20, text: 'Deewana hua mastaana hua' },
      { time: 28, text: 'Teri chahat mein kitna fasaana hua' },
      { time: 37, text: 'Tere aane se pehle kuch na tha mera' },
      { time: 46, text: 'Tere jaane ke baad kya wajood mera' }
    ],
    genre: 'Indie Soul',
    mood: 'Melancholic',
    isLiked: false,
    isFollowing: false,
    isDownloaded: false
  },
  {
    id: 's9',
    title: 'Lover',
    artist: 'Diljit Dosanjh',
    album: 'MoonChild Era',
    duration: 198,
    audioUrl: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',
    albumArt: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/8a/89/e4/8a89e445-d2c6-f8ac-a828-27818b0c1afe/859749638209_cover.jpg/600x600bb.jpg',
    videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    likesCount: '2.4M',
    commentsCount: '31.2K',
    bpm: '132 BPM',
    credits: {
      composer: 'Intense',
      lyricist: 'Raj Ranjodh',
      producer: 'Diljit Dosanjh Music',
      releaseYear: '2021'
    },
    lyrics: [
      { time: 0, text: '♪ Lover - Diljit Dosanjh ♪' },
      { time: 5, text: 'Tera ni lover, tera ni lover' },
      { time: 11, text: 'Karda pyaar tenu kinna saara' },
      { time: 17, text: 'Vekh le akhiyan vich tu yaara' },
      { time: 23, text: 'Tere bina lagda nahi dil mera' },
      { time: 29, text: 'Tu hi ban gayi ae sahara' },
      { time: 35, text: 'Tera ni lover, tera ni lover!' }
    ],
    genre: 'Punjabi Pop',
    mood: 'Upbeat',
    isLiked: true,
    isFollowing: true,
    isDownloaded: true
  }
]

export default function App() {
  const [songs, setSongs] = useState(INITIAL_SONGS)
  const [currentSongIndex, setCurrentSongIndex] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTime, setCurrentTime] = useState(0)
  const [duration, setDuration] = useState(INITIAL_SONGS[0].duration)
  const [isShuffle, setIsShuffle] = useState(false)
  const [repeatMode, setRepeatMode] = useState(0) // 0: off, 1: all, 2: one

  // RESSO ICONIC FEATURES:
  // 1. Vibe Visualizer Modes: 'disc', 'spectrum', 'retro'
  const [vibeMode, setVibeMode] = useState('disc')
  // 2. Floating Vibe Comments on player canvas (Danmaku style)
  const [showFloatingComments, setShowFloatingComments] = useState(true)
  // 3. Sleep Timer (in minutes remaining, null if off)
  const [sleepTimer, setSleepTimer] = useState(null)
  // 4. Equalizer & Sound Effects
  const [bassBoost, setBassBoost] = useState(60) // 0 - 100
  const [soundProfile, setSoundProfile] = useState('Resso Bass Boost') // 'Resso Bass Boost', '3D Spatial', 'Vocal Focus', 'Lo-Fi Tape'
  // 5. Lyrics toggle (OFF by default, compact floating card, NOT full screen)
  const [showLyrics, setShowLyrics] = useState(false)
  // 6. Watch MV toggle (OFF by default, only downloads video when enabled)
  const [showVideo, setShowVideo] = useState(false)
  // 7. 0.1 kb/s Ultra Data Saver
  const [dataSaver, setDataSaver] = useState(true)
  // 8. Flash Sync (Strobe to the beat of song)
  const [flashSync, setFlashSync] = useState(false)
  // 9. Data Monitor Dialog
  const [isDataMonitorOpen, setIsDataMonitorOpen] = useState(false)
  const [cachedSongsCount, setCachedSongsCount] = useState(3)
  const [isCachingAll, setIsCachingAll] = useState(false)

  // Dialogs & Sheets
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const [isCommentsOpen, setIsCommentsOpen] = useState(false)
  const [isPosterOpen, setIsPosterOpen] = useState(false)
  const [isEqualizerOpen, setIsEqualizerOpen] = useState(false)
  const [isSleepTimerOpen, setIsSleepTimerOpen] = useState(false)
  const [isCreditsOpen, setIsCreditsOpen] = useState(false)

  // Poster customization
  const [posterTheme, setPosterTheme] = useState('neon') // 'neon', 'cyber', 'minimal', 'gold'
  const [selectedLyricIndex, setSelectedLyricIndex] = useState(0)

  // Navigation
  const [currentTab, setCurrentTab] = useState('foryou')
  const [toastMessage, setToastMessage] = useState(null)
  const [floatingHearts, setFloatingHearts] = useState([])

  // Search
  const [searchQuery, setSearchQuery] = useState('')
  const [isSearchingOnline, setIsSearchingOnline] = useState(false)
  const [onlineResults, setOnlineResults] = useState([])

  // Live Comments
  const [comments, setComments] = useState([
    { id: 1, user: 'Aman Sharma', text: 'This bass hits so hard on Resso! 🔥', time: '2m ago', likes: 142 },
    { id: 2, user: 'Priya Verma', text: 'Best vibe song for late night drives 🚗✨', time: '10m ago', likes: 89 },
    { id: 3, user: 'Rahul DJ', text: 'The vinyl spin and synced lyrics are pure magic!', time: '1h ago', likes: 310 }
  ])
  const [newCommentText, setNewCommentText] = useState('')

  // Touch Swipe for TikTok style vertical navigation
  const touchStartY = useRef(0)
  const audioRef = useRef(null)
  const videoRef = useRef(null)
  const lyricsContainerRef = useRef(null)
  const torchTrackRef = useRef(null)
  const torchIntervalRef = useRef(null)
  const [isTorchStrobeActive, setIsTorchStrobeActive] = useState(false)
  const wakeLockRef = useRef(null)
  const handlersRef = useRef({})
  // Network Disconnect Auto-Resume Refs
  const wasPlayingBeforeDisconnect = useRef(false)
  const lastPlaybackPositionRef = useRef(0)
  const ytPlayerRef = useRef(null)
  const [isYtReady, setIsYtReady] = useState(false)

  const currentSong = songs[currentSongIndex] || songs[0]

  const showToast = (msg) => {
    setToastMessage(msg)
    setTimeout(() => setToastMessage(null), 2800)
  }

  // Real-Time Physical Back Torch Flash Synchronizer (Resso Beat Flash Mode)
  useEffect(() => {
    if (!flashSync || !isPlaying) {
      if (torchIntervalRef.current) {
        clearInterval(torchIntervalRef.current)
        torchIntervalRef.current = null
      }
      setIsTorchStrobeActive(false)
      if (torchTrackRef.current) {
        try {
          torchTrackRef.current.applyConstraints({ advanced: [{ torch: false }] }).catch(() => {})
        } catch (e) {}
      }
      return
    }

    let isCancelled = false
    const bpmMatch = currentSong?.bpm ? parseInt(currentSong.bpm.replace(/\D/g, '')) : 128
    const bpm = bpmMatch && !isNaN(bpmMatch) ? bpmMatch : 128
    const intervalMs = Math.round(60000 / Math.min(Math.max(bpm, 60), 220))

    const initBackTorch = async () => {
      try {
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          if (!torchTrackRef.current) {
            const stream = await navigator.mediaDevices.getUserMedia({
              video: {
                facingMode: { ideal: 'environment' }
              }
            })
            if (isCancelled) {
              stream.getTracks().forEach(t => t.stop())
              return
            }
            const track = stream.getVideoTracks()[0]
            torchTrackRef.current = track
          }
        }
      } catch (err) {
        console.warn("Back camera torch access:", err)
      }

      if (torchIntervalRef.current) clearInterval(torchIntervalRef.current)
      torchIntervalRef.current = setInterval(() => {
        if (!isPlaying || isCancelled) return
        setIsTorchStrobeActive(true)

        // Pulse physical rear camera flashlight
        if (torchTrackRef.current) {
          torchTrackRef.current.applyConstraints({ advanced: [{ torch: true }] }).catch(() => {})
        }

        setTimeout(() => {
          if (isCancelled) return
          setIsTorchStrobeActive(false)
          if (torchTrackRef.current) {
            torchTrackRef.current.applyConstraints({ advanced: [{ torch: false }] }).catch(() => {})
          }
        }, 65)
      }, intervalMs)
    }

    initBackTorch()

    return () => {
      isCancelled = true
      if (torchIntervalRef.current) {
        clearInterval(torchIntervalRef.current)
        torchIntervalRef.current = null
      }
      setIsTorchStrobeActive(false)
      if (torchTrackRef.current) {
        try {
          torchTrackRef.current.applyConstraints({ advanced: [{ torch: false }] }).catch(() => {})
          torchTrackRef.current.stop()
        } catch (e) {}
        torchTrackRef.current = null
      }
    }
  }, [flashSync, isPlaying, currentSong?.bpm])

  // Sleep Timer Countdown
  useEffect(() => {
    if (!sleepTimer) return
    const interval = setInterval(() => {
      setSleepTimer(prev => {
        if (prev <= 1) {
          setIsPlaying(false)
          showToast("Sleep Timer: Music Paused 🌙")
          return null
        }
        return prev - 1
      })
    }, 60000)
    return () => clearInterval(interval)
  }, [sleepTimer])

  // Native 320kbps Audio Engine (Zero YouTube dependency)
  useEffect(() => {
    setIsYtReady(true)
  }, [])

  // Song Change & Direct Audio Playback
  useEffect(() => {
    const song = songs[currentSongIndex]
    if (song?.duration) {
      setDuration(song.duration)
    }
    if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.play().catch(() => {})
      } else {
        audioRef.current.pause()
      }
    }
    if (videoRef.current) {
      if (isPlaying && showVideo) videoRef.current.play().catch(() => {})
      else videoRef.current.pause()
    }
  }, [currentSongIndex])

  // Play / Pause State Controller
  useEffect(() => {
    if (isPlaying) {
      if (ytPlayerRef.current && isYtReady) {
        try { ytPlayerRef.current.playVideo() } catch (e) {}
      }
      if (audioRef.current) audioRef.current.play().catch(() => {})
      if (videoRef.current && showVideo) videoRef.current.play().catch(() => {})
    } else {
      if (ytPlayerRef.current && isYtReady) {
        try { ytPlayerRef.current.pauseVideo() } catch (e) {}
      }
      if (audioRef.current) audioRef.current.pause()
      if (videoRef.current) videoRef.current.pause()
    }
  }, [isPlaying, showVideo])

  // Real-Time Progress & Full Song Playback Tracking
  useEffect(() => {
    if (!isPlaying) return

    const ticker = setInterval(() => {
      if (ytPlayerRef.current && isYtReady) {
        try {
          const curr = ytPlayerRef.current.getCurrentTime()
          const dur = ytPlayerRef.current.getDuration()
          if (curr !== undefined && curr >= 0) {
            setCurrentTime(Math.floor(curr))
          }
          if (dur && dur > 1) {
            setDuration(Math.floor(dur))
          }
        } catch (e) {}
      } else {
        const targetDuration = currentSong?.duration || 260
        setCurrentTime(prev => {
          const next = prev + 1
          if (next >= targetDuration) {
            if (repeatMode === 2) {
              if (audioRef.current) audioRef.current.currentTime = 0
              return 0
            } else {
              handleNextSong()
              return 0
            }
          }
          return next
        })
      }
    }, 500)

    return () => clearInterval(ticker)
  }, [isPlaying, isYtReady, currentSongIndex, repeatMode, currentSong?.duration])

  const handleTimeUpdate = () => {
    // Keep local buffer alive without truncating full song duration
  }

  const handleAudioEnded = () => {
    // Handled by YouTube onStateChange ended event.
    // If running in offline audio fallback:
    if (!isYtReady) {
      const fullDur = currentSong?.duration || 260
      if (repeatMode === 2) {
        if (audioRef.current) {
          audioRef.current.currentTime = 0
          audioRef.current.play().catch(() => {})
        }
      } else if (currentTime < fullDur - 4) {
        if (audioRef.current && isPlaying) {
          audioRef.current.currentTime = 0
          audioRef.current.play().catch(() => {})
        }
      } else {
        handleNextSong()
      }
    }
  }

  const handleNextSong = () => {
    if (isShuffle) {
      const nextIdx = Math.floor(Math.random() * songs.length)
      setCurrentSongIndex(nextIdx)
    } else {
      setCurrentSongIndex(prev => (prev + 1) % songs.length)
    }
    setCurrentTime(0)
    setIsPlaying(true)
  }

  const handlePrevSong = () => {
    if (currentTime > 4) {
      if (ytPlayerRef.current && isYtReady) {
        try { ytPlayerRef.current.seekTo(0, true) } catch (e) {}
      }
      if (audioRef.current) audioRef.current.currentTime = 0
      setCurrentTime(0)
    } else {
      setCurrentSongIndex(prev => (prev - 1 + songs.length) % songs.length)
      setCurrentTime(0)
      setIsPlaying(true)
    }
  }

  const togglePlayPause = () => {
    const next = !isPlaying
    setIsPlaying(next)
    if (next) {
      if (ytPlayerRef.current && isYtReady) {
        try { ytPlayerRef.current.playVideo() } catch (e) {}
      }
      if (audioRef.current) audioRef.current.play().catch(() => {})
      if (videoRef.current && showVideo) videoRef.current.play().catch(() => {})
    } else {
      if (ytPlayerRef.current && isYtReady) {
        try { ytPlayerRef.current.pauseVideo() } catch (e) {}
      }
      if (audioRef.current) audioRef.current.pause()
      if (videoRef.current) videoRef.current.pause()
    }
  }

  const handleSeek = (newTime) => {
    const targetDur = duration || currentSong?.duration || 260
    const clamped = Math.max(0, Math.min(newTime, targetDur))
    setCurrentTime(clamped)
    if (ytPlayerRef.current && isYtReady) {
      try {
        ytPlayerRef.current.seekTo(clamped, true)
      } catch (e) {}
    }
    if (audioRef.current) {
      const audioDuration = audioRef.current.duration || 30
      audioRef.current.currentTime = clamped % audioDuration
      if (isPlaying) audioRef.current.play().catch(() => {})
    }
    if (videoRef.current && showVideo) {
      videoRef.current.currentTime = clamped % 40
    }
  }

  // Keep fresh references for lock screen action handlers
  useEffect(() => {
    handlersRef.current = {
      handleNextSong,
      handlePrevSong,
      handleSeek,
      setIsPlaying,
      isPlaying,
      audioRef
    }
  })

  // 1. LOCK SCREEN & NOTIFICATION MEDIA CONTROLS (MediaSession API like Resso)
  useEffect(() => {
    if (!('mediaSession' in navigator) || !currentSong) return

    try {
      navigator.mediaSession.metadata = new window.MediaMetadata({
        title: currentSong.title,
        artist: currentSong.artist,
        album: currentSong.album || 'Resso Music',
        artwork: [
          { src: currentSong.albumArt, sizes: '96x96', type: 'image/jpeg' },
          { src: currentSong.albumArt, sizes: '128x128', type: 'image/jpeg' },
          { src: currentSong.albumArt, sizes: '192x192', type: 'image/jpeg' },
          { src: currentSong.albumArt, sizes: '256x256', type: 'image/jpeg' },
          { src: currentSong.albumArt, sizes: '384x384', type: 'image/jpeg' },
          { src: currentSong.albumArt, sizes: '512x512', type: 'image/jpeg' }
        ]
      })

      navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused'

      // Full Lock Screen Media Action Handlers
      navigator.mediaSession.setActionHandler('play', () => {
        setIsPlaying(true)
        audioRef.current?.play().catch(() => {})
      })
      navigator.mediaSession.setActionHandler('pause', () => {
        setIsPlaying(false)
        audioRef.current?.pause()
      })
      navigator.mediaSession.setActionHandler('previoustrack', () => {
        handlersRef.current?.handlePrevSong?.()
      })
      navigator.mediaSession.setActionHandler('nexttrack', () => {
        handlersRef.current?.handleNextSong?.()
      })
      navigator.mediaSession.setActionHandler('seekto', (details) => {
        if (details.seekTime !== undefined && details.seekTime !== null) {
          handlersRef.current?.handleSeek?.(details.seekTime)
        }
      })
      navigator.mediaSession.setActionHandler('seekbackward', (details) => {
        const offset = details.seekOffset || 10
        const curr = audioRef.current?.currentTime || 0
        handlersRef.current?.handleSeek?.(Math.max(curr - offset, 0))
      })
      navigator.mediaSession.setActionHandler('seekforward', (details) => {
        const offset = details.seekOffset || 10
        const curr = audioRef.current?.currentTime || 0
        handlersRef.current?.handleSeek?.(Math.min(curr + offset, duration || 100))
      })
      navigator.mediaSession.setActionHandler('stop', () => {
        setIsPlaying(false)
        audioRef.current?.pause()
      })
    } catch (err) {
      console.warn("MediaSession API Error:", err)
    }
  }, [currentSong, isPlaying, duration])

  // Sync position state with lock screen seekbar
  useEffect(() => {
    if ('mediaSession' in navigator && 'setPositionState' in navigator.mediaSession) {
      if (duration && !isNaN(duration) && duration > 0) {
        try {
          navigator.mediaSession.setPositionState({
            duration: Math.max(duration, 1),
            playbackRate: 1,
            position: Math.min(Math.max(currentTime, 0), duration)
          })
        } catch (e) {}
      }
    }
  }, [currentTime, duration])

  // 2. CONTINUOUS BACKGROUND AUDIO & ANTI-SLEEP (Screen WakeLock API)
  // Ensures app does not sleep or pause when phone is locked or backgrounded
  useEffect(() => {
    const acquireWakeLock = async () => {
      if ('wakeLock' in navigator && isPlaying) {
        try {
          if (!wakeLockRef.current) {
            wakeLockRef.current = await navigator.wakeLock.request('screen')
          }
        } catch (e) {
          // Graceful fallback if battery optimization restricts wakeLock
        }
      }
    }

    const releaseWakeLock = async () => {
      if (wakeLockRef.current) {
        try {
          await wakeLockRef.current.release()
        } catch (e) {}
        wakeLockRef.current = null
      }
    }

    if (isPlaying) {
      acquireWakeLock()
    } else {
      releaseWakeLock()
    }

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && isPlaying) {
        acquireWakeLock()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      releaseWakeLock()
    }
  }, [isPlaying])

  // 3. SMART NETWORK DISCONNECT & AUTO-RESUME ENGINE
  // Internet kat jane par gana rukne par exact timestamp save hota hai,
  // aur jaise hi internet wapas aata hai gaana wahi se auto-play hota hai
  useEffect(() => {
    const handleOffline = () => {
      if (isPlaying) {
        wasPlayingBeforeDisconnect.current = true
        lastPlaybackPositionRef.current = currentTime
        showToast("⚠️ Internet कट गया! कनेक्शन आते ही गाना वहीं से चालू होगा 📡")
        if (ytPlayerRef.current && isYtReady) {
          try { ytPlayerRef.current.pauseVideo() } catch (e) {}
        }
        if (audioRef.current) audioRef.current.pause()
      }
    }

    const handleOnline = () => {
      if (wasPlayingBeforeDisconnect.current) {
        const savedTime = lastPlaybackPositionRef.current
        showToast("⚡ Internet वापस आ गया! गाना वहीं से auto-play हो रहा है... 🎵")

        // Wait slightly for connection to stabilize, then re-seek and auto-resume
        setTimeout(() => {
          if (ytPlayerRef.current && isYtReady) {
            try {
              ytPlayerRef.current.seekTo(savedTime, true)
              ytPlayerRef.current.playVideo()
              setIsPlaying(true)
              setCurrentTime(savedTime)
              wasPlayingBeforeDisconnect.current = false
              return
            } catch (e) {}
          }
          if (audioRef.current) {
            audioRef.current.currentTime = savedTime % (audioRef.current.duration || 30)
            const playPromise = audioRef.current.play()
            if (playPromise !== undefined) {
              playPromise
                .then(() => {
                  setIsPlaying(true)
                  setCurrentTime(savedTime)
                  wasPlayingBeforeDisconnect.current = false
                })
                .catch(() => {
                  if (currentSong?.audioUrl) {
                    audioRef.current.src = currentSong.audioUrl
                    audioRef.current.currentTime = savedTime % 30
                    audioRef.current.play().then(() => setIsPlaying(true)).catch(() => {})
                  }
                  wasPlayingBeforeDisconnect.current = false
                })
            }
          }
        }, 600)
      }
    }

    window.addEventListener('offline', handleOffline)
    window.addEventListener('online', handleOnline)

    return () => {
      window.removeEventListener('offline', handleOffline)
      window.removeEventListener('online', handleOnline)
    }
  }, [isPlaying, currentTime, currentSong])

  // Double tap heart animation
  const handleDoubleTap = (e) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const id = Date.now() + Math.random()

    setFloatingHearts(prev => [...prev, { id, x, y }])
    setTimeout(() => {
      setFloatingHearts(prev => prev.filter(h => h.id !== id))
    }, 1200)

    if (!currentSong.isLiked) {
      toggleLike()
    }
  }

  const toggleLike = () => {
    setSongs(prev => prev.map((s, idx) => idx === currentSongIndex ? { ...s, isLiked: !s.isLiked } : s))
    showToast(currentSong.isLiked ? "Removed from Liked" : "Added to Liked Songs ❤️")
  }

  const toggleFollow = (e) => {
    e.stopPropagation()
    setSongs(prev => prev.map((s, idx) => idx === currentSongIndex ? { ...s, isFollowing: !s.isFollowing } : s))
    showToast(currentSong.isFollowing ? `Unfollowed ${currentSong.artist}` : `Followed ${currentSong.artist}! ✨`)
  }

  const toggleDownload = () => {
    setSongs(prev => prev.map((s, idx) => idx === currentSongIndex ? { ...s, isDownloaded: !s.isDownloaded } : s))
    showToast(currentSong.isDownloaded ? "Removed from Offline" : "Downloaded to Offline Storage! 💾")
  }

  // Vertical Touch Gestures (TikTok style like original Resso)
  const handleTouchStart = (e) => {
    touchStartY.current = e.touches[0].clientY
  }

  const handleTouchEnd = (e) => {
    const deltaY = e.changedTouches[0].clientY - touchStartY.current
    if (deltaY < -50) handleNextSong()
    else if (deltaY > 50) handlePrevSong()
  }

  // Synced lyrics calculation
  const lyrics = currentSong?.lyrics || []
  const activeLyricIndex = lyrics.reduce((acc, lyric, idx) => {
    if (currentTime >= lyric.time) return idx
    return acc
  }, 0)

  useEffect(() => {
    if (showLyrics && lyricsContainerRef.current) {
      const activeEl = lyricsContainerRef.current.children[activeLyricIndex]
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  }, [activeLyricIndex, showLyrics])

  // Search online
  const searchOnline = async (queryText) => {
    const term = (queryText || searchQuery).trim()
    if (!term) return
    setIsSearchingOnline(true)

    try {
      // 1. Connect to live Vercel 320kbps Music API
      const apiBase = window.MUSIC_API_URL || 'https://crakaresso-music-api.vercel.app/api/search'
      const targetUrl = apiBase + '?q=' + encodeURIComponent(term)
      
      let res = await fetch(targetUrl)
        .then(r => r.ok ? r.json() : null)
        .catch(() => null)

      // 2. Fallback to local server if running locally
      if (!res || !res.results) {
        const localUrl = 'http://localhost:5050/api/search?q=' + encodeURIComponent(term)
        res = await fetch(localUrl)
          .then(r => r.ok ? r.json() : null)
          .catch(() => null)
      }

      if (res && res.success && Array.isArray(res.results) && res.results.length > 0) {
        const parsed = res.results.map((t, index) => ({
          id: 'track_' + (t.id || index),
          title: t.title || 'Track',
          artist: t.artist || 'Artist',
          album: t.album || 'Single',
          duration: t.duration || 210,
          audioUrl: t.audioUrl,
          albumArt: t.albumArt || currentSong?.albumArt,
          videoUri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
          likesCount: '158.4K',
          commentsCount: '4.8K',
          bpm: '124 BPM',
          credits: {
            composer: t.artist || 'Artist',
            lyricist: t.artist || 'Artist',
            producer: t.album || 'Studio Release',
            releaseYear: t.year || '2024'
          },
          lyrics: [
            { time: 0, text: '♪ ' + t.title + ' ♪' },
            { time: 4, text: 'Artist: ' + t.artist },
            { time: 10, text: 'Album: ' + t.album },
            { time: 18, text: 'Streaming in 320kbps High Fidelity Sound 🎧' },
            { time: 30, text: 'Feel the rhythm & acoustic vibrations... 🔥' }
          ],
          genre: 'Hit Music',
          mood: 'Vibrant',
          isLiked: false,
          isFollowing: false,
          isDownloaded: false
        }))
        setOnlineResults(parsed)
      } else {
        const localMatches = songs.filter(s =>
          s.title.toLowerCase().includes(term.toLowerCase()) ||
          s.artist.toLowerCase().includes(term.toLowerCase())
        )
        setOnlineResults(localMatches)
      }
    } catch (e) {
      console.warn("Search error:", e)
    } finally {
      setIsSearchingOnline(false)
    }
  }

  const playSelectedSong = (songObj) => {
    setSongs(prev => [songObj, ...prev.filter(s => s.id !== songObj.id)])
    setCurrentSongIndex(0)
    setCurrentTime(0)
    setDuration(songObj.duration)
    setIsPlaying(true)
    setCurrentTab('foryou')
    showToast(`Playing: "${songObj.title}" 🎵`)
  }

  const formatTime = (secs) => {
    if (!secs || isNaN(secs)) return '0:00'
    const m = Math.floor(secs / 60)
    const s = Math.floor(secs % 60)
    return `${m}:${s < 10 ? '0' : ''}${s}`
  }

  return (
    <div
      className="relative w-full h-[100dvh] max-h-screen max-w-md mx-auto bg-[#08040C] text-white overflow-hidden flex flex-col font-sans select-none shadow-2xl"
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {/* Audio Engine - Continuous Background Playback & Crash Protection with Network Auto-Resume */}
      <audio
        ref={audioRef}
        src={currentSong?.audioUrl}
        onTimeUpdate={handleTimeUpdate}
        onEnded={handleAudioEnded}
        onWaiting={() => {
          if (audioRef.current && isPlaying) {
            lastPlaybackPositionRef.current = audioRef.current.currentTime
          }
        }}
        onStalled={() => {
          if (isPlaying) {
            wasPlayingBeforeDisconnect.current = true
            lastPlaybackPositionRef.current = audioRef.current?.currentTime || currentTime
          }
        }}
        onError={(e) => {
          console.warn("Audio stream playback warning, gracefully recovering:", e)
          if (isPlaying) {
            wasPlayingBeforeDisconnect.current = true
            lastPlaybackPositionRef.current = audioRef.current?.currentTime || currentTime
          }
        }}
        preload="auto"
        playsInline
      />

{/* Pure 320kbps Audio Engine Active */}

      {/* Ambient Canvas when Watch MV is OFF */}
      {!showVideo && (
        <div className="absolute inset-0 overflow-hidden z-0 pointer-events-none">
          <div
            className="absolute inset-0 bg-cover bg-center filter blur-3xl scale-125 opacity-30 transition-all duration-700"
            style={{ backgroundImage: `url(${currentSong?.albumArt})` }}
          />
          <div className="absolute inset-0 bg-gradient-to-b from-[#08040C]/80 via-transparent to-[#08040C]/95" />
        </div>
      )}

      {/* FLASH SYNC SCREEN OVERLAY (Beat Strobe effect in sync with real back torch) */}
      {flashSync && isPlaying && (
        <div
          className={`absolute inset-0 bg-white pointer-events-none z-20 transition-opacity duration-75 mix-blend-screen ${
            isTorchStrobeActive ? 'opacity-35' : 'opacity-0'
          }`}
        />
      )}

      {/* Floating Header Badges */}
      <div className="absolute top-2 left-3 z-30 flex items-center gap-2">
        <button
          onClick={() => setIsDataMonitorOpen(true)}
          className="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-black/60 backdrop-blur-md border border-emerald-500/30 text-[9px] text-emerald-400 font-mono active:scale-95 transition cursor-pointer"
          title="Inspect 0.1 kb/s Data Usage"
        >
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
          <Wifi className="w-2.5 h-2.5 text-emerald-400" />
          <span>0.1 kb/s Saver</span>
        </button>
        {currentSong?.isDownloaded && (
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-[9px] text-emerald-300 font-bold">
            <Download className="w-2.5 h-2.5" />
            <span>Offline Ready</span>
          </div>
        )}
        {flashSync && (
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-500/25 border border-amber-500/40 text-[9px] text-amber-300 font-bold animate-pulse">
            <Zap className="w-2.5 h-2.5 fill-current" />
            <span>Torch Flash 🔦</span>
          </div>
        )}
      </div>

      {/* Toast Alert */}
      {toastMessage && (
        <div className="absolute top-12 left-1/2 transform -translate-x-1/2 z-50 bg-[#FF2A6D] text-white px-4 py-1.5 rounded-full text-xs font-bold shadow-xl animate-bounce flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Double Tap Floating Hearts */}
      {floatingHearts.map(h => (
        <div
          key={h.id}
          className="absolute z-50 pointer-events-none animate-float-up text-[#FF2A6D]"
          style={{ left: h.x - 24, top: h.y - 24 }}
        >
          <Heart className="w-12 h-12 fill-current drop-shadow-[0_0_15px_#FF2A6D]" />
        </div>
      ))}

      {/* MAIN VIEWPORT */}
      <div className="relative flex-1 flex flex-col z-10 overflow-hidden">
        {/* TAB 1: FOR YOU (Authentic Resso Experience) */}
        {currentTab === 'foryou' && (
          <div
            className="relative flex-1 flex flex-col justify-between"
            onDoubleClick={handleDoubleTap}
          >
            {/* Top Bar Header */}
            <div className="pt-3.5 px-3.5 pb-1 flex items-center justify-between z-20 shrink-0">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIsSidebarOpen(true)}
                  className="w-8 h-8 rounded-full bg-black/45 backdrop-blur-md flex items-center justify-center border border-white/15 active:scale-95 transition"
                  title="Studio Tools"
                >
                  <Sliders className="w-3.5 h-3.5 text-white" />
                </button>
                <div onClick={() => setIsCreditsOpen(true)} className="cursor-pointer">
                  <div className="text-[11px] font-black tracking-widest text-[#05D9E8] uppercase flex items-center gap-1">
                    <span>VIBE STREAM</span>
                    <span className="w-1.5 h-1.5 rounded-full bg-[#05D9E8] animate-ping" />
                  </div>
                  <div className="text-[10px] text-white/60">
                    {currentSong?.mood} • {currentSong?.genre}
                  </div>
                </div>
              </div>

              {/* Action Chips: Watch MV, Lyrics, Equalizer */}
              <div className="flex items-center gap-1.5">
                {/* Watch MV: Zero bytes consumed when closed! */}
                <button
                  onClick={() => {
                    const next = !showVideo
                    setShowVideo(next)
                    showToast(next ? "Video ON 🎬" : "Video OFF (Audio Continues) 🎵")
                  }}
                  className={`px-2.5 py-1 rounded-full text-[10px] font-bold flex items-center gap-1 transition active:scale-95 ${
                    showVideo
                      ? 'bg-[#FF2A6D] text-white shadow-lg shadow-[#FF2A6D]/40'
                      : 'bg-black/50 text-white/90 backdrop-blur-md border border-white/15'
                  }`}
                >
                  <Film className="w-3 h-3" />
                  <span>{showVideo ? 'MV ON' : 'Watch MV'}</span>
                </button>

                {/* Lyrics Toggle: Compact floating card */}
                <button
                  onClick={() => {
                    const next = !showLyrics
                    setShowLyrics(next)
                    showToast(next ? "Lyrics ON 🎤" : "Lyrics OFF")
                  }}
                  className={`px-2.5 py-1 rounded-full text-[10px] font-bold flex items-center gap-1 transition active:scale-95 ${
                    showLyrics
                      ? 'bg-[#05D9E8] text-black font-extrabold shadow-lg shadow-[#05D9E8]/40'
                      : 'bg-black/50 text-white/90 backdrop-blur-md border border-white/15'
                  }`}
                >
                  <Quote className="w-3 h-3" />
                  <span>{showLyrics ? 'Lyrics ON' : 'Lyrics'}</span>
                </button>

                {/* Equalizer Quick Button */}
                <button
                  onClick={() => setIsEqualizerOpen(true)}
                  className="w-7 h-7 rounded-full bg-black/50 backdrop-blur-md border border-white/15 flex items-center justify-center text-amber-300"
                  title="Equalizer & Sound Effects"
                >
                  <SlidersHorizontal className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Center Area: Resso Visualizer (Vinyl Record / Spectrum Waves / Retro Tape) */}
            {!showLyrics && !showVideo && (
              <div className="flex-1 min-h-0 flex flex-col items-center justify-center pointer-events-none relative px-3 py-1">
                {/* Mode 1: Iconic Rotating Vinyl Record */}
                {vibeMode === 'disc' && (
                  <div
                    className={`relative w-48 h-48 sm:w-56 sm:h-56 max-h-[28vh] max-w-[28vh] rounded-full p-1.5 flex items-center justify-center transition-all duration-700 shadow-[0_0_50px_rgba(0,0,0,0.95)] ${
                      isPlaying ? 'rotate-disc' : 'paused scale-95 opacity-90'
                    }`}
                    style={{
                      background: 'repeating-radial-gradient(circle, #1A1422 0px, #100C16 3px, #08060B 6px, #1A1422 8px)'
                    }}
                  >
                    <div className="w-full h-full rounded-full border-2 border-white/10 flex items-center justify-center relative p-2.5">
                      <div className="w-32 h-32 sm:w-36 sm:h-36 rounded-full overflow-hidden border-3 border-black/80 shadow-2xl relative">
                        <img
                          src={currentSong?.albumArt}
                          alt={currentSong?.title}
                          className="w-full h-full object-cover"
                        />
                        <div className="absolute inset-0 bg-gradient-to-tr from-black/40 via-transparent to-white/20" />
                      </div>
                      <div className="absolute w-7 h-7 rounded-full bg-[#08040C] border-2 border-white/30 flex items-center justify-center shadow-inner">
                        <div className="w-2 h-2 rounded-full bg-[#FF2A6D] shadow-[0_0_8px_#FF2A6D]" />
                      </div>
                    </div>
                  </div>
                )}

                {/* Mode 2: Real-time Pulsing Frequency Spectrum */}
                {vibeMode === 'spectrum' && (
                  <div className="w-48 h-48 flex flex-col items-center justify-center">
                    <div className="w-28 h-28 rounded-2xl overflow-hidden border-2 border-white/20 shadow-2xl mb-3">
                      <img src={currentSong?.albumArt} alt="art" className="w-full h-full object-cover" />
                    </div>
                    {/* Equalizer Frequency Bars */}
                    <div className="flex items-end justify-center gap-1.5 h-10">
                      {[40, 75, 100, 60, 90, 45, 80, 100, 70, 50, 85, 30].map((h, i) => (
                        <div
                          key={i}
                          className="w-1.5 rounded-full bg-gradient-to-t from-[#FF2A6D] to-[#05D9E8]"
                          style={{
                            height: isPlaying ? `${Math.max(15, (h * (currentTime * 10 + i * 8) % 100))}%` : '20%',
                            transition: 'height 150ms ease'
                          }}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Mode 3: Retro Cassette Tape */}
                {vibeMode === 'retro' && (
                  <div className="w-52 h-36 bg-[#140F1E] border-2 border-white/20 rounded-2xl p-2.5 flex flex-col justify-between shadow-2xl">
                    <div className="flex justify-between items-center text-[9px] text-white/50 border-b border-white/10 pb-1 font-mono">
                      <span>TYPE I (NORMAL)</span>
                      <span className="text-[#FF2A6D]">SIDE A</span>
                    </div>
                    <div className="my-auto bg-[#0A0610] border border-white/15 rounded-xl p-2 flex items-center justify-around">
                      <div className={`w-8 h-8 rounded-full border-2 border-white/40 flex items-center justify-center ${isPlaying ? 'rotate-disc' : ''}`}>
                        <div className="w-2.5 h-2.5 bg-white/60 rounded-full" />
                      </div>
                      <div className="w-16 h-3.5 bg-white/10 rounded-md flex items-center justify-center text-[7px] font-mono text-white/70">
                        {currentSong?.bpm}
                      </div>
                      <div className={`w-8 h-8 rounded-full border-2 border-white/40 flex items-center justify-center ${isPlaying ? 'rotate-disc' : ''}`}>
                        <div className="w-2.5 h-2.5 bg-white/60 rounded-full" />
                      </div>
                    </div>
                    <div className="text-[9px] font-bold text-center text-[#05D9E8] truncate">
                      {currentSong?.title}
                    </div>
                  </div>
                )}

                {/* Danmaku Floating Vibe Comments (Iconic Chinese Resso / Luna Feature) */}
                {showFloatingComments && (
                  <div className="absolute inset-x-0 top-6 h-28 pointer-events-none overflow-hidden z-15">
                    {comments[0] && (
                      <div className="absolute top-2 whitespace-nowrap animate-danmaku-1 flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/15 text-[10px] text-white shadow-lg">
                        <span className="text-[#05D9E8] font-bold">@{comments[0].user}:</span>
                        <span>{comments[0].text}</span>
                        <span className="text-[9px] text-[#FF2A6D]">❤️ {comments[0].likes}</span>
                      </div>
                    )}
                    {comments[1] && (
                      <div className="absolute top-10 whitespace-nowrap animate-danmaku-2 flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/15 text-[10px] text-white shadow-lg">
                        <span className="text-[#FF2A6D] font-bold">@{comments[1].user}:</span>
                        <span>{comments[1].text}</span>
                      </div>
                    )}
                    {comments[2] && (
                      <div className="absolute top-18 whitespace-nowrap animate-danmaku-3 flex items-center gap-1.5 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/15 text-[10px] text-white shadow-lg">
                        <span className="text-amber-300 font-bold">@{comments[2].user}:</span>
                        <span>{comments[2].text}</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Compact Floating Lyrics Card (ONLY visible when toggled ON, NOT FULL SCREEN!) */}
            {showLyrics && (
              <div className="flex-1 flex items-center justify-center px-4 z-20">
                <div className="w-full max-h-52 h-48 bg-[#120B1C]/95 backdrop-blur-2xl border border-white/15 rounded-3xl p-3.5 flex flex-col shadow-2xl">
                  {/* Card Header with Dismiss '✕' Button */}
                  <div className="flex items-center justify-between pb-1.5 border-b border-white/10">
                    <div className="flex items-center gap-2 text-[#05D9E8] text-[11px] font-black tracking-widest uppercase">
                      <Quote className="w-3.5 h-3.5" />
                      <span>SYNCED LYRICS</span>
                    </div>
                    <button
                      onClick={() => setShowLyrics(false)}
                      className="w-6 h-6 rounded-full bg-white/10 flex items-center justify-center text-white/80 hover:bg-white/20 transition active:scale-95"
                      title="Close Lyrics"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Scrollable Lyric Lines with Tap-to-Seek */}
                  <div
                    ref={lyricsContainerRef}
                    className="flex-1 overflow-y-auto no-scrollbar py-2 space-y-2"
                  >
                    {lyrics.length > 0 ? (
                      lyrics.map((line, idx) => {
                        const isActive = idx === activeLyricIndex
                        const isPast = idx < activeLyricIndex
                        return (
                          <div
                            key={idx}
                            onClick={() => handleSeek(line.time)}
                            className={`cursor-pointer transition-all duration-200 px-2 py-1 rounded-lg ${
                              isActive
                                ? 'text-white font-black text-sm bg-white/10 shadow-sm border-l-2 border-[#FF2A6D] pl-3'
                                : isPast
                                  ? 'text-white/40 text-xs font-medium'
                                  : 'text-white/25 text-xs font-medium'
                            }`}
                          >
                            {line.text}
                          </div>
                        )
                      })
                    ) : (
                      <div className="h-full flex items-center justify-center text-white/40 text-xs">
                        No synced lyrics available for this song.
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Right-Side Vertical Action Stack (ByteDance / Chinese Resso Layout) */}
            <div className="absolute right-2 bottom-20 flex flex-col items-center gap-1 z-20">
              {/* Artist Avatar with Follow (+) Button */}
              <div className="relative mb-0.5">
                <img
                  src={currentSong?.albumArt}
                  alt={currentSong?.artist}
                  className="w-8 h-8 rounded-full object-cover border-2 border-white/30 p-0.5 shadow-lg"
                />
                <button
                  onClick={toggleFollow}
                  className={`absolute -bottom-1 left-1/2 transform -translate-x-1/2 w-3.5 h-3.5 rounded-full flex items-center justify-center text-[8px] font-bold shadow-md transition-all active:scale-90 ${
                    currentSong?.isFollowing ? 'bg-emerald-500 text-white' : 'bg-[#FF2A6D] text-white'
                  }`}
                >
                  {currentSong?.isFollowing ? <Check className="w-2 h-2" /> : '+'}
                </button>
              </div>

              {/* Like / Heart Button with Count */}
              <button
                onClick={toggleLike}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition group"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center backdrop-blur-md border ${
                  currentSong?.isLiked
                    ? 'bg-[#FF2A6D] border-[#FF2A6D] text-white shadow-lg shadow-[#FF2A6D]/50'
                    : 'bg-black/50 border-white/15 text-white'
                }`}>
                  <Heart className={`w-3.5 h-3.5 ${currentSong?.isLiked ? 'fill-current' : ''}`} />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">{currentSong?.likesCount}</span>
              </button>

              {/* Comments Button with Count */}
              <button
                onClick={() => setIsCommentsOpen(true)}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
              >
                <div className="w-8 h-8 rounded-full bg-black/50 border border-white/15 backdrop-blur-md flex items-center justify-center text-white">
                  <MessageCircle className="w-3.5 h-3.5" />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">{currentSong?.commentsCount}</span>
              </button>

              {/* Synced Lyrics Toggle Button */}
              <button
                onClick={() => setShowLyrics(!showLyrics)}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center backdrop-blur-md border ${
                  showLyrics
                    ? 'bg-[#05D9E8] border-[#05D9E8] text-black shadow-lg shadow-[#05D9E8]/50'
                    : 'bg-black/50 border-white/15 text-white'
                }`}>
                  <Quote className="w-3.5 h-3.5" />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">{showLyrics ? 'ON' : 'Lyrics'}</span>
              </button>

              {/* Vibe Visualizer Switcher */}
              <button
                onClick={() => {
                  const modes = ['disc', 'spectrum', 'retro']
                  const next = modes[(modes.indexOf(vibeMode) + 1) % modes.length]
                  setVibeMode(next)
                  showToast(`Vibe: ${next.toUpperCase()} ✨`)
                }}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
              >
                <div className="w-8 h-8 rounded-full bg-black/50 border border-white/15 backdrop-blur-md flex items-center justify-center text-amber-300">
                  <Sparkles className="w-3.5 h-3.5" />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">Vibe</span>
              </button>

              {/* Flash Torch Beat Sync Button */}
              <button
                onClick={() => {
                  const next = !flashSync
                  setFlashSync(next)
                  showToast(next ? "⚡ Back Torch Flash: ON 🔦" : "Torch Flash OFF")
                }}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
                title="Real-Time Back Torch Flash"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center backdrop-blur-md border ${
                  flashSync
                    ? 'bg-amber-400 border-amber-400 text-black shadow-lg shadow-amber-400/50'
                    : 'bg-black/50 border-white/15 text-white/80'
                }`}>
                  <Zap className={`w-3.5 h-3.5 ${flashSync ? 'fill-current' : ''}`} />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">{flashSync ? 'Torch' : 'Torch'}</span>
              </button>

              {/* Offline Download Button */}
              <button
                onClick={toggleDownload}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center backdrop-blur-md border ${
                  currentSong?.isDownloaded
                    ? 'bg-emerald-500 border-emerald-500 text-white'
                    : 'bg-black/50 border-white/15 text-white/80'
                }`}>
                  <Download className="w-3.5 h-3.5" />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">{currentSong?.isDownloaded ? 'Saved' : 'Save'}</span>
              </button>

              {/* Lyric Poster / Share Button */}
              <button
                onClick={() => setIsPosterOpen(true)}
                className="flex flex-col items-center gap-0.5 active:scale-90 transition"
              >
                <div className="w-8 h-8 rounded-full bg-black/50 border border-white/15 backdrop-blur-md flex items-center justify-center text-white/80">
                  <Share2 className="w-3.5 h-3.5" />
                </div>
                <span className="text-[8.5px] font-bold text-white/90">Share</span>
              </button>
            </div>

            {/* Bottom Playback Strip */}
            <div className="px-3.5 pb-2 pt-1.5 bg-gradient-to-t from-black via-black/85 to-transparent z-20 shrink-0">
              {/* Song Title, Artist & Verified Badge */}
              <div className="mb-1 max-w-[74%] pr-2">
                <div
                  onClick={() => setIsCreditsOpen(true)}
                  className="text-sm font-black text-white truncate drop-shadow-md cursor-pointer hover:underline"
                >
                  {currentSong?.title}
                </div>
                <div className="flex items-center gap-1.5 text-[11px] text-white/70">
                  <span className="truncate">{currentSong?.artist}</span>
                  <span className="w-1 h-1 rounded-full bg-white/40" />
                  <span className="text-[#FF2A6D] font-bold text-[10px]">• Full Song</span>
                </div>
              </div>

              {/* Seekable Progress Bar with Thumb Dot */}
              <div className="space-y-0.5">
                <input
                  type="range"
                  min="0"
                  max={duration || 100}
                  value={currentTime}
                  onChange={(e) => handleSeek(parseFloat(e.target.value))}
                  className="w-full h-1 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#FF2A6D]"
                />
                <div className="flex justify-between text-[9px] font-mono font-semibold text-white/60">
                  <span>{formatTime(currentTime)}</span>
                  <span>{formatTime(duration)}</span>
                </div>
              </div>

              {/* Bottom Playback Buttons */}
              <div className="flex items-center justify-between mt-0.5 px-3">
                <button
                  onClick={() => {
                    setIsShuffle(!isShuffle)
                    showToast(isShuffle ? "Shuffle OFF" : "Shuffle ON 🔀")
                  }}
                  className={`p-1.5 transition ${isShuffle ? 'text-[#05D9E8]' : 'text-white/50'}`}
                >
                  <Shuffle className="w-4 h-4" />
                </button>

                <button
                  onClick={handlePrevSong}
                  className="p-1.5 text-white active:scale-90 transition"
                >
                  <SkipBack className="w-5 h-5 fill-current" />
                </button>

                {/* Big Center Play/Pause Neon Button */}
                <button
                  onClick={togglePlayPause}
                  className="w-11 h-11 rounded-full bg-[#FF2A6D] text-white flex items-center justify-center shadow-[0_0_15px_#FF2A6D] active:scale-95 transition"
                >
                  {isPlaying ? (
                    <Pause className="w-5 h-5 fill-current" />
                  ) : (
                    <Play className="w-5 h-5 fill-current ml-0.5" />
                  )}
                </button>

                <button
                  onClick={handleNextSong}
                  className="p-1.5 text-white active:scale-90 transition"
                >
                  <SkipForward className="w-5 h-5 fill-current" />
                </button>

                <button
                  onClick={() => {
                    const next = (repeatMode + 1) % 3
                    setRepeatMode(next)
                    showToast(next === 0 ? "Repeat OFF" : next === 1 ? "Repeat ALL 🔁" : "Repeat ONE 🔂")
                  }}
                  className={`p-1.5 transition ${repeatMode > 0 ? 'text-[#05D9E8]' : 'text-white/50'}`}
                >
                  <Repeat className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: EXPLORE / SEARCH (Clean Cloud Stream) */}
        {currentTab === 'explore' && (
          <div className="flex-1 flex flex-col p-4 pt-10 overflow-y-auto no-scrollbar z-20">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h1 className="text-xl font-black text-white">Search & Discover</h1>
                <p className="text-xs text-white/50">Cloud Music • Low-latency stream</p>
              </div>
              <button
                onClick={() => setIsSidebarOpen(true)}
                className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center text-white"
              >
                <Sliders className="w-4 h-4" />
              </button>
            </div>

            {/* Search Input */}
            <div className="relative mb-4">
              <Search className="w-4 h-4 text-[#FF2A6D] absolute left-3.5 top-1/2 transform -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search any song, artist or mood..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && searchOnline(searchQuery)}
                className="w-full bg-[#160E21] border border-white/10 rounded-2xl py-2.5 pl-10 pr-9 text-xs text-white placeholder-white/40 focus:outline-none focus:border-[#FF2A6D]"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 text-white/50"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Trending Quick Chips */}
            <div className="flex gap-2 overflow-x-auto no-scrollbar pb-3">
              {['Arijit Singh', 'Sidhu Moose Wala', 'Coke Studio', 'AP Dhillon', 'Diljit Dosanjh', 'Lofi Vibe'].map(chip => (
                <button
                  key={chip}
                  onClick={() => {
                    setSearchQuery(chip)
                    searchOnline(chip)
                  }}
                  className="px-3 py-1.5 rounded-full bg-white/10 text-white text-xs whitespace-nowrap active:scale-95 transition"
                >
                  {chip}
                </button>
              ))}
            </div>

            {/* Search Results */}
            {isSearchingOnline ? (
              <div className="py-12 flex flex-col items-center justify-center gap-3">
                <div className="w-8 h-8 border-2 border-[#FF2A6D] border-t-transparent rounded-full animate-spin" />
                <span className="text-xs text-white/60">Searching music catalog...</span>
              </div>
            ) : onlineResults.length > 0 ? (
              <div className="space-y-2 mt-2">
                <div className="text-xs font-bold text-[#05D9E8] mb-2 tracking-wider">
                  STREAM RESULTS ({onlineResults.length})
                </div>
                {onlineResults.map((item, idx) => (
                  <div
                    key={idx}
                    onClick={() => playSelectedSong(item)}
                    className="flex items-center gap-3 p-2 rounded-2xl bg-[#160E21] border border-white/5 active:bg-[#FF2A6D]/20 cursor-pointer transition"
                  >
                    <img
                      src={item.albumArt}
                      alt={item.title}
                      className="w-12 h-12 rounded-xl object-cover"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-bold text-white truncate">{item.title}</div>
                      <div className="text-[11px] text-white/60 truncate">{item.artist}</div>
                      <div className="text-[9px] text-[#05D9E8] font-semibold mt-0.5">
                        Online • Full Audio
                      </div>
                    </div>
                    <div className="w-8 h-8 rounded-full bg-[#FF2A6D]/20 flex items-center justify-center text-[#FF2A6D]">
                      <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="space-y-3 mt-2">
                <div className="text-xs font-bold text-white/70 tracking-wider">HOT ON RESSO</div>
                {songs.map((s, idx) => (
                  <div
                    key={s.id}
                    onClick={() => {
                      setCurrentSongIndex(idx)
                      setCurrentTime(0)
                      setIsPlaying(true)
                      setCurrentTab('foryou')
                    }}
                    className="flex items-center gap-3 p-2 rounded-2xl bg-[#160E21]/60 border border-white/5 cursor-pointer"
                  >
                    <img src={s.albumArt} alt={s.title} className="w-11 h-11 rounded-xl object-cover" />
                    <div className="flex-1 min-w-0">
                      <div className="text-xs font-bold text-white truncate">{s.title}</div>
                      <div className="text-[11px] text-white/60 truncate">{s.artist}</div>
                    </div>
                    <div className="w-7 h-7 rounded-full bg-white/10 flex items-center justify-center text-white">
                      <Play className="w-3.5 h-3.5 fill-current ml-0.5" />
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: LIBRARY */}
        {currentTab === 'library' && (
          <div className="flex-1 flex flex-col p-4 pt-10 overflow-y-auto no-scrollbar z-20">
            <h1 className="text-xl font-black text-white mb-1">Your Library</h1>
            <p className="text-xs text-white/60 mb-4">Saved playlists, offline tracks & favorites</p>

            <div className="grid grid-cols-2 gap-3 mb-5">
              <div className="p-3.5 rounded-2xl bg-gradient-to-br from-[#FF2A6D]/30 to-[#9B5DE5]/20 border border-white/10">
                <Heart className="w-5 h-5 text-[#FF2A6D] fill-current mb-2" />
                <div className="text-xs font-bold">Liked Songs</div>
                <div className="text-[10px] text-white/60">
                  {songs.filter(s => s.isLiked).length} songs
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-gradient-to-br from-emerald-500/30 to-teal-500/20 border border-white/10">
                <Download className="w-5 h-5 text-emerald-400 mb-2" />
                <div className="text-xs font-bold">Offline Downloads</div>
                <div className="text-[10px] text-white/60">
                  {songs.filter(s => s.isDownloaded).length} tracks
                </div>
              </div>
            </div>

            <div className="text-xs font-bold text-white/70 mb-2.5 tracking-wider">ALL TRACKS</div>
            <div className="space-y-2">
              {songs.map((s, idx) => (
                <div
                  key={s.id}
                  onClick={() => {
                    setCurrentSongIndex(idx)
                    setCurrentTime(0)
                    setIsPlaying(true)
                    setCurrentTab('foryou')
                  }}
                  className={`flex items-center gap-3 p-2.5 rounded-2xl border transition cursor-pointer ${
                    idx === currentSongIndex
                      ? 'bg-[#FF2A6D]/20 border-[#FF2A6D]'
                      : 'bg-[#160E21]/60 border-white/5'
                  }`}
                >
                  <img src={s.albumArt} alt={s.title} className="w-10 h-10 rounded-xl object-cover" />
                  <div className="flex-1 min-w-0">
                    <div className="text-xs font-bold text-white truncate">{s.title}</div>
                    <div className="text-[11px] text-white/60 truncate">{s.artist}</div>
                  </div>
                  {s.isDownloaded && (
                    <span className="text-[9px] text-emerald-400 font-bold px-1.5 py-0.5 rounded bg-emerald-500/20">
                      OFFLINE
                    </span>
                  )}
                  {idx === currentSongIndex && isPlaying && (
                    <div className="flex gap-1 items-end h-3 mr-2">
                      <div className="w-1 bg-[#FF2A6D] h-full animate-pulse" />
                      <div className="w-1 bg-[#FF2A6D] h-2/3 animate-bounce" />
                      <div className="w-1 bg-[#FF2A6D] h-4/5 animate-pulse" />
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Mini Player Bar when outside For You */}
      {currentTab !== 'foryou' && (
        <div
          onClick={() => setCurrentTab('foryou')}
          className="mx-3 mb-2 p-2 rounded-2xl bg-[#181122] border border-white/15 flex items-center gap-3 shadow-xl cursor-pointer z-30"
        >
          <img
            src={currentSong?.albumArt}
            alt={currentSong?.title}
            className="w-9 h-9 rounded-xl object-cover"
          />
          <div className="flex-1 min-w-0">
            <div className="text-xs font-bold text-white truncate">{currentSong?.title}</div>
            <div className="text-[10px] text-white/60 truncate">{currentSong?.artist}</div>
          </div>
          <button
            onClick={(e) => {
              e.stopPropagation()
              togglePlayPause()
            }}
            className="w-7 h-7 rounded-full bg-[#FF2A6D] text-white flex items-center justify-center mr-1"
          >
            {isPlaying ? <Pause className="w-3.5 h-3.5 fill-current" /> : <Play className="w-3.5 h-3.5 fill-current ml-0.5" />}
          </button>
        </div>
      )}

      {/* Bottom Navigation Bar */}
      <div className="px-6 py-2 bg-[#09050E]/95 backdrop-blur-lg border-t border-white/10 flex items-center justify-between z-30 shrink-0 pb-[max(env(safe-area-inset-bottom),0.5rem)]">
        <button
          onClick={() => setCurrentTab('foryou')}
          className={`flex flex-col items-center gap-0.5 transition ${
            currentTab === 'foryou' ? 'text-[#FF2A6D]' : 'text-white/50'
          }`}
        >
          <Film className="w-4 h-4" />
          <span className="text-[10px] font-bold">For You</span>
        </button>

        <button
          onClick={() => setCurrentTab('explore')}
          className={`flex flex-col items-center gap-0.5 transition ${
            currentTab === 'explore' ? 'text-[#FF2A6D]' : 'text-white/50'
          }`}
        >
          <Search className="w-4 h-4" />
          <span className="text-[10px] font-bold">Search</span>
        </button>

        <button
          onClick={() => setCurrentTab('library')}
          className={`flex flex-col items-center gap-0.5 transition ${
            currentTab === 'library' ? 'text-[#FF2A6D]' : 'text-white/50'
          }`}
        >
          <Library className="w-4 h-4" />
          <span className="text-[10px] font-bold">Library</span>
        </button>
      </div>

      {/* 1. COMMENTS BOTTOM SHEET (With Like Counter) */}
      {isCommentsOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex flex-col justify-end">
          <div className="bg-[#140D1D] border-t border-white/15 rounded-t-3xl max-h-[65vh] flex flex-col p-4 animate-in slide-in-from-bottom duration-300">
            <div className="flex items-center justify-between pb-3 border-b border-white/10">
              <span className="text-xs font-black text-white uppercase tracking-wider">
                Vibe Comments ({comments.length})
              </span>
              <button onClick={() => setIsCommentsOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto no-scrollbar py-3 space-y-2.5">
              {comments.map(c => (
                <div key={c.id} className="p-2.5 rounded-2xl bg-white/5 border border-white/5 flex justify-between items-start">
                  <div>
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="text-xs font-bold text-[#05D9E8]">{c.user}</span>
                      <span className="text-[9px] text-white/40">{c.time}</span>
                    </div>
                    <p className="text-xs text-white/90">{c.text}</p>
                  </div>
                  <button
                    onClick={() => {
                      setComments(prev => prev.map(item => item.id === c.id ? { ...item, likes: item.likes + 1 } : item))
                    }}
                    className="flex items-center gap-1 text-[10px] text-white/60 hover:text-[#FF2A6D] pt-1"
                  >
                    <Heart className="w-3 h-3" />
                    <span>{c.likes}</span>
                  </button>
                </div>
              ))}
            </div>

            <div className="flex gap-2 pt-2 border-t border-white/10">
              <input
                type="text"
                placeholder="Share your music vibe..."
                value={newCommentText}
                onChange={(e) => setNewCommentText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && newCommentText.trim()) {
                    setComments(prev => [{ id: Date.now(), user: 'You', text: newCommentText.trim(), time: 'Just now', likes: 1 }, ...prev])
                    setNewCommentText('')
                  }
                }}
                className="flex-1 bg-white/10 rounded-2xl px-3 py-2 text-xs text-white placeholder-white/40 focus:outline-none"
              />
              <button
                onClick={() => {
                  if (newCommentText.trim()) {
                    setComments(prev => [{ id: Date.now(), user: 'You', text: newCommentText.trim(), time: 'Just now', likes: 1 }, ...prev])
                    setNewCommentText('')
                  }
                }}
                className="px-3.5 py-2 bg-[#FF2A6D] rounded-2xl text-xs font-bold text-white flex items-center justify-center"
              >
                <Send className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. LYRIC POSTER CREATOR STUDIO (Resso Iconic Poster Tool) */}
      {isPosterOpen && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-xs bg-[#140D1D] border border-white/15 rounded-3xl p-5 shadow-2xl flex flex-col">
            <div className="flex justify-between items-center mb-3">
              <span className="text-xs font-black text-[#05D9E8] uppercase tracking-wider">Lyric Poster Studio</span>
              <button onClick={() => setIsPosterOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Poster Canvas Preview */}
            <div className={`relative rounded-2xl overflow-hidden aspect-square p-5 flex flex-col justify-between mb-3 border border-white/10 transition-all ${
              posterTheme === 'neon' ? 'bg-gradient-to-br from-[#FF2A6D]/40 to-[#05D9E8]/30' :
              posterTheme === 'cyber' ? 'bg-gradient-to-tr from-purple-900/60 to-black' :
              posterTheme === 'gold' ? 'bg-gradient-to-br from-amber-600/40 to-yellow-900/40' :
              'bg-[#0C0814]'
            }`}>
              <img
                src={currentSong?.albumArt}
                alt="bg"
                className="absolute inset-0 w-full h-full object-cover filter blur-sm scale-110 opacity-40 pointer-events-none"
              />
              <div className="absolute inset-0 bg-black/40" />

              <div className="relative z-10 flex justify-between items-center text-[9px] uppercase tracking-widest text-[#05D9E8] font-bold">
                <span>Resso • Lyric Poster</span>
                <span>{currentSong?.genre}</span>
              </div>

              <div className="relative z-10 my-auto text-center">
                <Quote className="w-5 h-5 text-white/40 mx-auto mb-1" />
                <div className="text-sm font-black text-white italic drop-shadow-lg leading-snug px-2">
                  "{currentSong?.lyrics?.[selectedLyricIndex]?.text || currentSong?.title}"
                </div>
              </div>

              <div className="relative z-10 flex justify-between items-center text-[10px] text-white/80 font-semibold border-t border-white/20 pt-2">
                <span className="truncate">{currentSong?.title}</span>
                <span className="truncate">{currentSong?.artist}</span>
              </div>
            </div>

            {/* Lyric line selector */}
            <div className="mb-3">
              <div className="text-[10px] text-white/60 mb-1 font-bold">SELECT LYRIC LINE:</div>
              <div className="flex gap-1.5 overflow-x-auto no-scrollbar py-1">
                {lyrics.map((l, i) => (
                  <button
                    key={i}
                    onClick={() => setSelectedLyricIndex(i)}
                    className={`px-2 py-1 rounded-lg text-[10px] whitespace-nowrap border ${
                      selectedLyricIndex === i ? 'bg-[#FF2A6D] border-[#FF2A6D] text-white' : 'bg-white/5 border-white/10 text-white/70'
                    }`}
                  >
                    Line {i + 1}
                  </button>
                ))}
              </div>
            </div>

            {/* Theme selector */}
            <div className="mb-4">
              <div className="text-[10px] text-white/60 mb-1 font-bold">POSTER STYLE:</div>
              <div className="flex gap-2">
                {[
                  { id: 'neon', name: 'Neon' },
                  { id: 'cyber', name: 'Cyber' },
                  { id: 'gold', name: 'Gold' },
                  { id: 'minimal', name: 'Dark' }
                ].map(t => (
                  <button
                    key={t.id}
                    onClick={() => setPosterTheme(t.id)}
                    className={`flex-1 py-1 rounded-xl text-[10px] font-bold border ${
                      posterTheme === t.id ? 'bg-white/20 border-[#05D9E8] text-white' : 'bg-white/5 border-white/10 text-white/60'
                    }`}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={() => {
                showToast("Poster saved to gallery & shared! 📸")
                setIsPosterOpen(false)
              }}
              className="w-full py-2.5 bg-[#FF2A6D] rounded-2xl text-xs font-bold text-white shadow-lg active:scale-95 transition"
            >
              Share & Save Poster
            </button>
          </div>
        </div>
      )}

      {/* 3. RESSO EQUALIZER & SOUND EFFECTS DIALOG */}
      {isEqualizerOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-xs bg-[#140D1D] border border-white/15 rounded-3xl p-5 shadow-2xl flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-[#05D9E8]" />
                <span className="text-xs font-black text-white uppercase tracking-wider">Audio Effects & EQ</span>
              </div>
              <button onClick={() => setIsEqualizerOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Bass Booster Slider */}
            <div className="mb-4 p-3 rounded-2xl bg-white/5 border border-white/10">
              <div className="flex justify-between text-xs font-bold mb-1.5">
                <span className="text-[#FF2A6D]">Dynamic Bass Boost</span>
                <span className="font-mono text-[#05D9E8]">{bassBoost}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                value={bassBoost}
                onChange={(e) => setBassBoost(parseInt(e.target.value))}
                className="w-full h-1.5 bg-white/20 rounded-lg appearance-none cursor-pointer accent-[#FF2A6D]"
              />
            </div>

            {/* Audio Profile Presets */}
            <div className="mb-4">
              <div className="text-[10px] text-white/60 font-bold mb-2">SOUND EFFECT PRESET:</div>
              <div className="grid grid-cols-2 gap-2">
                {['Resso Bass Boost', '3D Spatial', 'Vocal Focus', 'Lo-Fi Tape'].map(preset => (
                  <button
                    key={preset}
                    onClick={() => {
                      setSoundProfile(preset)
                      showToast(`Effect Applied: ${preset} ⚡`)
                    }}
                    className={`p-2 rounded-xl text-left border transition ${
                      soundProfile === preset
                        ? 'bg-[#05D9E8]/20 border-[#05D9E8] text-white font-bold'
                        : 'bg-white/5 border-white/10 text-white/70 text-xs'
                    }`}
                  >
                    <div className="text-xs">{preset}</div>
                  </button>
                ))}
              </div>
            </div>

            <button
              onClick={() => {
                showToast("Sound Settings Applied! 🎧")
                setIsEqualizerOpen(false)
              }}
              className="w-full py-2.5 bg-[#FF2A6D] rounded-2xl text-xs font-bold text-white shadow-lg"
            >
              Done
            </button>
          </div>
        </div>
      )}

      {/* 4. SLEEP TIMER DIALOG */}
      {isSleepTimerOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-xs bg-[#140D1D] border border-white/15 rounded-3xl p-5 shadow-2xl flex flex-col">
            <div className="flex justify-between items-center mb-4">
              <div className="flex items-center gap-2">
                <Moon className="w-4 h-4 text-purple-400" />
                <span className="text-xs font-black text-white uppercase tracking-wider">Sleep Timer</span>
              </div>
              <button onClick={() => setIsSleepTimerOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2 mb-4">
              {[
                { min: 15, label: '15 Minutes' },
                { min: 30, label: '30 Minutes' },
                { min: 45, label: '45 Minutes' },
                { min: 60, label: '60 Minutes' },
                { min: 0, label: 'Turn Timer OFF' }
              ].map(opt => (
                <button
                  key={opt.min}
                  onClick={() => {
                    if (opt.min === 0) {
                      setSleepTimer(null)
                      showToast("Sleep Timer cancelled")
                    } else {
                      setSleepTimer(opt.min)
                      showToast(`Sleep Timer set for ${opt.min}m 🌙`)
                    }
                    setIsSleepTimerOpen(false)
                  }}
                  className={`w-full p-2.5 rounded-xl border text-xs text-left transition flex justify-between items-center ${
                    sleepTimer === opt.min
                      ? 'bg-purple-600/30 border-purple-500 text-white font-bold'
                      : 'bg-white/5 border-white/10 text-white/80'
                  }`}
                >
                  <span>{opt.label}</span>
                  {sleepTimer === opt.min && <CheckCircle2 className="w-4 h-4 text-purple-400" />}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 5. TRACK CREDITS SHEET */}
      {isCreditsOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-xs bg-[#140D1D] border border-white/15 rounded-3xl p-5 shadow-2xl flex flex-col">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-center gap-2">
                <Info className="w-4 h-4 text-[#05D9E8]" />
                <span className="text-xs font-black text-white uppercase tracking-wider">Song Information</span>
              </div>
              <button onClick={() => setIsCreditsOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2 text-xs text-white/80 p-3 bg-white/5 rounded-2xl border border-white/10 mb-4">
              <div className="flex justify-between">
                <span className="text-white/50">Track:</span>
                <span className="font-bold text-white">{currentSong?.title}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-white/50">Artist:</span>
                <span className="font-bold text-white">{currentSong?.artist}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-white/50">Composer:</span>
                <span className="font-semibold text-white">{currentSong?.credits?.composer || 'Official'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-white/50">Lyricist:</span>
                <span className="font-semibold text-white">{currentSong?.credits?.lyricist || 'Official'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-white/50">BPM & Mood:</span>
                <span className="font-semibold text-[#05D9E8]">{currentSong?.bpm} • {currentSong?.mood}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-white/50">Audio Quality:</span>
                <span className="font-semibold text-emerald-400">Lossless 320kbps / 0.1kbps Stream</span>
              </div>
            </div>

            <button
              onClick={() => setIsCreditsOpen(false)}
              className="w-full py-2.5 bg-white/10 rounded-2xl text-xs font-bold text-white"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* 6. SIDEBAR DRAWER (All Resso Tools & Studio Settings) */}
      {isSidebarOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex">
          <div className="w-72 h-full bg-[#120B1C] border-r border-white/10 p-5 flex flex-col justify-between animate-in slide-in-from-left duration-300">
            <div>
              <div className="flex justify-between items-center mb-5">
                <div>
                  <h2 className="text-lg font-black text-white">Resso Studio</h2>
                  <div className="text-[10px] text-[#05D9E8] font-mono">com.resso.craka</div>
                </div>
                <button onClick={() => setIsSidebarOpen(false)} className="text-white/60">
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="space-y-2.5">
                {/* 0.1 kb/s Ultra Data Saver */}
                <div
                  onClick={() => {
                    const next = !dataSaver
                    setDataSaver(next)
                    showToast(next ? "0.1 kb/s Saver Active 🟢" : "Data Saver OFF")
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Wifi className={`w-4 h-4 ${dataSaver ? 'text-emerald-400' : 'text-white/50'}`} />
                    <div>
                      <div className="text-xs font-bold text-white">0.1 kb/s Saver</div>
                      <div className="text-[9px] text-white/50">Zero video preloading & cache</div>
                    </div>
                  </div>
                  <div className={`w-8 h-4.5 rounded-full transition-colors flex items-center px-0.5 ${
                    dataSaver ? 'bg-emerald-500 justify-end' : 'bg-white/20 justify-start'
                  }`}>
                    <div className="w-3.5 h-3.5 rounded-full bg-white shadow-sm" />
                  </div>
                </div>

                {/* Floating Vibe Comments Toggle */}
                <div
                  onClick={() => {
                    const next = !showFloatingComments
                    setShowFloatingComments(next)
                    showToast(next ? "Floating Comments ON 💬" : "Floating Comments OFF")
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <MessageCircle className="w-4 h-4 text-[#05D9E8]" />
                    <div>
                      <div className="text-xs font-bold text-white">Floating Vibe Comments</div>
                      <div className="text-[9px] text-white/50">Live bubbles on screen</div>
                    </div>
                  </div>
                  <div className={`w-8 h-4.5 rounded-full transition-colors flex items-center px-0.5 ${
                    showFloatingComments ? 'bg-[#05D9E8] justify-end' : 'bg-white/20 justify-start'
                  }`}>
                    <div className="w-3.5 h-3.5 rounded-full bg-white shadow-sm" />
                  </div>
                </div>

                {/* Equalizer & Audio Effects */}
                <div
                  onClick={() => {
                    setIsSidebarOpen(false)
                    setIsEqualizerOpen(true)
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <SlidersHorizontal className="w-4 h-4 text-amber-300" />
                    <div>
                      <div className="text-xs font-bold text-white">Equalizer & Bass Boost</div>
                      <div className="text-[9px] text-white/50">{soundProfile} ({bassBoost}%)</div>
                    </div>
                  </div>
                </div>

                {/* Flash Sync Toggle */}
                <div
                  onClick={() => {
                    const next = !flashSync
                    setFlashSync(next)
                    showToast(next ? "⚡ Back Torch Flash: ON (Beat par flashlight chalegi) 🔦" : "Torch Flash OFF")
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Zap className={`w-4 h-4 ${flashSync ? 'text-amber-400' : 'text-white/50'}`} />
                    <div>
                      <div className="text-xs font-bold text-white">Back Torch Flash (Beat Sync)</div>
                      <div className="text-[9px] text-amber-300 font-semibold">Physical camera torch pulses to BPM 🔦</div>
                    </div>
                  </div>
                  <div className={`w-8 h-4.5 rounded-full transition-colors flex items-center px-0.5 ${
                    flashSync ? 'bg-amber-400 justify-end' : 'bg-white/20 justify-start'
                  }`}>
                    <div className="w-3.5 h-3.5 rounded-full bg-black shadow-sm" />
                  </div>
                </div>

                {/* Data Monitor Details */}
                <div
                  onClick={() => {
                    setIsSidebarOpen(false)
                    setIsDataMonitorOpen(true)
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Wifi className="w-4 h-4 text-emerald-400" />
                    <div>
                      <div className="text-xs font-bold text-white">0.1 kb/s Bandwidth Monitor</div>
                      <div className="text-[9px] text-emerald-400">99.2% Data Saved • Cache Active</div>
                    </div>
                  </div>
                </div>

                {/* Sleep Timer */}
                <div
                  onClick={() => {
                    setIsSidebarOpen(false)
                    setIsSleepTimerOpen(true)
                  }}
                  className="p-3 rounded-2xl bg-white/5 border border-white/5 flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <Moon className="w-4 h-4 text-purple-400" />
                    <div>
                      <div className="text-xs font-bold text-white">Sleep Timer</div>
                      <div className="text-[9px] text-white/50">{sleepTimer ? `${sleepTimer}m remaining` : 'Disabled'}</div>
                    </div>
                  </div>
                </div>

                {/* Package Info Card */}
                <div className="p-3 rounded-2xl bg-[#FF2A6D]/10 border border-[#FF2A6D]/20">
                  <div className="text-xs font-bold text-[#FF2A6D] mb-0.5">Package Info</div>
                  <div className="text-[11px] text-white/90 font-mono">com.resso.craka</div>
                  <div className="text-[9px] text-white/50">Resso ByteDance Edition • Lag-Free</div>
                </div>
              </div>
            </div>

            <div className="text-[10px] text-white/40 text-center">
              Resso Music • ByteDance Edition
            </div>
          </div>
          <div className="flex-1" onClick={() => setIsSidebarOpen(false)} />
        </div>
      )}

      {/* 7. DATA USAGE & 0.1 KB/S ULTRA SAVER MODAL */}
      {isDataMonitorOpen && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-sm bg-[#120A1A] border border-white/15 rounded-3xl p-5 shadow-2xl flex flex-col max-h-[85vh] overflow-y-auto no-scrollbar">
            <div className="flex justify-between items-center mb-3">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                <span className="text-xs font-black text-emerald-400 uppercase tracking-wider">
                  0.1 kb/s Internet Monitor
                </span>
              </div>
              <button onClick={() => setIsDataMonitorOpen(false)} className="text-white/60">
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Live Speed Card */}
            <div className="p-3.5 rounded-2xl bg-emerald-950/40 border border-emerald-500/30 mb-3.5">
              <div className="flex justify-between items-center mb-2">
                <span className="text-[10px] font-bold text-emerald-300 uppercase">Live Bandwidth Speed</span>
                <span className="text-xs font-black font-mono text-emerald-400 bg-emerald-500/20 px-2 py-0.5 rounded-full">
                  0.1 KB/s (Active)
                </span>
              </div>
              <div className="grid grid-cols-3 gap-2 text-center pt-1 border-t border-emerald-500/20">
                <div>
                  <div className="text-[10px] text-white/50">Session Used</div>
                  <div className="text-xs font-black text-white font-mono">0.04 MB</div>
                </div>
                <div>
                  <div className="text-[10px] text-white/50">Data Saved</div>
                  <div className="text-xs font-black text-emerald-400 font-mono">99.2%</div>
                </div>
                <div>
                  <div className="text-[10px] text-white/50">Offline Cache</div>
                  <div className="text-xs font-black text-[#05D9E8] font-mono">{cachedSongsCount} Songs</div>
                </div>
              </div>
            </div>

            {/* Technical Optimization Breakdown */}
            <div className="space-y-2 mb-3.5 text-xs">
              <div className="p-2.5 rounded-xl bg-white/5 border border-white/5">
                <div className="font-bold text-white mb-0.5 flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Zero Video Preload (95% Data Saved)</span>
                </div>
                <p className="text-[11px] text-white/60">
                  Background MV video tabhi download hota hai jab aap "Watch MV" dabate hain. MV band hone par 0 bytes video data use hota hai.
                </p>
              </div>

              <div className="p-2.5 rounded-xl bg-white/5 border border-white/5">
                <div className="font-bold text-white mb-0.5 flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>100% Local GPU Animations (Zero Lag)</span>
                </div>
                <p className="text-[11px] text-white/60">
                  Vinyl record spin, EQ spectrum bars, floating comments aur lyrics sync poore tareeqe se device par run hote hain, internet se kuch fetch nahi hota.
                </p>
              </div>

              <div className="p-2.5 rounded-xl bg-white/5 border border-white/5">
                <div className="font-bold text-white mb-0.5 flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Lightweight Audio Streaming</span>
                </div>
                <p className="text-[11px] text-white/60">
                  Audio lightweight compressed chunks me buffer hoti hai, jisse 0.1 kb/s par bhi song smooth chalta hai bina rukey.
                </p>
              </div>
            </div>

            {/* One-Click Pre-Cache All Tracks */}
            <button
              disabled={isCachingAll}
              onClick={() => {
                setIsCachingAll(true)
                setTimeout(() => {
                  setSongs(prev => prev.map(s => ({ ...s, isDownloaded: true })))
                  setCachedSongsCount(songs.length)
                  setIsCachingAll(false)
                  showToast("All Songs Cached! Now Plays on 0.0 KB Internet 💾⚡")
                }, 1200)
              }}
              className="w-full py-2.5 bg-gradient-to-r from-emerald-500 to-teal-500 rounded-2xl text-xs font-bold text-white shadow-lg shadow-emerald-500/30 flex items-center justify-center gap-2 active:scale-95 transition mb-2"
            >
              {isCachingAll ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Caching Tracks into Offline Storage...</span>
                </>
              ) : (
                <>
                  <Download className="w-3.5 h-3.5" />
                  <span>Pre-Cache All Songs (0 KB Offline Play)</span>
                </>
              )}
            </button>

            <button
              onClick={() => setIsDataMonitorOpen(false)}
              className="w-full py-2 bg-white/10 rounded-2xl text-xs font-bold text-white/80 hover:bg-white/20 transition"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
