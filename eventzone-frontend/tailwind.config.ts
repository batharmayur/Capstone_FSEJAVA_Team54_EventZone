import type { Config } from 'tailwindcss';

const config: Config = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f5f7ff',
          100: '#e8eeff',
          200: '#cddcff',
          300: '#a3beff',
          400: '#7a9dff',
          500: '#4f7ef7',
          600: '#315de0',
          700: '#2947b3',
          800: '#263a8d',
          900: '#233673'
        }
      },
      boxShadow: {
        soft: '0 20px 45px -20px rgba(79, 126, 247, 0.45)'
      }
    }
  },
  plugins: []
};

export default config;
