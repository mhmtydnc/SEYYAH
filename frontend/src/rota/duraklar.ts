import type { Konum, KoridorYeri, Rota, RotaNoktasi } from '../api/tipler'

export interface SeciliDurak {
  yer: Pick<KoridorYeri, 'id' | 'ad' | 'kategori' | 'enlem' | 'boylam'> & Partial<Pick<KoridorYeri, 'calismaSaatleri'>>
  kalisDakika: number
  eklenmeSirasi: number
}

export interface SiraliNokta {
  ad: string
  enlem: number
  boylam: number
  durak?: SeciliDurak
  sanal?: boolean
}

function rotaYeri(rota: Rota, kimlik: number): KoridorYeri | undefined {
  return Object.values(rota.yerler).flat().find((yer) => yer.id === kimlik)
}

export function araNoktaOrani(rota: Rota): number {
  if (!rota.araNokta || rota.geometri.length < 2) return 0.5
  let toplam = 0
  const uzunluklar = rota.geometri.slice(1).map((nokta, sira) => {
    const onceki = rota.geometri[sira]
    const uzunluk = Math.hypot(nokta[0] - onceki[0], nokta[1] - onceki[1])
    toplam += uzunluk
    return uzunluk
  })
  let gecilen = 0
  let enYakin = { uzaklik: Infinity, oran: 0.5 }
  rota.geometri.slice(1).forEach((nokta, sira) => {
    const onceki = rota.geometri[sira]
    const farkX = nokta[0] - onceki[0]
    const farkY = nokta[1] - onceki[1]
    const kare = farkX * farkX + farkY * farkY
    const oran = kare ? Math.max(0, Math.min(1, ((rota.araNokta!.boylam - onceki[0]) * farkX + (rota.araNokta!.enlem - onceki[1]) * farkY) / kare)) : 0
    const uzaklik = Math.hypot(onceki[0] + oran * farkX - rota.araNokta!.boylam, onceki[1] + oran * farkY - rota.araNokta!.enlem)
    if (uzaklik < enYakin.uzaklik) enYakin = { uzaklik, oran: toplam ? (gecilen + oran * uzunluklar[sira]) / toplam : 0.5 }
    gecilen += uzunluklar[sira]
  })
  return enYakin.oran
}

export function duraklariSirala(duraklar: SeciliDurak[], rota: Rota): SiraliNokta[] {
  const sirali: (SiraliNokta & { oran?: number })[] = duraklar.map((durak) => ({
    ad: durak.yer.ad, enlem: durak.yer.enlem, boylam: durak.yer.boylam, durak,
    oran: rotaYeri(rota, durak.yer.id)?.yolOrani,
  })).sort((ilk, ikinci) => {
    if (ilk.oran == null && ikinci.oran == null) return ilk.durak.eklenmeSirasi - ikinci.durak.eklenmeSirasi
    if (ilk.oran == null) return 1
    if (ikinci.oran == null) return -1
    return ilk.oran - ikinci.oran || ilk.durak.eklenmeSirasi - ikinci.durak.eklenmeSirasi
  })
  if (rota.araNokta && !sirali.some((nokta) => Math.abs(nokta.enlem - rota.araNokta!.enlem) < 0.0001 && Math.abs(nokta.boylam - rota.araNokta!.boylam) < 0.0001)) {
    const oran = araNoktaOrani(rota)
    const sira = sirali.findIndex((nokta) => nokta.oran == null || nokta.oran > oran)
    const sanal = { ...rota.araNokta, sanal: true }
    sirali.splice(sira < 0 ? sirali.length : sira, 0, { ...sanal, oran })
  }
  return sirali.map(({ ad, enlem, boylam, durak, sanal }) => ({ ad, enlem, boylam, durak, sanal }))
}

export function rotaNoktalari(kalkis: Konum, varis: Konum, sirali: SiraliNokta[]): RotaNoktasi[] {
  return ([kalkis, ...sirali, varis] as SiraliNokta[]).map((nokta) => ({
    enlem: nokta.enlem, boylam: nokta.boylam,
    ...(nokta.durak ? { durakId: nokta.durak.yer.id } : {}),
  }))
}
