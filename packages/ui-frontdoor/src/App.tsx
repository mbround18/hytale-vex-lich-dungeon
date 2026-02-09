import React, { useEffect, useMemo, useState } from "react";
import {
  BrowserRouter,
  Link,
  Navigate,
  NavLink,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Badge } from "ui-shared/components";
import { navSections, primaryAction } from "./data/navigation";
import { homeContent } from "./data/home";
import "./styles/app.scss";

const isExternal = (href: string) => href.startsWith("http");

const linkProps = (href: string) =>
  isExternal(href)
    ? ({ target: "_blank", rel: "noopener noreferrer" } as const)
    : ({} as const);

const accentClass: Record<string, string> = {
  purple: "hover:border-vex-purple",
  gold: "hover:border-ancient-gold",
  green: "hover:border-necro-green",
};

const normalizePath = (href: string) => {
  const withoutQuery = href.split("?")[0];
  if (withoutQuery === "/") return "/";
  return withoutQuery.replace(/\/+$/, "");
};

const routePaths = new Set([
  "/",
  "/dev",
  "/dev/ui",
  "/dev/hosting",
  "/dev/logs",
]);

const isMarkdownRoute = (href: string) =>
  href.startsWith("/dev/ui/") || href.startsWith("/dev/logs/");

const isRouteLink = (href: string) => {
  if (!href.startsWith("/")) return false;
  if (href.includes("#")) return false;
  if (href.match(/\.(html|json)$/i)) return false;
  if (href.match(/\.md$/i)) return isMarkdownRoute(href);
  return routePaths.has(normalizePath(href));
};

type FrontmatterData = Record<string, any>;

