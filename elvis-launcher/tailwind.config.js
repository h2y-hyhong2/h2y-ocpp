/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          dark: '#0A1E5A',
          orange: '#E96600',
          blue: '#0569A0',
          cyan: '#00A0E9'
        }
      }
    },
  },
  plugins: [],
}
