import { useState } from 'react';

const FALLBACK_SRC = '/images/events/placeholder.svg';

interface CoverImageProps {
  src: string | null;
  alt: string;
  className?: string;
  loading?: 'lazy' | 'eager';
}

export default function CoverImage({ src, alt, className, loading }: CoverImageProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const resolved = !src || failedSrc === src ? FALLBACK_SRC : src;

  return <img src={resolved} alt={alt} className={className} loading={loading} onError={() => setFailedSrc(src)} />;
}
