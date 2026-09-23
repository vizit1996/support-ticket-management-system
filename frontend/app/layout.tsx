import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ticket Desk",
  description: "Support ticket management",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <header className="topbar">
          <Link href="/" className="brand" aria-label="Ticket Desk home">
            <span className="brand-mark">T</span>
            <span>Ticket Desk</span>
          </Link>
          <span className="environment">Support operations</span>
        </header>
        {children}
      </body>
    </html>
  );
}
