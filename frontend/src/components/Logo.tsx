/** Mascote robo + "WECTI" - mesmo SVG usado em .nav-logo-icon na landing
 *  page (index.html raiz), replicado aqui para o sistema parecer parte da
 *  mesma familia visual. */
export default function Logo({ className = '' }: { className?: string }) {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <svg className="h-10 w-10 flex-shrink-0" viewBox="0 0 36 36" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="9" y="8" width="18" height="14" rx="4" fill="none" stroke="#00e8c4" strokeWidth="1.5" />
        <circle cx="14" cy="14" r="2" fill="#00e8c4" />
        <circle cx="22" cy="14" r="2" fill="#00e8c4" />
        <circle cx="14" cy="14" r="1" fill="#fff" opacity="0.7" />
        <circle cx="22" cy="14" r="1" fill="#fff" opacity="0.7" />
        <rect x="13" y="18" width="10" height="1.5" rx="0.75" fill="#00e8c4" opacity="0.7" />
        <line x1="18" y1="8" x2="18" y2="4" stroke="#00e8c4" strokeWidth="1.5" strokeLinecap="round" />
        <circle cx="18" cy="3" r="1.5" fill="#00d4b0" />
        <rect x="16" y="22" width="4" height="2" fill="#00e8c4" opacity="0.5" />
        <rect x="10" y="24" width="16" height="9" rx="3" fill="none" stroke="#00e8c4" strokeWidth="1.5" opacity="0.8" />
        <rect x="13" y="27" width="4" height="3" rx="1" fill="#00e8c4" opacity="0.3" />
        <circle cx="24" cy="28.5" r="1.5" fill="#00e8c4" opacity="0.5" />
        <line x1="10" y1="26" x2="6" y2="29" stroke="#00e8c4" strokeWidth="1.5" strokeLinecap="round" />
        <line x1="26" y1="26" x2="30" y2="29" stroke="#00e8c4" strokeWidth="1.5" strokeLinecap="round" />
      </svg>
      <span className="bg-gradient-to-r from-accent to-accent-2 bg-clip-text text-xl font-extrabold tracking-tight text-transparent">
        WECTI
      </span>
    </div>
  );
}
