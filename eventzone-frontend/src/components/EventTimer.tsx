import React, { useState, useEffect } from 'react';

type EventTimerProps = {
  targetDate: string | Date;
};

export default function EventTimer({ targetDate }: EventTimerProps) {
  const [timeLeft, setTimeLeft] = useState<string>('');

  useEffect(() => {
    const calculateTime = () => {
      const difference = +new Date(targetDate) - +new Date();
      
      if (difference <= 0) {
        return 'Happening now / Ended';
      }

      const days = Math.floor(difference / (1000 * 60 * 60 * 24));
      const hours = Math.floor((difference / (1000 * 60 * 60)) % 24);
      const minutes = Math.floor((difference / 1000 / 60) % 60);
      const seconds = Math.floor((difference / 1000) % 60);

      // Compact format perfect for placement next to a title
      if (days > 0) {
        return `${days}d ${hours}h ${minutes}m`;
      }
      return `${hours}h ${minutes}m ${seconds}s`;
    };

    // Set initial text immediately
    setTimeLeft(calculateTime());

    // Update every second
    const interval = setInterval(() => {
      setTimeLeft(calculateTime());
    }, 1000);

    return () => clearInterval(interval);
  }, [targetDate]);

  if (timeLeft.includes('Happening now')) return null;

  return (
    <span className="inline-flex items-center rounded bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-700 animate-pulse">
      ⏳ {timeLeft}
    </span>
  );
}
