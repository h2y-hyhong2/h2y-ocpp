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
      fontSize: {
        xs: ['0.875rem', { lineHeight: '1.25rem' }],     // 14px (기존 12px)
        sm: ['0.9375rem', { lineHeight: '1.35rem' }],    // 15px (기존 14px)
        base: ['1.0625rem', { lineHeight: '1.55rem' }],  // 17px (기존 16px)
        lg: ['1.1875rem', { lineHeight: '1.75rem' }],    // 19px (기존 18px)
        xl: ['1.375rem', { lineHeight: '1.9rem' }],      // 22px (기존 20px)
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
