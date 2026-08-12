import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'TangoDB',
  tagline: 'High-Performance Embedded Key-Value Storage Engine in Modern Java',
  favicon: 'img/logo.svg',

  future: {
    v4: true,
  },

  url: 'https://gokul-ceo.github.io',
  baseUrl: process.env.BASE_URL || '/tangodb-site/',

  organizationName: 'gokul-ceo',
  projectName: 'tangodb-site',

  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/gokul-ceo/TangoDB/tree/main/tangodb-site/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/logo.svg',
    colorMode: {
      defaultMode: 'dark',
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: '',
      logo: {
        alt: 'TangoDB Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          to: '/docs/architecture/overview',
          label: 'Architecture',
          position: 'left',
        },
        {
          to: '/docs/benchmarks/performance',
          label: 'Benchmarks',
          position: 'left',
        },
        {
          to: '/docs/roadmap/limitations-roadmap',
          label: 'Roadmap & Limitations',
          position: 'left',
        },
        {
          to: '/docs/api-reference/java-api',
          label: 'API Reference',
          position: 'left',
        },
        {
          href: 'https://github.com/gokul-ceo/TangoDB',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {
              label: 'Overview & Status',
              to: '/docs/intro',
            },
            {
              label: 'Quickstart Guide',
              to: '/docs/getting-started/quickstart',
            },
            {
              label: 'Configuration',
              to: '/docs/getting-started/configuration',
            },
          ],
        },
        {
          title: 'Architecture & Design',
          items: [
            {
              label: 'Storage Engine Design',
              to: '/docs/architecture/overview',
            },
            {
              label: 'Memory Model & FFM',
              to: '/docs/architecture/memory-model',
            },
            {
              label: 'SSTable Format',
              to: '/docs/architecture/sstable-format',
            },
            {
              label: 'Concurrency & Design Rationale',
              to: '/docs/architecture/concurrency-design',
            },
          ],
        },
        {
          title: 'Performance & Future',
          items: [
            {
              label: 'JMH Benchmarks',
              to: '/docs/benchmarks/performance',
            },
            {
              label: 'Limitations & Roadmap',
              to: '/docs/roadmap/limitations-roadmap',
            },
            {
              label: 'GitHub Repository',
              href: 'https://github.com/gokul-ceo/TangoDB',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} TangoDB (Experimental). Built with Docusaurus and Modern Java.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'bash', 'json', 'markup'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
