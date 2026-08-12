import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    'intro',
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/quickstart',
        'getting-started/configuration',
      ],
    },
    {
      type: 'category',
      label: 'Architecture & Design',
      collapsed: false,
      items: [
        'architecture/overview',
        'architecture/memory-model',
        'architecture/sstable-format',
        'architecture/concurrency-design',
      ],
    },
    {
      type: 'category',
      label: 'Performance & Optimization',
      collapsed: false,
      items: [
        'benchmarks/performance',
      ],
    },
    {
      type: 'category',
      label: 'Status & Roadmap',
      collapsed: false,
      items: [
        'roadmap/limitations-roadmap',
      ],
    },
    {
      type: 'category',
      label: 'API Reference',
      collapsed: false,
      items: [
        'api-reference/java-api',
      ],
    },
  ],
};

export default sidebars;
