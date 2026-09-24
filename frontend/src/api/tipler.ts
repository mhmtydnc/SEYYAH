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
}

export interface Rota {
  sira: number
  ad: string
  uzerinden: string | null
  mesafeM: number
  sureSn: number
  geometri: [number, number][]
  yerler: Record<YerTuru, KoridorYeri[]>
}

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
  olusturulma: string
}

export type RotaKaydi = Pick<KayitliRota, 'baslik' | 'kalkis' | 'varis'>
