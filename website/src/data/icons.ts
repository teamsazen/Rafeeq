// Inline SVGs shared by the page and its components.
export const playSvg = `<svg viewBox="0 0 24 24" aria-hidden="true"><path class="pl-b" d="M3.2 1.8 13.4 12 3.2 22.2c-.3-.2-.5-.6-.5-1.1V2.9c0-.5.2-.9.5-1.1z"/><path class="pl-g" d="M3.2 1.8c.4-.3 1-.3 1.6 0L16.6 8.6 13.4 12z"/><path class="pl-r" d="M13.4 12l3.2 3.4-11.8 6.8c-.6.3-1.2.3-1.6 0z"/><path class="pl-y" d="M16.6 8.6l3.7 2.1c1 .6 1 2 0 2.6l-3.7 2.1-3.2-3.4z"/></svg>`;
export const appleSvg = `<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M16.4 12.6c0-2.6 2.1-3.8 2.2-3.9-1.2-1.8-3.1-2-3.7-2-1.6-.2-3.1.9-3.9.9-.8 0-2-.9-3.4-.9-1.7 0-3.3 1-4.2 2.6-1.8 3.1-.5 7.7 1.3 10.2.9 1.2 1.9 2.6 3.2 2.6 1.3-.1 1.8-.8 3.3-.8 1.6 0 2 .8 3.4.8 1.4 0 2.3-1.3 3.1-2.5 1-1.4 1.4-2.8 1.4-2.9-.1 0-2.7-1-2.7-4.1zM13.9 5.1c.7-.9 1.2-2.1 1.1-3.3-1 0-2.3.7-3 1.6-.7.8-1.2 2-1.1 3.2 1.2.1 2.3-.6 3-1.5z"/></svg>`;
export const githubSvg = `<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 .5a11.5 11.5 0 0 0-3.6 22.4c.6.1.8-.3.8-.6v-2c-3.2.7-3.9-1.5-3.9-1.5-.5-1.3-1.3-1.7-1.3-1.7-1-.7.1-.7.1-.7 1.2.1 1.8 1.2 1.8 1.2 1 1.8 2.8 1.3 3.5 1 .1-.8.4-1.3.7-1.6-2.6-.3-5.3-1.3-5.3-5.7 0-1.3.5-2.3 1.2-3.1-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.2 1.2a11 11 0 0 1 5.8 0c2.2-1.5 3.2-1.2 3.2-1.2.6 1.6.2 2.8.1 3.1.8.8 1.2 1.9 1.2 3.1 0 4.4-2.7 5.4-5.3 5.7.4.4.8 1.1.8 2.2v3.3c0 .3.2.7.8.6A11.5 11.5 0 0 0 12 .5z"/></svg>`;

// Eight-point star: two squares 45° apart, like the app's star pattern.
const starPath = (c: number, outer: number) => {
  const inner = outer * 0.765;
  const pts = Array.from({ length: 16 }, (_, i) => {
    const a = (i * Math.PI) / 8 - Math.PI / 2, r = i % 2 ? inner : outer;
    return `${(c + r * Math.cos(a)).toFixed(1)} ${(c + r * Math.sin(a)).toFixed(1)}`;
  });
  return `<path d="M${pts.join('L')}Z"/>`;
};
export const star = starPath(26, 24);

// Line icons for the feature list (24×24, drawn with currentColor).
const line = (d: string) => `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;
export const icons = {
  home: line('<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 9.5V20h13V9.5"/><path d="M10 20v-5h4v5"/>'),
  namaz: line('<path d="M4 20h16"/><path d="M6 20v-6a6 6 0 0 1 12 0v6"/><path d="M12 4v4"/>'),
  hijri: line('<rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M3.5 10h17M8 3v4M16 3v4"/><path d="M14.5 13.2a2.6 2.6 0 1 0 0 4.6 3.2 3.2 0 0 1 0-4.6z"/>'),
  names: line('<path d="M12 3.5 14.2 8l4.9.7-3.5 3.4.8 4.9L12 14.7 7.6 17l.8-4.9L4.9 8.7 9.8 8z"/>'),
  language: line('<path d="M4 6h9M8.5 4v2M6 6c.5 3 2.5 5.5 5 6.5M11 6c-.5 3-2.5 6-6 7.5"/><path d="M13 20l4-9 4 9M14.5 17h5"/>'),
  theme: line('<circle cx="12" cy="12" r="8"/><path d="M12 4a8 8 0 0 0 0 16z" fill="currentColor"/>'),
};
export const chevron = (dir: 'left' | 'right') => line(dir === 'left' ? '<path d="M15 5l-7 7 7 7"/>' : '<path d="M9 5l7 7-7 7"/>');
