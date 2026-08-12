import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import CodeBlock from '@theme/CodeBlock';

import styles from './index.module.css';

interface FeatureItem {
  title: string;
  icon: string;
  description: ReactNode;
}

const FeatureList: FeatureItem[] = [
  {
    title: '53.2M+ Ops/Sec MemTable Ingest',
    icon: '⚡',
    description: (
      <>
        Designed for extreme throughput, achieving <strong>53,214,262 ops/sec</strong> in JMH single-threaded off-heap MemTable record write benchmarks.
      </>
    ),
  },
  {
    title: 'High-Speed SSTable Serialization',
    icon: '💾',
    description: (
      <>
        Background disk serialization flushes <strong>84.219 complete SSTable files/sec</strong> for 100,000-record table batches.
      </>
    ),
  },
  {
    title: 'Java FFM & Off-Heap Memory',
    icon: '🚀',
    description: (
      <>
        Uses JDK 22+ Foreign Function & Memory (FFM) API (<code>MemorySegment</code> and <code>Arena</code>)
        to store binary record payloads natively off-heap, significantly reducing JVM Garbage Collection pressure and pause times.
      </>
    ),
  },
  {
    title: 'LSM-Tree Storage Pipeline',
    icon: '📦',
    description: (
      <>
        Features concurrent SkipList index mapping, active and immutable MemTables, background asynchronous flushing,
        and multi-way merge compaction.
      </>
    ),
  },
];

function Feature({title, icon, description}: FeatureItem) {
  return (
    <div className={clsx('col col--6 margin-bottom--lg')}>
      <div className="featureCard">
        <div className="featureIcon">{icon}</div>
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <div className="badge--version margin-right--sm">
          🏷️ Version v0.1.0 (Experimental)
        </div>
        <div className="badge--perf margin-bottom--md">
          ⚠️ Experimental Status: APIs & Storage Formats May Change
        </div>
        <br />
        <div className="badge--perf">
          🔥 JMH Scores: 53.2M ops/sec MemTable Put | 84.2 ops/sec 100K SSTable Flush
        </div>
        <Heading as="h1" className="heroBannerTitle margin-top--md">
          {siteConfig.title} <span style={{fontSize: '2rem', verticalAlign: 'middle', color: '#ff6d00'}}>v0.1.0</span>
        </Heading>
        <p className="heroBannerSubtitle">
          TangoDB v0.1.0 is a high-performance embedded key-value storage engine built from scratch in modern Java.
        </p>
        <div className={styles.buttons}>
          <Link
            className="button button--primary button--lg margin-right--md"
            to="/docs/intro">
            Get Started 🚀
          </Link>
          <Link
            className="button button--secondary button--lg margin-right--md"
            to="/docs/architecture/overview">
            Explore Architecture ⚙️
          </Link>
          <Link
            className="button button--outline button--lg"
            to="/docs/benchmarks/performance">
            Benchmark Results 📊
          </Link>
        </div>

        <div className="codeTeaser">
          <CodeBlock language="java" title="TangoDB v0.1.0 Quick Example">
{`// Initialize configuration with FFM native off-heap Arena
TangoConfig config = TangoConfig.builder()
        .sstableDirectory(Path.of("data"))
        .memTableSize(64L * 1024 * 1024) // 64 MiB MemTable
        .build();

// Open database instance & execute operations
try (TangoDB db = TangoDB.open(config)) {
    db.put(bytes("user:1001"), bytes("{\"name\":\"Alice\"}"));
    
    byte[] value = db.get(bytes("user:1001"));
    System.out.println("Retrieved: " + new String(value));
    
    db.delete(bytes("user:1001"));
}`}
          </CodeBlock>
        </div>
      </div>
    </header>
  );
}

export default function Home(): ReactNode {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} v0.1.0 - High-Performance Embedded Key-Value Storage`}
      description="TangoDB v0.1.0 experimental high-performance embedded key-value storage engine built from scratch in modern Java using FFM API and off-heap memory.">
      <HomepageHeader />
      <main>
        <section className={styles.features}>
          <div className="container">
            <div className="row">
              {FeatureList.map((props, idx) => (
                <Feature key={idx} {...props} />
              ))}
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
