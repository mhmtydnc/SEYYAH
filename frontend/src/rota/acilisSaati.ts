import OpeningHours from 'opening_hours'
import { saatMetni } from './zamanCizelgesi'

export type AcilisDurumu = 'acik' | 'kapali' | 'kalisSirasindaKapaniyor' | 'bilinmiyor'

export function durum(calismaSaatleri: string | null | undefined, varis: Date, kalisDakika: number): { durum: AcilisDurumu; saat: string | null } {
  const bilinmiyor = { durum: 'bilinmiyor' as const, saat: null }
  if (!calismaSaatleri?.trim() || !Number.isFinite(varis.getTime()) || !Number.isFinite(kalisDakika) || kalisDakika < 0) return bilinmiyor
  try {
    const saatler = new OpeningHours(calismaSaatleri)
    if (saatler.getUnknown(varis)) return bilinmiyor
    const sonraki = saatler.getNextChange(varis)
    if (!saatler.getState(varis)) {
      return { durum: 'kapali', saat: sonraki && saatler.getState(sonraki) && !saatler.getUnknown(sonraki) ? saatMetni(sonraki) : null }
    }
    const ayrilis = new Date(varis.getTime() + kalisDakika * 60000)
    if (sonraki && sonraki < ayrilis && !saatler.getState(sonraki) && !saatler.getUnknown(sonraki)) {
      return { durum: 'kalisSirasindaKapaniyor', saat: saatMetni(sonraki) }
    }
    return { durum: 'acik', saat: null }
  } catch { return bilinmiyor }
}

export function uyariMetni(calismaSaatleri: string | null | undefined, varis: Date, kalisDakika: number): string | null {
  const sonuc = durum(calismaSaatleri, varis, kalisDakika)
  if (sonuc.durum === 'kapali') return `Varışta kapalı olabilir${sonuc.saat ? ` (açılış ${sonuc.saat})` : ''}`
  if (sonuc.durum === 'kalisSirasindaKapaniyor') return `Kalışın sırasında kapanıyor (${sonuc.saat})`
  return null
}
