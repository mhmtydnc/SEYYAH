import type { RotaBacagi } from '../api/tipler'
import type { SiraliNokta } from './duraklar'

// Seçili alternatifi korumak için eklenen sanal ara şehir ("Aksaray üzerinden") yolculukta durak değildir:
// "Vardım" beklenmez. Bacağı bir sonraki gerçek noktanın bacağına eklenir, varış saatleri değişmez.
// bacaklar[i] = noktalar[i]'ye gelen bacak (kalkış noktalarda yok).
export function sanalNoktalariBirlestir(noktalar: SiraliNokta[], bacaklar: RotaBacagi[]) {
  const sonucNoktalar: SiraliNokta[] = []
  const sonucBacaklar: RotaBacagi[] = []
  let biriken: RotaBacagi = { mesafeM: 0, sureSn: 0 }
  noktalar.forEach((nokta, sira) => {
    const bacak = bacaklar[sira] ?? { mesafeM: 0, sureSn: 0 }
    biriken = { mesafeM: biriken.mesafeM + bacak.mesafeM, sureSn: biriken.sureSn + bacak.sureSn }
    if (nokta.sanal) return
    sonucNoktalar.push(nokta)
    sonucBacaklar.push(biriken)
    biriken = { mesafeM: 0, sureSn: 0 }
  })
  return { noktalar: sonucNoktalar, bacaklar: sonucBacaklar }
}
