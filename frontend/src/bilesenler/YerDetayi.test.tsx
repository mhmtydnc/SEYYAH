import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { KoridorYeri, YerDetayi as YerDetayiTipi } from '../api/tipler'
import { YerDetayiIcerigi } from './YerDetayi'

const yer: KoridorYeri = {
  id: 118, ad: 'Kırşehir Kalesi', kategori: 'castle', tur: 'gezi', enlem: 39.14, boylam: 34.16,
  yolaUzaklikM: 1200, yolOrani: 0.5, ucret: 'Ücretsiz', calismaSaatleri: '09:00-17:00',
  wikidataId: null, website: 'https://ornek.com',
}

function ciz(detay: YerDetayiTipi | null) {
  return renderToStaticMarkup(<YerDetayiIcerigi yer={yer} detay={detay} hata={false} durakMi={false}
    eklenebilir durakDegistir={() => {}} />)
}

describe('Yer detayı koşullu bölümleri', () => {
  it('wikidata ve google boşken bilinen alanları gösterir', () => {
    const html = ciz({ ...yer, wikidata: null, google: null })
    expect(html).toContain('Kırşehir Kalesi')
    expect(html).toContain('09:00-17:00')
    expect(html).toContain('Ücretsiz')
    expect(html).toContain('https://ornek.com')
    expect(html).not.toContain('yer-detay-gorsel')
    expect(html).not.toContain('yer-detay-google')
    expect(html).not.toContain('Vikipedi')
  })

  it('google boşken açıklama ve Vikipedi bağlantısını gösterir', () => {
    const html = ciz({ ...yer, google: null, wikidata: {
      aciklama: 'kırşehirde bir kale', vikipedi: 'https://tr.wikipedia.org/wiki/Kale', gorsel: null,
    } })
    expect(html).toContain('Kırşehirde bir kale')
    expect(html).toContain('Vikipedi&#x27;de oku')
    expect(html).not.toContain('yer-detay-google')
  })

  it('görselde yazar yoksa yalnız lisansı ve Commons bağlantısını gösterir', () => {
    const html = ciz({ ...yer, google: { puan: 4.5, yorumSayisi: 1234, haritaBaglantisi: 'https://maps.google.com' }, wikidata: {
      aciklama: null, vikipedi: null, gorsel: {
        url: 'https://upload.wikimedia.org/foto.jpg', sayfa: 'https://commons.wikimedia.org/wiki/File:Foto.jpg',
        yazar: null, lisans: 'CC BY-SA 4.0',
      },
    } })
    expect(html).toContain('alt="Kırşehir Kalesi"')
    expect(html).toContain('loading="lazy"')
    expect(html).toContain('Fotoğraf: CC BY-SA 4.0')
    expect(html).toContain('commons.wikimedia.org')
    expect(html).toContain('Google')
    expect(html).toContain('1.234 değerlendirme')
  })
})
