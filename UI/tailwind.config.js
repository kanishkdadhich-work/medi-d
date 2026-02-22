/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        med: {
          50: '#f6fbff',
          100: '#e9f6ff',
          200: '#cfeaff',
          300: '#a7d9ff',
          400: '#73beff',
          500: '#4b9ff5',
          600: '#2d81d6',
          700: '#1f66af',
          800: '#1e568f',
          900: '#1f4a75'
        },
        success: {
          50: '#effdf6',
          100: '#d7f8e8',
          500: '#16a34a',
          600: '#15803d'
        }
      },
      boxShadow: {
        panel: '0 12px 34px rgba(30, 74, 117, 0.08)'
      }
    }
  },
  plugins: []
};
