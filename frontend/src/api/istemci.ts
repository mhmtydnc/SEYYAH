import type { HazirSoruTuru, KimlikYaniti, KonumOnerisi, Kullanici, KayitliRota, RotaKaydi, RotaYaniti, Konum, DurakliRota, RotaNoktasi, YerDetayi, YerSorusuYaniti } from './tipler'

export const TOKEN_ANAHTARI = 'seyyah-token'

export class IstekHatasi extends Error {
  constructor(mesaj: string, public durum: number) {
    super(mesaj)
    this.name = 'IstekHatasi'
  }
}

export async function istek<T>(yol: string, ayarlar: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem(TOKEN_ANAHTARI)
  const basliklar = new Headers(ayarlar.headers)
  if (token) basliklar.set('Authorization', `Bearer ${token}`)
  if (ayarlar.body) basliklar.set('Content-Type', 'application/json')

  let yanit: Response
  try {
    yanit = await fetch(yol, { ...ayarlar, headers: basliklar })
  } catch (neden) {
    if (neden instanceof Error && neden.name === 'AbortError') throw neden
    throw new IstekHatasi('Sunucuya ulaşılamadı. Lütfen tekrar deneyin.', 0)
  }

  if (!yanit.ok) {
    const hata = await yanit.json().catch(() => null) as { detail?: unknown; title?: unknown } | null
    const mesaj = typeof hata?.detail === 'string' && hata.detail.trim()
      ? hata.detail
      : typeof hata?.title === 'string' && hata.title.trim()
        ? hata.title
        : `İstek başarısız oldu (${yanit.status}).`
    throw new IstekHatasi(mesaj, yanit.status)
  }

  if (yanit.status === 204) return undefined as T
  return yanit.json() as Promise<T>
}

export const api = {
  yerDetayi: (kimlik: number) => istek<YerDetayi>(`/api/yerler/${kimlik}/detay`),
  hazirSoruSor: (kimlik: number, soru: HazirSoruTuru) =>
    istek<YerSorusuYaniti>(`/api/yerler/${kimlik}/hazir-soru`, { method: 'POST', body: JSON.stringify({ soru }) }),
  yerHakkindaSor: (kimlik: number, metin: string) =>
    istek<YerSorusuYaniti>(`/api/uye/yerler/${kimlik}/soru`, { method: 'POST', body: JSON.stringify({ metin }) }),
  konumAra: (sorgu: string, sinyal?: AbortSignal) =>
    istek<KonumOnerisi[]>(`/api/konum/ara?${new URLSearchParams({ q: sorgu, limit: '5' })}`, { signal: sinyal }),
  rotaOlustur: (kalkis: Konum, varis: Konum, yaricap: number) =>
    istek<RotaYaniti>(`/api/rota?${new URLSearchParams({
      kalkisEnlem: String(kalkis.enlem), kalkisBoylam: String(kalkis.boylam),
      varisEnlem: String(varis.enlem), varisBoylam: String(varis.boylam),
      yaricap: String(yaricap), limit: '20',
    })}`),
  durakliRotaOlustur: (noktalar: RotaNoktasi[], sinyal?: AbortSignal) =>
    istek<DurakliRota>('/api/rota/duraklu', { method: 'POST', body: JSON.stringify({ noktalar }), signal: sinyal }),
  giris: (eposta: string, sifre: string) =>
    istek<KimlikYaniti>('/api/auth/giris', { method: 'POST', body: JSON.stringify({ eposta, sifre }) }),
  kayit: (ad: string, eposta: string, sifre: string) =>
    istek<KimlikYaniti>('/api/auth/kayit', { method: 'POST', body: JSON.stringify({ ad, eposta, sifre }) }),
  ben: () => istek<Kullanici>('/api/auth/ben'),
  rotalarim: () => istek<KayitliRota[]>('/api/rotalarim'),
  rotaKaydet: (kayit: RotaKaydi) =>
    istek<KayitliRota>('/api/rotalarim', { method: 'POST', body: JSON.stringify(kayit) }),
  rotaSil: (kimlik: number) => istek<void>(`/api/rotalarim/${kimlik}`, { method: 'DELETE' }),
}
