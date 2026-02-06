import React, { useMemo, useState } from 'react';
import { Badge } from 'ui-shared/components';
import { navSections, primaryAction } from './data/navigation';
import { homeContent } from './data/home';
import './styles/app.scss';

const isExternal = (href: string) => href.startsWith('http');

const linkProps = (href: string) =>
  isExternal(href)
    ? ({ target: '_blank', rel: 'noreferrer' } as const)
    : ({} as const);

const accentClass: Record<string, string> = {
  purple: 'hover:border-vex-purple',
  gold: 'hover:border-ancient-gold',
  green: 'hover:border-necro-green'
};

export default function App() {
  const [navOpen, setNavOpen] = useState(false);
  const sections = useMemo(() => navSections, []);

  return (
    <div className="min-h-screen bg-vex-dark text-slate-200 antialiased custom-scrollbar selection:bg-necro-green selection:text-vex-dark relative">
      <div
        className="fixed inset-0 opacity-10 pointer-events-none"
        style={{
          backgroundImage: "url('https://www.transparenttextures.com/patterns/black-scales.png')"
        }}
      />

      <nav className="sticky top-0 w-full z-50 bg-vex-surface/90 backdrop-blur-md border-b border-vex-border transition-all duration-300">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <button
              className="flex-shrink-0 flex items-center gap-3 cursor-pointer group"
              onClick={() => {
                if (window.location.pathname !== '/') {
                  window.location.assign('/');
                } else {
                  window.scrollTo({ top: 0, behavior: 'smooth' });
                }
              }}
              type="button"
            >
              <img
                src="https://imagedelivery.net/6QPDh2i4MEi-JY_RW1iPZQ/62e09a2c-0be3-4486-ebe6-4185678d9800/public"
                alt="Vex Logo"
                className="w-12 h-12 rounded-lg border-2 border-ancient-gold shadow-lg transform group-hover:rotate-12 transition duration-300"
              />
              <div>
                <span className="font-fantasy font-bold text-lg tracking-widest text-ancient-gold group-hover:text-necro-green transition duration-300">
                  Vex&apos;s Challenge
                </span>
              </div>
            </button>

            <div className="hidden md:flex items-center gap-8 text-sm font-medium">
              {sections.map((section) => (
                <div key={section.id} className="flex items-center gap-3">
                  <span className="text-[10px] uppercase tracking-[0.2em] text-slate-500">
                    {section.label}
                  </span>
                  <div className="flex items-center gap-3 text-xs text-slate-300">
                    {section.links.map((child) => (
                      <a
                        key={child.href}
                        href={child.href}
                        className="hover:text-necro-green transition"
                        {...linkProps(child.href)}
                      >
                        {child.label}
                      </a>
                    ))}
                  </div>
                </div>
              ))}
              <a
                href={primaryAction.href}
                className="px-4 py-2 rounded-full bg-vex-deep text-white font-fantasy text-xs uppercase tracking-widest border border-necro-green/40 hover:bg-necro-green hover:text-vex-dark transition"
                {...linkProps(primaryAction.href)}
              >
                {primaryAction.label}
              </a>
            </div>

            <div className="md:hidden flex items-center">
              <button
                className="text-ancient-gold hover:text-necro-green"
                aria-controls="mobile-nav"
                aria-expanded={navOpen}
                onClick={() => setNavOpen((prev) => !prev)}
              >
                <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <div
          id="mobile-nav"
          className={`md:hidden border-t border-vex-border bg-vex-surface/95 ${navOpen ? 'block' : 'hidden'}`}
        >
          <div className="px-6 py-5 space-y-6">
            {sections.map((section) => (
              <div key={section.id} className="space-y-3">
                <p className="text-[11px] uppercase tracking-[0.3em] text-slate-500">
                  {section.label}
                </p>
                <div className="grid grid-cols-1 gap-2">
                  {section.links.map((child) => (
                    <a
                      key={child.href}
                      href={child.href}
                      className="block text-sm text-slate-300 hover:text-necro-green transition"
                      onClick={() => setNavOpen(false)}
                      {...linkProps(child.href)}
                    >
                      {child.label}
                    </a>
                  ))}
                </div>
              </div>
            ))}
            <a
              href={primaryAction.href}
              className="block text-center px-4 py-2 rounded-full bg-vex-deep text-white font-fantasy text-xs uppercase tracking-widest border border-necro-green/40 hover:bg-necro-green hover:text-vex-dark transition"
              onClick={() => setNavOpen(false)}
              {...linkProps(primaryAction.href)}
            >
              {primaryAction.label}
            </a>
          </div>
        </div>
      </nav>

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
                      action.variant === 'primary'
                        ? 'px-8 py-4 rounded bg-gradient-to-r from-vex-deep to-vex-purple text-white font-fantasy font-bold text-xl hover:from-necro-green hover:to-necro-dark hover:scale-105 transition duration-300 shadow-lg border border-white/10 text-center flex items-center justify-center gap-2'
                        : 'px-8 py-4 rounded bg-[#FF5E5B] text-white font-fantasy font-bold text-xl hover:bg-[#d44542] hover:scale-105 transition duration-300 shadow-lg border border-white/10 text-center flex items-center justify-center gap-2'
                    }
                    {...linkProps(action.href)}
                  >
                    <span>{action.icon}</span>
                    {action.label}
                  </a>
                ))}
              </div>
              <p className="mt-4 text-sm text-slate-500 font-body italic">{homeContent.hero.note}</p>
            </div>

            <div className="lg:col-span-5 relative flex justify-center">
              <div className="absolute inset-0 bg-gradient-to-tr from-vex-purple to-necro-green opacity-20 rounded-full blur-3xl" />
              <div className="relative w-80 h-96 lg:w-96 lg:h-[30rem] float-animation">
                <div className="absolute inset-0 border-4 border-ancient-gold rounded-3xl transform rotate-3 bg-vex-dark/50 backdrop-blur-sm z-0" />
                <div className="absolute inset-0 border-4 border-stone-600 rounded-3xl transform -rotate-3 bg-vex-dark/80 z-10 flex items-center justify-center overflow-hidden">
                  <div className="text-center p-8">
                    <div className="text-8xl mb-4 drop-shadow-[0_0_15px_rgba(74,222,128,0.8)]">💀</div>
                    <h3 className="font-fantasy text-2xl text-vex-purple">{homeContent.heroPortrait.title}</h3>
                    <p className="text-sm text-slate-400 mt-2 italic">{homeContent.heroPortrait.quote}</p>
                    <div className="mt-4 text-xs text-necro-green border border-necro-green/30 rounded px-2 py-1 inline-block">
                      {homeContent.heroPortrait.bossLevel}
                    </div>
                    <div className="mt-6 text-xs text-slate-500">{homeContent.heroPortrait.hint}</div>
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

      <section id={homeContent.lore.id} className="py-20 bg-stone-900 relative border-t border-slate-800">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <h2 className="font-fantasy text-4xl text-ancient-gold mb-8">{homeContent.lore.title}</h2>
          <div className="mx-auto">
            {homeContent.lore.paragraphs.map((paragraph, index) => (
              <p key={`lore-${index}`} className="text-slate-300 leading-8 mb-6">
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
                <h3 className="font-fantasy text-xl text-white mb-2">{card.title}</h3>
                <p className="text-slate-400 text-sm">{card.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id={homeContent.dungeon.id} className="py-24 bg-vex-dark relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="font-fantasy text-4xl text-white mb-4">{homeContent.dungeon.title}</h2>
            <p className="text-slate-400">{homeContent.dungeon.subtitle}</p>
          </div>

          <div className="grid md:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              {homeContent.dungeon.features.map((feature) => (
                <div
                  key={feature.step}
                  className={`flex gap-4 ${feature.status === 'coming' ? 'opacity-75' : ''}`}
                >
                  <div
                    className={
                      feature.status === 'coming'
                        ? 'flex-shrink-0 w-12 h-12 bg-stone-800 rounded border border-slate-600 flex items-center justify-center text-xl font-bold text-slate-400'
                        : 'flex-shrink-0 w-12 h-12 bg-vex-deep rounded border border-vex-purple flex items-center justify-center text-xl font-bold text-white'
                    }
                  >
                    {feature.step}
                  </div>
                  <div>
                    <h3
                      className={
                        feature.status === 'coming'
                          ? 'font-fantasy text-2xl text-slate-300 mb-2'
                          : 'font-fantasy text-2xl text-necro-green mb-2'
                      }
                    >
                      {feature.title}
                      {feature.status === 'coming' && (
                        <span className="ml-2">
                          <Badge variant="gold">Coming Soon</Badge>
                        </span>
                      )}
                    </h3>
                    <p className={feature.status === 'coming' ? 'text-slate-500' : 'text-slate-400'}>
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
                  backgroundImage: 'radial-gradient(#4c1d95 1px, transparent 1px)',
                  backgroundSize: '20px 20px',
                  opacity: 0.2
                }}
              />
            </div>
          </div>
        </div>
      </section>

      <section id={homeContent.callToAction.id} className="py-20 relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-vex-dark to-vex-deep opacity-80" />
        <div className="max-w-4xl mx-auto px-4 relative z-10 text-center">
          <h2 className="font-fantasy text-5xl text-ancient-gold mb-6 text-glow">
            {homeContent.callToAction.title}
          </h2>
          <p className="text-xl text-slate-300 mb-10">{homeContent.callToAction.subtitle}</p>

          <div className="bg-stone-900/80 p-8 rounded-2xl border border-ancient-gold/30 backdrop-blur-sm max-w-lg mx-auto">
            <div className="flex flex-col gap-4">
              <a
                href={homeContent.callToAction.actions[0].href}
                className="w-full py-4 rounded bg-necro-green hover:bg-necro-dark text-vex-dark font-fantasy font-bold text-xl transition duration-300 flex items-center justify-center gap-2"
                {...linkProps(homeContent.callToAction.actions[0].href)}
              >
                <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
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
            <span className="font-fantasy text-xl text-slate-500">{homeContent.footer.title}</span>
          </div>
          <p className="text-slate-600 text-sm mb-8">{homeContent.footer.subtitle}</p>
          <div className="flex justify-center space-x-6 text-slate-500">
            {homeContent.footer.links.map((link) => (
              <a key={link.label} href={link.href} className="hover:text-vex-purple transition">
                {link.label}
              </a>
            ))}
          </div>
        </div>
      </footer>
    </div>
  );
}
