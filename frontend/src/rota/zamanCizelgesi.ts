import type { RotaBacagi } from '../api/tipler'
import type { SiraliNokta } from './duraklar'

export function yuvarlanmisKalkis(zaman = new Date()): string {
  const yuvarlanmis = new Date(Math.ceil(zaman.getTime() / 300000) * 300000)
  return `${String(yuvarlanmis.getHours()).padStart(2, '0')}:${String(yuvarlanmis.getMinutes()).padStart(2, '0')}`
}

export function kalkisZamani(saat: string, bugun = new Date()): Date {
  const [saatSayi, dakika] = saat.split(':').map(Number)
  const zaman = new Date(bugun)
  zaman.setHours(saatSayi, dakika, 0, 0)
  return zaman
}

export function zamanCizelgesi(kalkis: Date, noktalar: SiraliNokta[], bacaklar: RotaBacagi[]) {
  let zaman = kalkis.getTime()
  return noktalar.map((nokta, sira) => {
    zaman += (bacaklar[sira]?.sureSn ?? 0) * 1000
    const varis = new Date(zaman)
    zaman += (nokta.durak?.kalisDakika ?? 0) * 60000
    return { nokta, varis }
  })
}

export function saatMetni(zaman: Date): string {
  return zaman.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}
