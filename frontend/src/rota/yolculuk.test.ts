import { describe, expect, it } from 'vitest'
import type { SiraliNokta } from './duraklar'
import { sanalNoktalariBirlestir } from './yolculuk'

const durak = (ad: string): SiraliNokta => ({
  ad, enlem: 39, boylam: 33,
  durak: { yer: { id: 1, ad, kategori: 'castle', enlem: 39, boylam: 33 }, kalisDakika: 30, eklenmeSirasi: 0 },
})

describe('sanalNoktalariBirlestir', () => {
  it('ara şehri çıkarır, bacağını sonraki noktaya ekler', () => {
    const noktalar: SiraliNokta[] = [durak('Kale'), { ad: 'Aksaray', enlem: 38.4, boylam: 34, sanal: true }, { ad: 'Göreme', enlem: 38.6, boylam: 34.8 }]
    const bacaklar = [{ mesafeM: 100, sureSn: 60 }, { mesafeM: 200, sureSn: 120 }, { mesafeM: 300, sureSn: 180 }]

    const sonuc = sanalNoktalariBirlestir(noktalar, bacaklar)

    expect(sonuc.noktalar.map((n) => n.ad)).toEqual(['Kale', 'Göreme'])
    expect(sonuc.bacaklar).toEqual([{ mesafeM: 100, sureSn: 60 }, { mesafeM: 500, sureSn: 300 }])
  })

  it('sanal nokta yoksa değiştirmez', () => {
    const noktalar = [durak('Kale'), { ad: 'Göreme', enlem: 38.6, boylam: 34.8 }]
    const bacaklar = [{ mesafeM: 100, sureSn: 60 }, { mesafeM: 300, sureSn: 180 }]

    expect(sanalNoktalariBirlestir(noktalar, bacaklar)).toEqual({ noktalar, bacaklar })
  })
})