const parseFrontmatter = (
  content: string,
): { frontmatter: FrontmatterData; markdown: string } => {
  const frontmatterRegex = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/;
  const match = content.match(frontmatterRegex);

  if (!match) {
    return { frontmatter: {}, markdown: content };
  }

  const [, frontmatterStr, markdown] = match;
  const frontmatter: FrontmatterData = {};

  // Simple YAML parser for common formats
  frontmatterStr.split("\n").forEach((line: string) => {
    const keyValue = line.match(/^(\w+):\s*(.+)$/);
    if (keyValue) {
      const [, key, value] = keyValue;
      // Handle arrays: [item1, item2]
      if (value.startsWith("[") && value.endsWith("]")) {
        frontmatter[key] = value
          .slice(1, -1)
          .split(",")
          .map((v) => v.trim().replace(/^["']|["']$/g, ""));
      }
      // Handle quoted strings
      else if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        frontmatter[key] = value.slice(1, -1);
      }
      // Handle numbers
      else if (!isNaN(Number(value))) {
        frontmatter[key] = Number(value);
      }
      // Handle booleans
      else if (value === "true" || value === "false") {
        frontmatter[key] = value === "true";
      }
      // Plain strings
      else {
        frontmatter[key] = value;
      }
    }
  });

  return { frontmatter, markdown };
};

function Shell() {
  const [navOpen, setNavOpen] = useState(false);
  const sections = useMemo(() => navSections, []);
  const quickRoutes = useMemo(
    () => [
      { label: "Home", to: "/" },
      { label: "Dev Hub", to: "/dev" },
      { label: "Hosting", to: "/dev/hosting" },
      { label: "Dev Logs", to: "/dev/logs" },
    ],
    [],
  );
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogoClick = () => {
    if (location.pathname !== "/") {
      navigate("/");
      return;
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const renderLink = (
    href: string,
    label: string,
    className: string,
    onClick?: () => void,
  ) => {
    if (isRouteLink(href)) {
      const target = href.endsWith(".md") ? `/md${href}` : normalizePath(href);
      return (
        <NavLink
          to={target}
          className={({ isActive }) =>
            `${className} ${isActive ? "text-necro-green" : ""}`.trim()
          }
          onClick={onClick}
        >
          {label}
        </NavLink>
      );
    }
    return (
      <a
        href={href}
        className={className}
        onClick={onClick}
        {...linkProps(href)}
      >
        {label}
      </a>
    );
  };

  return (
    <div className="min-h-screen bg-vex-dark text-slate-200 antialiased custom-scrollbar selection:bg-necro-green selection:text-vex-dark relative">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div
        className="fixed inset-0 opacity-10 pointer-events-none"
        style={{
          backgroundImage:
            "url('https://www.transparenttextures.com/patterns/black-scales.png')",
        }}
      />

      <nav className="sticky top-0 w-full z-50 bg-vex-surface/95 backdrop-blur-md border-b border-vex-border/80 shadow-[0_8px_24px_rgba(0,0,0,0.25)] transition-all duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-3 py-3">
            <div className="flex justify-between items-center">
              <button
                className="flex-shrink-0 flex items-center gap-3 cursor-pointer group"
                onClick={handleLogoClick}
                type="button"
                aria-label="Go to home"
              >
                <img
                  src="https://imagedelivery.net/6QPDh2i4MEi-JY_RW1iPZQ/62e09a2c-0be3-4486-ebe6-4185678d9800/public"
                  alt="Vex Logo"
                  className="w-11 h-11 rounded-xl border border-ancient-gold/70 shadow-lg transform group-hover:rotate-6 transition duration-300"
                />
                <div>
                  <span className="font-fantasy font-bold text-lg tracking-widest text-ancient-gold group-hover:text-necro-green transition duration-300">
                    Vex&apos;s Challenge
                  </span>
                  <span className="block text-[10px] uppercase tracking-[0.3em] text-slate-500">
                    Frontdoor
                  </span>
                </div>
              </button>

              <div className="hidden md:flex items-center gap-3">
                <div className="flex flex-wrap items-center gap-2 rounded-full border border-vex-border/70 bg-vex-panel px-2 py-1">
                  {quickRoutes.map((route) => (
                    <NavLink
                      key={route.to}
                      to={route.to}
                      className={({ isActive }) =>
                        `px-3 py-1 rounded-full text-[11px] font-semibold uppercase tracking-[0.2em] transition ${
                          isActive
                            ? "bg-necro-green text-vex-dark shadow-[0_0_12px_rgba(74,222,128,0.4)]"
                            : "text-slate-100 hover:text-necro-green hover:bg-white/5"
                        }`
                      }
                    >
                      {route.label}
                    </NavLink>
                  ))}
                </div>
                {renderLink(
                  primaryAction.href,
                  primaryAction.label,
                  "ml-2 px-4 py-2 rounded-full bg-vex-deep text-white font-fantasy text-xs uppercase tracking-widest border border-necro-green/40 hover:bg-necro-green hover:text-vex-dark transition",
                )}
              </div>

              <div className="md:hidden flex items-center">
                <button
                  className="text-ancient-gold hover:text-necro-green"
                  aria-controls="mobile-nav"
                  aria-expanded={navOpen}
                  aria-label={
                    navOpen ? "Close navigation menu" : "Open navigation menu"
                  }
                  onClick={() => setNavOpen((prev) => !prev)}
                >
                  <svg
                    className="h-8 w-8"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M4 6h16M4 12h16M4 18h16"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <div className="hidden md:flex items-center gap-6 border-t border-vex-border/60 pt-3">
              {sections.map((section) => (
                <div key={section.id} className="flex items-center gap-3">
                  <span className="text-[10px] uppercase tracking-[0.2em] text-slate-200">
                    {section.label}
                  </span>
                  <div className="flex flex-wrap items-center gap-3 text-xs text-slate-100">
                    {section.links.map((child) => (
                      <span key={child.href}>
                        {renderLink(
                          child.href,
                          child.label,
                          "hover:text-necro-green transition",
                        )}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div
          id="mobile-nav"
          className={`md:hidden border-t border-vex-border bg-vex-surface/95 ${navOpen ? "block" : "hidden"}`}
          aria-hidden={!navOpen}
        >
          <div className="px-6 py-5 space-y-6">
            {sections.map((section) => (
              <div key={section.id} className="space-y-3">
                <p className="text-[11px] uppercase tracking-[0.3em] text-slate-200">
                  {section.label}
                </p>
                <div className="grid grid-cols-1 gap-2">
                  {section.links.map((child) => (
                    <span key={child.href}>
                      {renderLink(
                        child.href,
                        child.label,
                        "block text-sm text-slate-100 hover:text-necro-green transition",
                        () => setNavOpen(false),
                      )}
                    </span>
                  ))}
                </div>
              </div>
            ))}
            {renderLink(
              primaryAction.href,
              primaryAction.label,
              "block text-center px-4 py-2 rounded-full bg-vex-deep text-white font-fantasy text-xs uppercase tracking-widest border border-necro-green/40 hover:bg-necro-green hover:text-vex-dark transition",
              () => setNavOpen(false),
            )}
          </div>
        </div>
      </nav>

      <Outlet />
    </div>
  );
}

const renderResourceLink = (href: string, label: string, className: string) => {
  if (isRouteLink(href)) {
    const target = href.endsWith(".md") ? `/md${href}` : normalizePath(href);
    return (
      <Link to={target} className={className}>
        {label}
      </Link>
    );
  }
  return (
    <a href={href} className={className} {...linkProps(href)}>
      {label}
    </a>
  );
};

const extractYoutubeId = (url: string) => {
  const match = url.match(/(?:v=|youtu\.be\/)([A-Za-z0-9_-]{6,})/);
  return match ? match[1] : null;
};

const toYoutubeThumb = (url: string) => {
  const id = extractYoutubeId(url);
  return id ? `https://img.youtube.com/vi/${id}/hqdefault.jpg` : null;
};

type DevLogEntry = {
  title: string;
  date?: string;
  excerpt?: string;
  href: string;
};

const parseDevLogEntry = (file: string, content: string): DevLogEntry => {
  const datePart = file.split("-").slice(0, 3).join("-");
  const date = /^\d{4}-\d{2}-\d{2}$/.test(datePart) ? datePart : undefined;
  const lines = content.split(/\r?\n/).map((line) => line.trim());
  let title = file.replace(/\.md$/i, "").replace(/-/g, " ");
  for (const line of lines) {
    if (line.startsWith("# ")) {
      title = line.replace(/^#\s+/, "").trim();
      break;
    }
  }
  let excerpt = "";
  for (const line of lines) {
    if (!line) continue;
    if (line.startsWith("#")) continue;
    if (line.startsWith(">")) continue;
    excerpt = line;
    break;
  }
  return {
    title,
    date,
    excerpt,
    href: `/dev/logs/${file}`,
  };
};

const formatLogTitle = (file: string) =>
  file.replace(/\.md$/i, "").replace(/-/g, " ");

const parseLogDate = (file: string) => {
  const datePart = file.split("-").slice(0, 3).join("-");
  return /^\d{4}-\d{2}-\d{2}$/.test(datePart) ? datePart : undefined;
};

function HomePage() {
  const [activeVideoCategory, setActiveVideoCategory] = useState<
    "dev" | "trailer"
  >("dev");
  const videoEntries = useMemo(
    () =>
      homeContent.videos.entries.filter(
        (entry) => entry.category === activeVideoCategory,
      ),
    [activeVideoCategory],
  );

  return (
    <main id="main-content">
      <section className="pt-32 pb-20 lg:pt-48 lg:pb-32 overflow-hidden relative">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-vex-purple rounded-full blur-[120px] opacity-20 pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="lg:grid lg:grid-cols-12 lg:gap-16 items-center">
            <div className="lg:col-span-7 text-center lg:text-left mb-12 lg:mb-0">
              <div className="inline-flex items-center gap-2 px-4 py-1 rounded-full bg-vex-deep/30 border border-necro-green/30 text-necro-green text-sm font-bold mb-6 tracking-wide uppercase">
                <span className="w-2 h-2 rounded-full bg-necro-green animate-pulse" />
                {homeContent.hero.kicker}
              </div>
              <h1 className="font-fantasy text-5xl sm:text-6xl lg:text-7xl font-black text-white tracking-tight mb-6 leading-tight text-glow">
                {homeContent.hero.title[0]} <br />
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-vex-purple via-purple-400 to-necro-green">
                  {homeContent.hero.title[1]}
                </span>
              </h1>
              <p className="text-xl text-slate-400 mb-8 leading-relaxed max-w-2xl mx-auto lg:mx-0 font-body">
                {homeContent.hero.subtitle}
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
                {homeContent.hero.actions.map((action) => (
                  <a
                    key={action.label}
                    href={action.href}
                    className={
                      action.variant === "primary"
                        ? "px-8 py-4 rounded bg-gradient-to-r from-vex-deep to-vex-purple text-white font-fantasy font-bold text-xl hover:from-necro-green hover:to-necro-dark hover:scale-105 transition duration-300 shadow-lg border border-white/10 text-center flex items-center justify-center gap-2"
                        : "px-8 py-4 rounded bg-[#FF5E5B] text-white font-fantasy font-bold text-xl hover:bg-[#d44542] hover:scale-105 transition duration-300 shadow-lg border border-white/10 text-center flex items-center justify-center gap-2"
                    }
                    {...linkProps(action.href)}
                  >
                    <span>{action.icon}</span>
                    {action.label}
                  </a>
                ))}
              </div>
              <p className="mt-4 text-sm text-slate-500 font-body italic">
                {homeContent.hero.note}
              </p>
            </div>

            <div className="lg:col-span-5 relative flex justify-center">
              <div className="absolute inset-0 bg-gradient-to-tr from-vex-purple to-necro-green opacity-20 rounded-full blur-3xl" />
              <div className="relative w-80 h-96 lg:w-96 lg:h-[30rem] float-animation">
                <div className="absolute inset-0 border-4 border-ancient-gold rounded-3xl transform rotate-3 bg-vex-dark/50 backdrop-blur-sm z-0" />
                <div className="absolute inset-0 border-4 border-stone-600 rounded-3xl transform -rotate-3 bg-vex-dark/80 z-10 flex items-center justify-center overflow-hidden">
                  <div className="text-center p-8">
                    <div className="text-8xl mb-4 drop-shadow-[0_0_15px_rgba(74,222,128,0.8)]">
                      💀
                    </div>
                    <h3 className="font-fantasy text-2xl text-vex-purple">
                      {homeContent.heroPortrait.title}
                    </h3>
                    <p className="text-sm text-slate-400 mt-2 italic">
                      {homeContent.heroPortrait.quote}
                    </p>
                    <div className="mt-4 text-xs text-necro-green border border-necro-green/30 rounded px-2 py-1 inline-block">
                      {homeContent.heroPortrait.bossLevel}
                    </div>
                    <div className="mt-6 text-xs text-slate-500">
                      {homeContent.heroPortrait.hint}
                    </div>
                  </div>
                </div>

                <div className="absolute -top-6 -right-6 w-16 h-16 bg-stone-800 border-2 border-ancient-gold rounded-full flex items-center justify-center z-20 shadow-lg animate-bounce">
                  <span className="text-2xl">⚙️</span>
                </div>
                <div className="absolute -bottom-6 -left-6 w-16 h-16 bg-stone-800 border-2 border-ancient-gold rounded-full flex items-center justify-center z-20 shadow-lg animate-pulse">
                  <span className="text-2xl">🧪</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section
        id={homeContent.lore.id}
        className="py-20 bg-stone-900 relative border-t border-slate-800"
      >
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="font-fantasy text-4xl text-ancient-gold mb-8">
            {homeContent.lore.title}
          </h2>
          <div className="mx-auto">
            {homeContent.lore.paragraphs.map((paragraph, index) => (
              <p
                key={`lore-${index}`}
                className="text-slate-300 leading-8 mb-6"
              >
                {paragraph}
              </p>
            ))}
          </div>

          <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-8">
            {homeContent.lore.cards.map((card) => (
              <div
                key={card.title}
                className={`p-6 rounded bg-vex-dark border border-slate-700 transition ${accentClass[card.accent]}`}
              >
                <div className="text-4xl mb-4">{card.icon}</div>
                <h3 className="font-fantasy text-xl text-white mb-2">
                  {card.title}
                </h3>
                <p className="text-slate-400 text-sm">{card.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section
        id={homeContent.dungeon.id}
        className="py-24 bg-vex-dark relative"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="font-fantasy text-4xl text-white mb-4">
              {homeContent.dungeon.title}
            </h2>
            <p className="text-slate-400">{homeContent.dungeon.subtitle}</p>
          </div>

          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              {homeContent.dungeon.features.map((feature) => (
                <div
                  key={feature.step}
                  className={`flex gap-4 ${feature.status === "coming" ? "opacity-75" : ""}`}
                >
                  <div
                    className={
                      feature.status === "coming"
                        ? "flex-shrink-0 w-12 h-12 bg-stone-800 rounded border border-slate-600 flex items-center justify-center text-xl font-bold text-slate-400"
                        : "flex-shrink-0 w-12 h-12 bg-vex-deep rounded border border-vex-purple flex items-center justify-center text-xl font-bold text-white"
                    }
                  >
                    {feature.step}
                  </div>
                  <div>
                    <h3
                      className={
                        feature.status === "coming"
                          ? "font-fantasy text-2xl text-slate-300 mb-2"
                          : "font-fantasy text-2xl text-necro-green mb-2"
                      }
                    >
                      {feature.title}
                      {feature.status === "coming" && (
                        <span className="ml-2">
                          <Badge variant="gold">Coming Soon</Badge>
                        </span>
                      )}
                    </h3>
                    <p
                      className={
                        feature.status === "coming"
                          ? "text-slate-500"
                          : "text-slate-400"
                      }
                    >
                      {feature.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="relative h-96 bg-stone-900 rounded-lg border-2 border-slate-700 overflow-hidden group">
              <div className="absolute inset-0 flex items-center justify-center bg-gradient-to-br from-stone-800 to-black">
                <div className="text-center">
                  <span className="text-6xl mb-4 block opacity-50">🚧</span>
                  <span className="text-slate-500 font-fantasy text-2xl group-hover:text-slate-400 transition block">
                    {homeContent.dungeon.preview.title}
                  </span>
                  <span className="text-ancient-gold text-sm mt-2 uppercase tracking-widest">
                    {homeContent.dungeon.preview.badge}
                  </span>
                </div>
              </div>
              <div
                className="absolute inset-0"
                style={{
                  backgroundImage:
                    "radial-gradient(#4c1d95 1px, transparent 1px)",
                  backgroundSize: "20px 20px",
                  opacity: 0.2,
                }}
              />
            </div>
          </div>
        </div>
      </section>

      <section
        id={homeContent.videos.id}
        className="py-24 bg-vex-surface/40 border-t border-vex-border"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-6">
            <div>
              <p className="text-necro-green text-xs uppercase tracking-[0.3em]">
                Video Vault
              </p>
              <h2 className="font-fantasy text-4xl text-white mt-3">
                {homeContent.videos.title}
              </h2>
              <p className="text-slate-400 mt-2 max-w-2xl">
                {homeContent.videos.subtitle}
              </p>
            </div>
            <div className="flex items-center gap-2 rounded-full border border-vex-border/60 bg-vex-panel p-1">
              {homeContent.videos.categories.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => setActiveVideoCategory(cat.id)}
                  aria-pressed={activeVideoCategory === cat.id}
                  className={`px-4 py-2 rounded-full text-xs uppercase tracking-[0.2em] font-semibold transition ${
                    activeVideoCategory === cat.id
                      ? "bg-necro-green text-vex-dark shadow-[0_0_16px_rgba(74,222,128,0.4)]"
                      : "text-slate-100 hover:text-necro-green"
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>

          {videoEntries.length === 0 ? (
            <div className="mt-10 rounded-2xl border border-vex-border bg-vex-surface/70 p-8 text-slate-400">
              No videos in this category yet.
            </div>
          ) : (
            <div className="mt-10 flex gap-6 overflow-x-auto pb-4 custom-scrollbar">
              {videoEntries.map((entry) => {
                const thumb = toYoutubeThumb(entry.href);
                return (
                  <a
                    key={entry.href}
                    href={entry.href}
                    target="_blank"
                    rel="noreferrer"
                    className="min-w-[280px] max-w-[320px] flex-shrink-0 rounded-2xl border border-vex-border bg-vex-surface/70 overflow-hidden hover:border-necro-green/60 transition"
                  >
                    <div className="relative aspect-video bg-black">
                      {thumb ? (
                        <img
                          src={thumb}
                          alt={entry.title}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-500">
                          Video
                        </div>
                      )}
                      <div className="absolute inset-0 flex items-center justify-center">
                        <div className="w-12 h-12 rounded-full bg-black/60 border border-white/20 flex items-center justify-center">
                          <span className="text-white text-xl">▶</span>
                        </div>
                      </div>
                    </div>
                    <div className="p-4 space-y-2">
                      <h3 className="font-fantasy text-lg text-white">
                        {entry.title}
                      </h3>
                      {entry.summary && (
                        <p className="text-sm text-slate-400">
                          {entry.summary}
                        </p>
                      )}
                      <span className="text-[11px] uppercase tracking-[0.3em] text-necro-green">
                        {entry.category}
                      </span>
                    </div>
                  </a>
                );
              })}
            </div>
          )}
        </div>
      </section>

      <section
        id={homeContent.callToAction.id}
        className="py-20 relative overflow-hidden"
      >
        <div className="absolute inset-0 bg-gradient-to-b from-vex-dark to-vex-deep opacity-80" />
        <div className="max-w-4xl mx-auto px-4 relative z-10 text-center">
          <h2 className="font-fantasy text-5xl text-ancient-gold mb-6 text-glow">
            {homeContent.callToAction.title}
          </h2>
          <p className="text-xl text-slate-300 mb-10">
            {homeContent.callToAction.subtitle}
          </p>

          <div className="bg-stone-900/80 p-8 rounded-2xl border border-ancient-gold/30 backdrop-blur-sm max-w-lg mx-auto">
            <div className="flex flex-col gap-4">
              <a
                href={homeContent.callToAction.actions[0].href}
                className="w-full py-4 rounded bg-necro-green hover:bg-necro-dark text-vex-dark font-fantasy font-bold text-xl transition duration-300 flex items-center justify-center gap-2"
                {...linkProps(homeContent.callToAction.actions[0].href)}
              >
                <svg
                  className="w-6 h-6"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    fillRule="evenodd"
                    d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    clipRule="evenodd"
                  />
                </svg>
                {homeContent.callToAction.actions[0].label}
              </a>

              <a
                href={homeContent.callToAction.actions[1].href}
                className="w-full py-4 rounded bg-[#29abe0] hover:bg-[#208ab5] text-white font-fantasy font-bold text-xl transition duration-300 flex items-center justify-center gap-2"
                {...linkProps(homeContent.callToAction.actions[1].href)}
              >
                <span>☕</span>
                {homeContent.callToAction.actions[1].label}
              </a>

              <div className="flex items-center justify-center gap-4 text-sm text-slate-400 mt-2">
                <span>{homeContent.callToAction.footer[0]}</span>
                <span>•</span>
                <span>{homeContent.callToAction.footer[1]}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <footer className="bg-black py-12 border-t border-slate-900">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <div className="flex justify-center items-center gap-2 mb-4">
            <span className="text-2xl">☠️</span>
            <span className="font-fantasy text-xl text-slate-500">
              {homeContent.footer.title}
            </span>
          </div>
          <p className="text-slate-600 text-sm mb-8">
            {homeContent.footer.subtitle}
          </p>
          <div className="flex justify-center space-x-6 text-slate-500">
            {homeContent.footer.links.map((link) => (
              <a
                key={link.label}
                href={link.href}
                className="hover:text-vex-purple transition"
              >
                {link.label}
              </a>
            ))}
          </div>
        </div>
      </footer>
    </main>
  );
}

function DevHubPage() {
  const devLinks =
    navSections.find((section) => section.id === "dev")?.links ?? [];
  const resourceLinks = devLinks.filter((link) => link.href !== "/dev");
  const [logEntries, setLogEntries] = useState<DevLogEntry[]>([]);
  const [logLoading, setLogLoading] = useState(true);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [selectedContent, setSelectedContent] = useState("");
  const [selectedFrontmatter, setSelectedFrontmatter] =
    useState<FrontmatterData>({});
  const [contentLoading, setContentLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const res = await fetch("/dev/logs/index.json");
        if (!res.ok) {
          throw new Error("Failed to load index");
        }
        const files = (await res.json()) as string[];
        const latestFiles = files.slice(0, 6);
        const entries = await Promise.all(
          latestFiles.map(async (file) => {
            try {
              const contentRes = await fetch(`/dev/logs/${file}`);
              const content = contentRes.ok ? await contentRes.text() : "";
              return parseDevLogEntry(file, content);
            } catch {
              return parseDevLogEntry(file, "");
            }
          }),
        );
        if (!cancelled) {
          setLogEntries(entries);
        }
      } catch {
        if (!cancelled) {
          setLogEntries([]);
        }
      } finally {
        if (!cancelled) {
          setLogLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const loadEntryContent = async (index: number) => {
    const entry = logEntries[index];
    if (!entry) return;

    setSelectedIndex(index);
    setContentLoading(true);
    try {
      const res = await fetch(entry.href);
      if (res.ok) {
        const rawContent = await res.text();
        const { frontmatter: fm, markdown: md } = parseFrontmatter(rawContent);
        setSelectedFrontmatter(fm);
        setSelectedContent(md);
      }
    } catch (error) {
      setSelectedContent("Failed to load content");
      setSelectedFrontmatter({});
    } finally {
      setContentLoading(false);
    }
  };

  // Auto-load first entry when entries are loaded
  useEffect(() => {
    if (logEntries.length > 0 && selectedContent === "") {
      loadEntryContent(0);
    }
  }, [logEntries]);

  return (
    <main id="main-content" className="py-24 bg-vex-dark">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <header className="space-y-4">
          <p className="text-necro-green text-xs uppercase tracking-[0.3em]">
            Developer Hub
          </p>
          <h1 className="font-fantasy text-4xl text-white">Dev log portal</h1>
          <p className="text-slate-400 max-w-2xl">
            A running narrative of Vex build notes, plus quick links to the
            resources we use daily.
          </p>
        </header>

        <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
          {/* Entry List */}
          <div className="space-y-6">
            <div className="flex items-center justify-between gap-4">
              <h2 className="font-fantasy text-2xl text-ancient-gold">
                Recent entries
              </h2>
            </div>
            <div className="space-y-3">
              {(logLoading ? Array.from({ length: 6 }) : logEntries).map(
                (entry, index) => (
                  <button
                    key={entry?.href ?? `loading-${index}`}
                    onClick={() => loadEntryContent(index)}
                    className={`w-full text-left rounded-lg border p-4 transition ${
                      selectedIndex === index && !logLoading
                        ? "border-necro-green bg-necro-green/10"
                        : "border-vex-border bg-vex-surface/40 hover:border-vex-border/80"
                    }`}
                  >
                    {entry ? (
                      <>
                        <p className="text-[11px] uppercase tracking-[0.3em] text-slate-500">
                          {entry.date ?? "Dev Log"}
                        </p>
                        <h3 className="font-fantasy text-sm text-white mt-1 line-clamp-2">
                          {entry.title}
                        </h3>
                      </>
                    ) : (
                      <div className="animate-pulse space-y-2">
                        <div className="h-2 w-16 bg-vex-border/60 rounded" />
                        <div className="h-3 w-32 bg-vex-border/60 rounded" />
                      </div>
                    )}
                  </button>
                ),
              )}
            </div>

            {/* Resources Sidebar */}
            <div className="space-y-4 pt-6 border-t border-vex-border">
              <h2 className="font-fantasy text-lg text-ancient-gold">
                Resources
              </h2>
              <div className="space-y-3">
                {resourceLinks.map((link) => (
                  <div
                    key={link.href}
                    className="rounded-lg border border-vex-border bg-vex-surface/60 p-3"
                  >
                    {renderResourceLink(
                      link.href,
                      link.label,
                      "text-xs text-slate-200 hover:text-necro-green transition",
                    )}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Content Preview */}
          <div className="rounded-2xl border border-vex-border bg-vex-surface/40 p-6">
            {contentLoading && (
              <div className="animate-pulse space-y-4">
                <div className="h-6 w-40 bg-vex-border/60 rounded" />
                <div className="h-4 w-3/4 bg-vex-border/40 rounded" />
                <div className="h-4 w-full bg-vex-border/40 rounded" />
              </div>
            )}

            {!contentLoading && selectedContent && (
              <>
                <article className="markdown-body prose prose-invert prose-headings:font-fantasy prose-a:text-necro-green max-w-none mb-6">
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    components={{
                      a: ({ href = "", children, ...props }) => {
                        if (!href) {
                          return <span {...props}>{children}</span>;
                        }
                        if (isExternal(href)) {
                          return (
                            <a
                              href={href}
                              target="_blank"
                              rel="noopener noreferrer"
                              {...props}
                            >
                              {children}
                            </a>
                          );
                        }
                        if (href.startsWith("/")) {
                          const target = href.endsWith(".md")
                            ? `/md${href}`
                            : href;
                          return (
                            <Link to={target} {...props}>
                              {children}
                            </Link>
                          );
                        }
                        return (
                          <a href={href} {...props}>
                            {children}
                          </a>
                        );
                      },
                    }}
                  >
                    {selectedContent}
                  </ReactMarkdown>
                </article>

                {/* Navigation */}
                <div className="flex items-center justify-between gap-4 pt-6 border-t border-vex-border">
                  <button
                    onClick={() =>
                      loadEntryContent(Math.max(0, selectedIndex - 1))
                    }
                    disabled={selectedIndex === 0}
                    className={`px-4 py-2 rounded text-xs uppercase tracking-widest transition ${
                      selectedIndex === 0
                        ? "text-slate-600 cursor-not-allowed"
                        : "text-necro-green hover:bg-necro-green/20"
                    }`}
                  >
                    ← Previous
                  </button>

                  <span className="text-xs text-slate-400">
                    {selectedIndex + 1} / {logEntries.length}
                  </span>

                  {selectedIndex >= logEntries.length - 1 ? (
                    <Link
                      to="/dev/logs"
                      className="px-4 py-2 rounded text-xs uppercase tracking-widest text-necro-green hover:bg-necro-green/20 transition"
                    >
                      View all →
                    </Link>
                  ) : (
                    <button
                      onClick={() =>
                        loadEntryContent(
                          Math.min(logEntries.length - 1, selectedIndex + 1),
                        )
                      }
                      className="px-4 py-2 rounded text-xs uppercase tracking-widest text-necro-green hover:bg-necro-green/20 transition"
                    >
                      Next →
                    </button>
                  )}
                </div>

                {/* Frontmatter Metadata */}
                {Object.keys(selectedFrontmatter).length > 0 && (
                  <div className="rounded-xl border border-vex-border bg-vex-panel p-4 mt-8 space-y-3">
                    <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-3">
                      Entry metadata
                    </p>
                    <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 text-sm">
                      {selectedFrontmatter.title && (
                        <div>
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                            Title
                          </p>
                          <p className="font-fantasy text-white text-sm">
                            {selectedFrontmatter.title}
                          </p>
                        </div>
                      )}
                      {selectedFrontmatter.createdAt && (
                        <div>
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                            Created
                          </p>
                          <p className="text-slate-200 text-sm">
                            {selectedFrontmatter.createdAt}
                          </p>
                        </div>
                      )}
                      {selectedFrontmatter.updatedAt && (
                        <div>
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                            Updated
                          </p>
                          <p className="text-slate-200 text-sm">
                            {selectedFrontmatter.updatedAt}
                          </p>
                        </div>
                      )}
                      {selectedFrontmatter.commit && (
                        <div>
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                            Commit
                          </p>
                          <p className="font-mono text-xs text-necro-green">
                            {selectedFrontmatter.commit.slice(0, 8)}
                          </p>
                        </div>
                      )}
                      {selectedFrontmatter.author && (
                        <div>
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                            Author
                          </p>
                          <p className="text-slate-200 text-sm">
                            {selectedFrontmatter.author}
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Tags */}
                    {Array.isArray(selectedFrontmatter.tags) &&
                      selectedFrontmatter.tags.length > 0 && (
                        <div className="pt-3 border-t border-vex-border/50">
                          <p className="text-[10px] uppercase tracking-[0.3em] text-slate-300 mb-2">
                            Tags
                          </p>
                          <div className="flex flex-wrap gap-2">
                            {selectedFrontmatter.tags.map((tag: string) => (
                              <span
                                key={tag}
                                className="px-2 py-1 rounded text-xs bg-vex-border/40 border border-vex-border/60 text-slate-300 hover:border-necro-green hover:text-necro-green transition"
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                  </div>
                )}

                {/* Raw Markdown Link */}
                <div className="mt-4 text-center">
                  <a
                    href={logEntries[selectedIndex]?.href}
                    className="text-xs text-slate-400 hover:text-necro-green transition"
                  >
                    View raw markdown
                  </a>
                </div>
              </>
            )}

            {!contentLoading && !selectedContent && !logLoading && (
              <div className="text-center text-slate-400 py-12">
                <p>Select an entry to view</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}

function DevUiPage() {
  const uiResources = [
    {
      title: "UI Cheat Sheet",
      description:
        "Quick reference for UI syntax, components, and common patterns.",
      href: "/dev/ui/cheat-sheet.md",
      icon: "📋",
      tags: ["reference", "quick-start"],
    },
    {
      title: "Core Rules",
      description:
        "Foundational UI rules, guardrails, and best practices for consistency.",
      href: "/dev/ui/core-rules.md",
      icon: "📐",
      tags: ["fundamentals", "guidelines"],
    },
    {
      title: "UI Patterns",
      description: "Reusable patterns, layouts, and component compositions.",
      href: "/dev/ui/patterns.md",
      icon: "🧩",
      tags: ["patterns", "components"],
    },
  ];

  return (
    <main id="main-content" className="py-24 bg-vex-dark">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <header className="space-y-4">
          <p className="text-necro-green text-xs uppercase tracking-[0.3em]">
            UI Grimoire
          </p>
          <h1 className="font-fantasy text-4xl text-white">
            Interface Patterns &amp; Cheatsheets
          </h1>
          <p className="text-slate-400 max-w-2xl">
            Reference pages for Vex UI patterns, rules, and reusable components.
            Everything you need to build consistent, maintainable interfaces.
          </p>
        </header>

        <div className="grid gap-6 md:grid-cols-3">
          {uiResources.map((resource) => (
            <div
              key={resource.href}
              className="group rounded-2xl border border-vex-border bg-vex-surface/70 p-6 hover:border-necro-green transition-all duration-300"
            >
              <div className="space-y-4">
                <div className="flex items-start justify-between">
                  <div className="text-4xl">{resource.icon}</div>
                  <div className="flex gap-2">
                    {resource.tags.map((tag) => (
                      <span
                        key={tag}
                        className="text-xs px-2 py-1 rounded bg-vex-surface border border-vex-border text-slate-400"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  {renderResourceLink(
                    resource.href,
                    resource.title,
                    "text-xl font-fantasy text-white group-hover:text-necro-green transition inline-block",
                  )}
                  <p className="text-sm text-slate-400">
                    {resource.description}
                  </p>
                </div>

                <div className="pt-2">
                  {renderResourceLink(
                    resource.href,
                    "View Reference →",
                    "text-sm text-necro-green hover:text-ancient-gold transition",
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>

        <section className="rounded-2xl border border-vex-border bg-vex-surface/70 p-8 space-y-6">
          <div className="space-y-2">
            <h2 className="text-2xl font-fantasy text-white">What's Inside</h2>
            <p className="text-slate-400">
              The UI Grimoire provides comprehensive guidance for building
              Vex-themed interfaces:
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2">
            <div className="space-y-3">
              <h3 className="text-lg font-semibold text-necro-green">
                📋 Cheat Sheet
              </h3>
              <ul className="space-y-2 text-sm text-slate-300">
                <li>• Component syntax reference</li>
                <li>• Color palette & typography</li>
                <li>• Common UI patterns</li>
                <li>• Code snippets & examples</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-lg font-semibold text-necro-green">
                📐 Core Rules
              </h3>
              <ul className="space-y-2 text-sm text-slate-300">
                <li>• Design principles & constraints</li>
                <li>• Accessibility guidelines</li>
                <li>• Naming conventions</li>
                <li>• State management patterns</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-lg font-semibold text-necro-green">
                🧩 UI Patterns
              </h3>
              <ul className="space-y-2 text-sm text-slate-300">
                <li>• Layout compositions</li>
                <li>• Navigation structures</li>
                <li>• Form patterns</li>
                <li>• Data visualization</li>
              </ul>
            </div>

            <div className="space-y-3">
              <h3 className="text-lg font-semibold text-necro-green">
                🎨 Design System
              </h3>
              <ul className="space-y-2 text-sm text-slate-300">
                <li>• Theme tokens & variables</li>
                <li>• Component library</li>
                <li>• Icon sets</li>
                <li>• Animation guidelines</li>
              </ul>
            </div>
          </div>
        </section>

        <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6">
          <h2 className="text-xl font-fantasy text-white mb-4">
            Related Resources
          </h2>
          <div className="space-y-2">
            <a
              href="/dev/logs"
              className="block text-necro-green hover:text-ancient-gold transition"
            >
              📝 Development Logs →
            </a>
            <a
              href="/dev/hosting"
              className="block text-necro-green hover:text-ancient-gold transition"
            >
              🚀 Hosting & Deployment →
            </a>
            <a
              href="https://github.com/mbround18/hytale-vex-lich-dungeon"
              className="block text-necro-green hover:text-ancient-gold transition"
              target="_blank"
              rel="noopener noreferrer"
            >
              📦 GitHub Repository →
            </a>
          </div>
        </div>
      </div>
    </main>
  );
}
function DevHostingPage() {
  return (
    <main id="main-content" className="py-24 bg-vex-dark">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <header className="space-y-4">
          <p className="text-necro-green text-xs uppercase tracking-[0.3em]">
            Hosting
          </p>
          <h1 className="font-fantasy text-4xl text-white">
            Deployment &amp; Hosting Notes
          </h1>
          <p className="text-slate-400">
            Live notes and runbooks for hosting the Vex stack.
          </p>
        </header>

        <section className="space-y-6">
          <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6 space-y-4">
            <h2 className="text-2xl font-fantasy text-white">
              Docker Deployment
            </h2>
            <p className="text-slate-300">
              The Vex Lich Dungeon stack runs via Docker Compose with Hytale
              server, database, and web dashboard services.
            </p>
            <div className="space-y-2">
              <h3 className="text-lg font-semibold text-necro-green">
                Quick Start
              </h3>
              <pre className="bg-vex-dark p-4 rounded-lg overflow-x-auto">
                <code className="text-sm text-slate-300">
                  {`# Clone and start services
docker compose up -d

# View logs
docker compose logs -f hytale-server

# Stop services
docker compose down`}
                </code>
              </pre>
            </div>
          </div>

          <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6 space-y-4">
            <h2 className="text-2xl font-fantasy text-white">
              Server Configuration
            </h2>
            <p className="text-slate-300">
              Configuration files are located in{" "}
              <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                data/server/
              </code>
            </p>
            <ul className="list-disc list-inside space-y-2 text-slate-300 ml-4">
              <li>
                Server properties:{" "}
                <code className="text-necro-green">server.properties</code>
              </li>
              <li>
                Plugin configs:{" "}
                <code className="text-necro-green">plugins/*/config.yml</code>
              </li>
              <li>
                World data: <code className="text-necro-green">worlds/</code>
              </li>
            </ul>
          </div>

          <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6 space-y-4">
            <h2 className="text-2xl font-fantasy text-white">Monitoring</h2>
            <div className="space-y-3">
              <div>
                <h3 className="text-lg font-semibold text-necro-green mb-2">
                  Health Checks
                </h3>
                <ul className="list-disc list-inside space-y-1 text-slate-300 ml-4">
                  <li>
                    Server status:{" "}
                    <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                      http://localhost:25565/status
                    </code>
                  </li>
                  <li>
                    Dashboard:{" "}
                    <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                      http://localhost:5173
                    </code>
                  </li>
                  <li>
                    API endpoints:{" "}
                    <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                      http://localhost:8080/api
                    </code>
                  </li>
                </ul>
              </div>
              <div>
                <h3 className="text-lg font-semibold text-necro-green mb-2">
                  Logs
                </h3>
                <p className="text-slate-300">
                  Server logs are written to{" "}
                  <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                    data/server/logs/
                  </code>{" "}
                  and telemetry events to{" "}
                  <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                    telemetry_dump_*.json
                  </code>
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6 space-y-4">
            <h2 className="text-2xl font-fantasy text-white">
              Troubleshooting
            </h2>
            <div className="space-y-3">
              <div>
                <h3 className="text-lg font-semibold text-necro-green">
                  Common Issues
                </h3>
                <dl className="mt-2 space-y-3">
                  <div>
                    <dt className="font-medium text-white">Port conflicts</dt>
                    <dd className="text-slate-300 mt-1">
                      Check if ports 25565, 5173, or 8080 are already in use
                      with{" "}
                      <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                        lsof -i :25565
                      </code>
                    </dd>
                  </div>
                  <div>
                    <dt className="font-medium text-white">
                      Permission errors
                    </dt>
                    <dd className="text-slate-300 mt-1">
                      Ensure data directories have correct ownership:{" "}
                      <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                        chown -R 1000:1000 data/
                      </code>
                    </dd>
                  </div>
                  <div>
                    <dt className="font-medium text-white">Build failures</dt>
                    <dd className="text-slate-300 mt-1">
                      Clean build artifacts:{" "}
                      <code className="text-necro-green bg-vex-dark px-2 py-1 rounded">
                        ./gradlew clean build
                      </code>
                    </dd>
                  </div>
                </dl>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-vex-border bg-vex-surface/70 p-6">
            <h2 className="text-2xl font-fantasy text-white mb-4">
              Additional Resources
            </h2>
            <div className="space-y-2">
              <a
                href="https://github.com/mbround18/hytale-vex-lich-dungeon"
                className="block text-necro-green hover:text-ancient-gold transition"
                target="_blank"
                rel="noopener noreferrer"
              >
                📦 GitHub Repository →
              </a>
              <a
                href="/dev/logs"
                className="block text-necro-green hover:text-ancient-gold transition"
              >
                📝 Development Logs →
              </a>
              <a
                href="https://hub.docker.com/r/mbround18/hytale-vex-lich-dungeon"
                className="block text-necro-green hover:text-ancient-gold transition"
                target="_blank"
                rel="noopener noreferrer"
              >
                🐳 Docker Hub →
              </a>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}

function DevLogsPage() {
  const [files, setFiles] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const res = await fetch("/dev/logs/index.json");
        if (!res.ok) {
          throw new Error("Failed to load logs");
        }
        const list = (await res.json()) as string[];
        if (!cancelled) {
          setFiles(list);
        }
      } catch {
        if (!cancelled) {
          setFiles([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <main id="main-content" className="py-24 bg-vex-dark">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
        <header className="space-y-4">
          <p className="text-necro-green text-xs uppercase tracking-[0.3em]">
            Dev Logs
          </p>
          <h1 className="font-fantasy text-4xl text-white">
            Recent build notes
          </h1>
          <p className="text-slate-400">
            Chronological updates from the dungeon engineering team.
          </p>
        </header>

        <div className="grid gap-4 md:grid-cols-2">
          {(loading ? Array.from({ length: 6 }) : files).map((file, index) => (
            <div
              key={file ?? `loading-${index}`}
              className="rounded-xl border border-vex-border bg-vex-surface/60 p-4"
            >
              {file ? (
                <>
                  <p className="text-[11px] uppercase tracking-[0.3em] text-slate-500">
                    {parseLogDate(file) ?? "Dev Log"}
                  </p>
                  <h3 className="font-fantasy text-lg text-white mt-2">
                    {formatLogTitle(file)}
                  </h3>
                  <div className="mt-3 flex gap-3 text-xs text-slate-400">
                    <Link
                      to={`/md/dev/logs/${encodeURIComponent(file)}`}
                      className="text-necro-green hover:text-ancient-gold transition"
                    >
                      Open post →
                    </Link>
                    <a
                      href={`/dev/logs/${file}`}
                      className="hover:text-necro-green transition"
                    >
                      Raw markdown
                    </a>
                  </div>
                </>
              ) : (
                <div className="animate-pulse space-y-3">
                  <div className="h-3 w-20 bg-vex-border/60 rounded" />
                  <div className="h-5 w-40 bg-vex-border/60 rounded" />
                  <div className="h-3 w-5/6 bg-vex-border/40 rounded" />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}

function MarkdownPage() {
  const { "*": pathSlug } = useParams();
  const [content, setContent] = useState("");
  const [frontmatter, setFrontmatter] = useState<FrontmatterData>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const backTarget = pathSlug?.startsWith("dev/ui/")
    ? "/dev/ui"
    : pathSlug?.startsWith("dev/logs/")
      ? "/dev/logs"
      : "/dev";

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      if (!pathSlug) {
        setError("Missing markdown path");
        setLoading(false);
        return;
      }
      const safePath = pathSlug.replace(/\.\.+/g, "");
      try {
        const res = await fetch(`/${safePath}`);
        if (!res.ok) {
          throw new Error("Not found");
        }
        const text = await res.text();
        if (!cancelled) {
          const { frontmatter: fm, markdown: md } = parseFrontmatter(text);
          setFrontmatter(fm);
          setContent(md);
          setError(null);
        }
      } catch {
        if (!cancelled) {
          setError("Markdown not found");
          setContent("");
          setFrontmatter({});
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [pathSlug]);

  return (
    <main id="main-content" className="py-24 bg-vex-dark">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <div className="flex items-center gap-4">
          <Link
            to={backTarget}
            className="text-xs text-slate-400 hover:text-necro-green transition"
          >
            ← Back to hub
          </Link>
          {pathSlug && (
            <a
              href={`/${pathSlug}`}
              className="text-xs text-slate-400 hover:text-necro-green transition"
            >
              View raw markdown
            </a>
          )}
        </div>

        {loading && (
          <div className="animate-pulse space-y-4">
            <div className="h-6 w-40 bg-vex-border/60 rounded" />
            <div className="h-4 w-3/4 bg-vex-border/40 rounded" />
            <div className="h-4 w-full bg-vex-border/40 rounded" />
          </div>
        )}
        {error && !loading && (
          <div className="rounded-xl border border-vex-border bg-vex-surface/60 p-6 text-slate-300">
            {error}
          </div>
        )}
        {!loading && !error && (
          <>
            {/* Content */}
            <article className="markdown-body rounded-2xl border border-vex-border bg-vex-surface/70 p-6 prose prose-invert prose-headings:font-fantasy prose-a:text-necro-green max-w-none">
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={{
                  a: ({ href = "", children, ...props }) => {
                    if (!href) {
                      return <span {...props}>{children}</span>;
                    }
                    if (isExternal(href)) {
                      return (
                        <a
                          href={href}
                          target="_blank"
                          rel="noopener noreferrer"
                          {...props}
                        >
                          {children}
                        </a>
                      );
                    }
                    if (href.startsWith("/")) {
                      const target = href.endsWith(".md") ? `/md${href}` : href;
                      return (
                        <Link to={target} {...props}>
                          {children}
                        </Link>
                      );
                    }
                    return (
                      <a href={href} {...props}>
                        {children}
                      </a>
                    );
                  },
                }}
              >
                {content}
              </ReactMarkdown>
            </article>

            {/* Frontmatter Metadata */}
            {Object.keys(frontmatter).length > 0 && (
              <div className="rounded-2xl border border-vex-border bg-vex-panel p-6 space-y-4">
                <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-3">
                  Metadata
                </p>
                <div className="grid gap-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                  {frontmatter.title && (
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                        Title
                      </p>
                      <p className="text-sm font-fantasy text-white">
                        {frontmatter.title}
                      </p>
                    </div>
                  )}
                  {frontmatter.createdAt && (
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                        Created
                      </p>
                      <p className="text-sm text-slate-200">
                        {frontmatter.createdAt}
                      </p>
                    </div>
                  )}
                  {frontmatter.updatedAt && (
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                        Updated
                      </p>
                      <p className="text-sm text-slate-200">
                        {frontmatter.updatedAt}
                      </p>
                    </div>
                  )}
                  {frontmatter.commit && (
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                        Commit
                      </p>
                      <p className="font-mono text-xs text-necro-green">
                        {frontmatter.commit.slice(0, 8)}
                      </p>
                    </div>
                  )}
                  {frontmatter.author && (
                    <div>
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-1">
                        Author
                      </p>
                      <p className="text-sm text-slate-200">
                        {frontmatter.author}
                      </p>
                    </div>
                  )}
                </div>

                {/* Tags */}
                {Array.isArray(frontmatter.tags) &&
                  frontmatter.tags.length > 0 && (
                    <div className="pt-4 border-t border-vex-border/50">
                      <p className="text-[11px] uppercase tracking-[0.3em] text-slate-300 mb-3">
                        Tags
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {frontmatter.tags.map((tag: string) => (
                          <span
                            key={tag}
                            className="px-3 py-1 rounded-full bg-vex-border/40 border border-vex-border/60 text-xs text-slate-300 hover:border-necro-green hover:text-necro-green transition"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}

function NotFound() {
  return (
    <main
      id="main-content"
      className="min-h-screen bg-vex-dark text-slate-200 flex items-center justify-center"
    >
      <div className="text-center space-y-4">
        <p className="font-fantasy text-4xl text-ancient-gold">
          Lost in the catacombs
        </p>
        <p className="text-slate-400">That page does not exist.</p>
        <Link
          to="/"
          className="inline-flex items-center justify-center px-4 py-2 rounded-full bg-vex-deep text-white font-fantasy text-xs uppercase tracking-widest border border-necro-green/40 hover:bg-necro-green hover:text-vex-dark transition"
        >
          Return home
        </Link>
      </div>
    </main>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Shell />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/dev" element={<DevHubPage />} />
          <Route path="/dev/ui" element={<DevUiPage />} />
          <Route path="/dev/ui/" element={<Navigate to="/dev/ui" replace />} />
          <Route
            path="/dev/ui/index.html"
            element={<Navigate to="/dev/ui" replace />}
          />
          <Route path="/dev/hosting" element={<DevHostingPage />} />
          <Route path="/dev/logs" element={<DevLogsPage />} />
          <Route path="/md/*" element={<MarkdownPage />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
