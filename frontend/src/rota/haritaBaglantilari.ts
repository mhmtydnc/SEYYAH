import type { Konum } from '../api/tipler'

function koordinat(nokta: Pick<Konum, 'enlem' | 'boylam'>): string {
  return `${nokta.enlem},${nokta.boylam}`
}

export function gitBaglantisi(nokta: Pick<Konum, 'enlem' | 'boylam'>, kullaniciAraci: string): string {
  if (/iPad|iPhone|iPod/.test(kullaniciAraci)) return `https://maps.apple.com/?daddr=${koordinat(nokta)}&dirflg=d`
  return `https://www.google.com/maps/dir/?api=1&destination=${koordinat(nokta)}&travelmode=driving`
}

export function tumRotaBaglantisi(duraklar: Pick<Konum, 'enlem' | 'boylam'>[], varis: Pick<Konum, 'enlem' | 'boylam'>): string {
  const sorgu = new URLSearchParams({ api: '1', origin: 'My Location', destination: koordinat(varis), travelmode: 'driving' })
  if (duraklar.length) sorgu.set('waypoints', duraklar.slice(0, 9).map(koordinat).join('|'))
  return `https://www.google.com/maps/dir/?${sorgu.toString()}`
}
