// "Today's price": Morocco's national average gasoline price, published weekly by
// .github/workflows/fuel-price.yml (format written by scripts/fuel/fetch_price.py).
// Only ever called from a tap on the button — the site makes no request on its own.
import { FUEL_PRICE_URL } from './config.js';

export async function fetchFuelPrice() {
  if (!FUEL_PRICE_URL) throw new Error('Price lookup is turned off');
  const res = await fetch(FUEL_PRICE_URL, { cache: 'no-store', credentials: 'omit', referrerPolicy: 'no-referrer' });
  if (!res.ok) throw new Error(res.status === 404 ? 'No price published yet' : `Price lookup failed (${res.status})`);
  const d = await res.json();
  // Same plausibility band as the scraper: anything else means a broken file, not the market.
  if (d.format !== 'agility-fuel-price' || !(d.pricePerLiter >= 5 && d.pricePerLiter <= 40)) throw new Error('Unexpected price file');
  return d;
}
