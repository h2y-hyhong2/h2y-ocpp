/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Inconsolata', 'monospace'],
      },
      fontSize: {
        xs: ['0.8125rem', { lineHeight: '1.15rem' }],     // 13px (기존 12px)
        sm: ['0.9375rem', { lineHeight: '1.35rem' }],     // 15px (기존 14px)
        base: ['1.0625rem', { lineHeight: '1.55rem' }],   // 17px (기존 16px)
        lg: ['1.1875rem', { lineHeight: '1.75rem' }],     // 19px (기존 18px)
        xl: ['1.375rem', { lineHeight: '1.9rem' }],       // 22px (기존 20px)
        '2xl': ['1.625rem', { lineHeight: '2.15rem' }],   // 26px (기존 24px)
        '3xl': ['2rem', { lineHeight: '2.4rem' }],        // 32px (기존 30px)
      },
    },
  },
  plugins: [],
}
