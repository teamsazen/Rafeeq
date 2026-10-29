// Landing page interactions. Everything is an enhancement: without script every
// feature panel and the Qibla video (with its own controls) still show.
document.documentElement.classList.add('js');
const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
const behavior = reduce ? 'auto' : 'smooth';
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

/* ================= Feature guide: tabs ================= */
(() => {
  const guide = $('[data-guide]'); if (!guide) return;
  const list = $('.guide__tabs', guide), tabs = $$('[role="tab"]', list), panels = $$('[role="tabpanel"]', guide);
  list.hidden = false;
  const select = (tab, focus) => {
    tabs.forEach((t) => { const on = t === tab; t.setAttribute('aria-selected', on); t.tabIndex = on ? 0 : -1; });
    panels.forEach((p) => { p.hidden = p.id !== tab.getAttribute('aria-controls'); });
    if (focus) { tab.focus(); tab.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior }); }
  };
  tabs.forEach((t, i) => {
    t.addEventListener('click', () => select(t));
    t.addEventListener('keydown', (e) => {
      const n = { ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 }[e.key];
      if (e.key === 'Home') { e.preventDefault(); select(tabs[0], true); }
      else if (e.key === 'End') { e.preventDefault(); select(tabs[tabs.length - 1], true); }
      else if (n) { e.preventDefault(); select(tabs[(i + n + tabs.length) % tabs.length], true); }
    });
  });
  select(tabs[0]);
})();

/* ================= Qibla video: plays only when asked ================= */
(() => {
  const box = $('[data-player]'); if (!box) return;
  const video = $('video', box), start = $('.player__start', box);
  video.controls = false; start.hidden = false;
  const play = () => {
    video.controls = true; start.hidden = true; box.setAttribute('data-playing', '');
    if (video.textTracks[0]) video.textTracks[0].mode = 'showing';
    video.play().catch(() => {});
  };
  start.addEventListener('click', play);
  video.addEventListener('ended', () => box.removeAttribute('data-playing'));
  $$('[data-play]').forEach((a) => a.addEventListener('click', (e) => {
    e.preventDefault();
    $('#qibla').scrollIntoView({ behavior, block: 'center' });
    play();
  }));
})();
