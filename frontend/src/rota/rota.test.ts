import { describe, expect, it } from 'vitest'
import type { Rota } from '../api/tipler'
import { duraklariSirala, rotaNoktalari, type SeciliDurak } from './duraklar'
import { varsayilanKalisSuresi } from './kalisSureleri'
import { kalkisZamani, yuvarlanmisKalkis, zamanCizelgesi } from './zamanCizelgesi'
import { gitBaglantisi, tumRotaBaglantisi } from './haritaBaglantilari'

const rota: Rota = {
  sira: 1, ad: 'Aksaray üzerinden', uzerinden: 'Aksaray', araNokta: { ad: 'Aksaray', enlem: 0, boylam: 5 },
  mesafeM: 100, sureSn: 100, geometri: [[0, 0], [10, 0]],
  yerler: { gezi: [
    { id: 1, ad: 'Bir', kategori: 'museum', tur: 'gezi', enlem: 0, boylam: 8, yolOrani: 0.8, yolaUzaklikM: 0, ucret: null, calismaSaatleri: null, wikidataId: null, website: null },
    { id: 2, ad: 'İki', kategori: 'park', tur: 'gezi', enlem: 0, boylam: 2, yolOrani: 0.2, yolaUzaklikM: 0, ucret: null, calismaSaatleri: null, wikidataId: null, website: null },
  ], mola: [], destek: [] },
}
const duraklar: SeciliDurak[] = [
  { yer: { id: 1, ad: 'Bir', kategori: 'museum', enlem: 0, boylam: 8 }, kalisDakika: 90, eklenmeSirasi: 0 },
  { yer: { id: 3, ad: 'Dışarıda', kategori: 'park', enlem: 1, boylam: 9 }, kalisDakika: 30, eklenmeSirasi: 1 },
  { yer: { id: 2, ad: 'İki', kategori: 'park', enlem: 0, boylam: 2 }, kalisDakika: 30, eklenmeSirasi: 2 },
]

describe('durak sıralama', () => {
  it('yol oranı ve sanal ara noktayı sıralar, rota dışındakini sona koyar', () => {
    const sirali = duraklariSirala(duraklar, rota)
    expect(sirali.map((nokta) => nokta.ad)).toEqual(['İki', 'Aksaray', 'Bir', 'Dışarıda'])
    expect(rotaNoktalari({ ad: 'Baş', enlem: 0, boylam: 0 }, { ad: 'Son', enlem: 0, boylam: 10 }, sirali).map((nokta) => nokta.durakId)).toEqual([undefined, 2, undefined, 1, 3, undefined])
  })
  it('ara nokta zaten duraksa ikinci kez eklemez', () => {
    const sirali = duraklariSirala([{ yer: { id: 4, ad: 'Aksaray', kategori: 'park', enlem: 0, boylam: 5 }, kalisDakika: 30, eklenmeSirasi: 0 }], rota)
    expect(sirali).toHaveLength(1)
  })
})

describe('zaman çizelgesi', () => {
  it('bacakları ve önceki kalış sürelerini toplar', () => {
    const noktalar = duraklariSirala(duraklar.slice(0, 1), { ...rota, araNokta: null })
    const cizelge = zamanCizelgesi(new Date('2026-09-24T08:00:00'), [...noktalar, { ad: 'Son', enlem: 0, boylam: 10 }], [{ sureSn: 3600, mesafeM: 1 }, { sureSn: 1800, mesafeM: 1 }])
    expect(cizelge.map((satir) => [satir.varis.getHours(), satir.varis.getMinutes()])).toEqual([[9, 0], [11, 0]])
  })
  it('kalkışı beş dakikaya yuvarlar ve kalış varsayılanlarını verir', () => {
    expect(yuvarlanmisKalkis(new Date('2026-09-24T08:02:00'))).toBe('08:05')
    expect(kalkisZamani('12:35', new Date('2026-09-24T08:00:00')).getHours()).toBe(12)
    expect([varsayilanKalisSuresi('museum'), varsayilanKalisSuresi('fuel'), varsayilanKalisSuresi('bilinmeyen')]).toEqual([90, 10, 30])
  })
})

describe('harita bağlantıları', () => {
  it('iOS için Apple, diğerleri için Google bağlantısı üretir', () => {
    const nokta = { enlem: 39, boylam: 33 }
    expect(gitBaglantisi(nokta, 'iPhone')).toBe('https://maps.apple.com/?daddr=39,33&dirflg=d')
    expect(gitBaglantisi(nokta, 'Android')).toContain('destination=39,33&travelmode=driving')
  })
  it('kalan ara noktaları dokuz ile sınırlar', () => {
    const adres = new URL(tumRotaBaglantisi(Array.from({ length: 12 }, (_, sira) => ({ enlem: sira, boylam: 1 })), { enlem: 40, boylam: 30 }))
    expect(adres.searchParams.get('origin')).toBe('My Location')
    expect(adres.searchParams.get('destination')).toBe('40,30')
    expect(adres.searchParams.get('waypoints')?.split('|')).toHaveLength(9)
  })
})
