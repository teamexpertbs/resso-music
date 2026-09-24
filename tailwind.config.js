/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        resso: {
          primary: '#FF2A6D',
          secondary: '#05D9E8',
          accent: '#FFE600',
          bg: '#0A0910',
          surface: '#14131E',
          card: '#1B1927',
          muted: '#A09EB2'
        }
      }
    },
  },
  plugins: [],
}
