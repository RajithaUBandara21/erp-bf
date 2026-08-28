import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Universal ERP",
  description: "Universal ERP - inventory, sales, purchasing, accounting, and POS in one platform.",
};

// Runs synchronously during HTML parsing so a saved dark theme is applied before first paint.
// Mirrors the "erp_theme" key ThemeToggle writes; the try/catch covers localStorage being unavailable.
const themeScript = `(function(){try{var t=localStorage.getItem("erp_theme");if(t==="dark"||t==="light")document.documentElement.dataset.theme=t}catch(e){}})()`;

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      data-theme="light"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
