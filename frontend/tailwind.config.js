/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#111827",
        canvas: "#f3f6ff",
        violet: "#6558f5",
        aqua: "#a7f3e3",
        paper: "#ffffff",
      },
      fontFamily: {
        sans: ["Manrope", "ui-sans-serif", "system-ui", "sans-serif"],
        display: ["Space Grotesk", "Manrope", "ui-sans-serif", "system-ui", "sans-serif"],
      },
      boxShadow: {
        card: "0 24px 70px rgba(37, 43, 91, 0.12)",
      },
    },
  },
  plugins: [],
};
