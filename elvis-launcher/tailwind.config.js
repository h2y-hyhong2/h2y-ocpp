/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'Inconsolata', 'D2Coding', 'Consolas', 'monospace'],
      },
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
