/**** Tailwind CSS Config ****/
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './index.html',
    './src/**/*.{ts,tsx,jsx,js}'
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#0d4bcf',
          dark: '#08398f'
        }
      }
    },
  },
  plugins: [],
};
