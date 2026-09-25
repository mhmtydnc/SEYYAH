export interface Konum {
  ad: string
  enlem: number
  boylam: number
}

export interface KonumOnerisi extends Konum {
  etiket: string
}

export type YerTuru = 'gezi' | 'mola' | 'destek'

export interface KoridorYeri {
  id: number
  ad: string
  kategori: string
  tur: YerTuru
  enlem: number
  boylam: number
  yolaUzaklikM: number
  yolOrani: number
  ucret: string | number | null
  calismaSaatleri: string | null
  wikidataId: string | null
  website: string | null
  gorselUrl: string | null
}

export interface YerDetayi extends Pick<KoridorYeri, 'id' | 'ad' | 'kategori' | 'tur' | 'enlem' | 'boylam' | 'ucret' | 'calismaSaatleri' | 'website'> {
  ozet?: { metin: string; vikipedi: string | null } | null
  wikidata: {
    aciklama: string | null
    vikipedi: string | null
    gorsel: { url: string; sayfa: string; yazar: string | null; lisans: string | null } | null
  } | null
  google: { puan: number; yorumSayisi: number; haritaBaglantisi: string | null
    yorumlar?: { yazar: string; yazarBaglantisi: string | null; puan: number; metin: string; zaman: string }[] } | null
}

export type HazirSoruTuru = 'deger' | 'sure' | 'cocuk' | 'ipucu'

export interface YerSorusuYaniti {
  cevap: string
  onbellekten: boolean
}

export interface Rota {
  sira: number
  ad: string
  uzerinden: string | null
  araNokta: Konum | null
  mesafeM: number
  sureSn: number
  geometri: [number, number][]
  yerler: Record<YerTuru, KoridorYeri[]>
}

export interface RotaNoktasi {
  enlem: number
  boylam: number
  durakId?: number
}

export interface RotaBacagi {
  mesafeM: number
  sureSn: number
}

export interface DurakliRota {
  mesafeM: number
  sureSn: number
  geometri: [number, number][]
  bacaklar: RotaBacagi[]
}

export interface KayitliDurak extends Pick<KoridorYeri, 'id' | 'ad' | 'kategori' | 'enlem' | 'boylam'> {}

export interface RotaYaniti {
  rotalar: Rota[]
}

export interface Kullanici {
  id: number
  ad: string
  eposta: string
}

export interface KimlikYaniti {
  token: string
  kullanici: Kullanici
}

export interface KayitliRota {
  id: number
  baslik: string
  kalkis: Konum
  varis: Konum
  uzerinden?: Konum | null
  duraklar?: KayitliDurak[]
  olusturulma: string
}

export type RotaKaydi = Pick<KayitliRota, 'baslik' | 'kalkis' | 'varis' | 'uzerinden' | 'duraklar'>
