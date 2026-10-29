import { defineConfig } from 'astro/config';

// `site` feeds canonical URLs and social preview links.
export default defineConfig({
  site: 'https://rafeeq.teamsazen.com',
  output: 'static',
  trailingSlash: 'never',
  build: { format: 'file' },
  prefetch: { prefetchAll: true, defaultStrategy: 'viewport' },
  image: {
    responsiveStyles: true,
  },
});
