import { renderToStaticMarkup } from 'react-dom/server'
// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, IstekHatasi } from '../api/istemci'
import type { KoridorYeri, YerDetayi as YerDetayiTipi } from '../api/tipler'
import { OturumSaglayici } from '../oturum/Oturum'
import { YerDetayi, YerDetayiIcerigi, YerSorulari } from './YerDetayi'

afterEach(cleanup)

const yer: KoridorYeri = {
  id: 118, ad: 'Kırşehir Kalesi', kategori: 'castle', tur: 'gezi', enlem: 39.14, boylam: 34.16,
  yolaUzaklikM: 1200, yolOrani: 0.5, ucret: 'Ücretsiz', calismaSaatleri: '09:00-17:00',
  wikidataId: null, website: 'https://ornek.com', gorselUrl: null,
}

function ciz(detay: YerDetayiTipi | null) {
  return renderToStaticMarkup(<YerDetayiIcerigi yer={yer} detay={detay} hata={false} durakMi={false}
    eklenebilir durakDegistir={() => {}} />)
}

describe('Yer detayı koşullu bölümleri', () => {
  it('detay isteği bitmeden bilinen bilgileri ve liste görselini gösterir', () => {
    vi.spyOn(api, 'yerDetayi').mockImplementation(() => new Promise(() => {}))
    render(<OturumSaglayici><YerDetayi yer={{ ...yer, id: 119, gorselUrl: 'https://upload.wikimedia.org/foto.jpg' }}
      durakMi={false} eklenebilir durakDegistir={() => {}} /></OturumSaglayici>)
    expect(screen.getByRole('heading', { name: 'Kırşehir Kalesi' })).toBeTruthy()
    expect(screen.getByText('09:00-17:00')).toBeTruthy()
    expect(screen.getByText(/Kale · Yoldan/)).toBeTruthy()
    expect(screen.getByRole('img', { name: 'Kırşehir Kalesi' }).getAttribute('src')).toBe('https://upload.wikimedia.org/foto.jpg')
  })
  it('istek sürerken bilinen alanları ve hazır görseli gösterir', () => {
    const html = renderToStaticMarkup(<YerDetayiIcerigi yer={{ ...yer, gorselUrl: 'https://upload.wikimedia.org/foto.jpg' }}
      detay={null} hata={false} yukleniyor durakMi={false} eklenebilir durakDegistir={() => {}} />)
    expect(html).toContain('Kırşehir Kalesi')
    expect(html).toContain('Kale')
    expect(html).toContain('09:00-17:00')
    expect(html).toContain('https://upload.wikimedia.org/foto.jpg')
    expect(html).toContain('Sorular yükleniyor')
  })
  it('özet varken eski açıklamayı gizler', () => {
    const html = ciz({ ...yer, ozet: { metin: 'Yeni özet metni', vikipedi: 'https://tr.wikipedia.org/wiki/Kale' },
      wikidata: { aciklama: 'Eski açıklama', vikipedi: null, gorsel: null }, google: null })
    expect(html).toContain('Yeni özet metni')
    expect(html).toContain('Yapay zekâ ile Vikipedi&#x27;den özetlendi')
    expect(html).not.toContain('Eski açıklama')
  })

  it('yorum yokken yorum bölümünü göstermez', () => {
    const html = ciz({ ...yer, wikidata: null, google: { puan: 4.5, yorumSayisi: 12, haritaBaglantisi: null, yorumlar: [] } })
    expect(html).not.toContain('Google yorumları')
  })

  it('en fazla üç yorumu düz metin ve güvenli bağlantılarla gösterir', () => {
    const yorum = { yazar: '<Ayşe>', yazarBaglantisi: 'javascript:alert(1)', puan: 5, metin: '<b>Güzel</b>', zaman: '2 ay önce' }
    const html = ciz({ ...yer, wikidata: null, google: { puan: 5, yorumSayisi: 4, haritaBaglantisi: null,
      yorumlar: [yorum, yorum, yorum, { ...yorum, metin: 'Dördüncü' }] } })
    expect(html).toContain('&lt;b&gt;Güzel&lt;/b&gt;')
    expect(html).not.toContain('javascript:')
    expect(html).not.toContain('Dördüncü')
    expect(html.match(/yer-detay-yorum-ust/g)).toHaveLength(3)
  })

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

describe('yer soru bolumu', () => {
  beforeEach(() => vi.restoreAllMocks())
  it('hazir soruyu dogru uca gonderir ve tekrari onbellekten yanitlar', async () => {
    const istek = vi.spyOn(api, 'hazirSoruSor').mockResolvedValue({ cevap: 'Evet, gormeye deger.', onbellekten: false })
    render(<YerSorulari yerKimligi={84721} girisYapilmis={false} />)
    const dugme = screen.getAllByRole('button')[0]
    fireEvent.click(dugme)
    expect(await screen.findByText('Evet, gormeye deger.')).toBeTruthy()
    expect(istek).toHaveBeenCalledWith(84721, 'deger')
    fireEvent.click(dugme)
    await waitFor(() => expect(screen.getAllByText('Evet, gormeye deger.')).toHaveLength(2))
    expect(istek).toHaveBeenCalledTimes(1)
  })
  it('giris yokken serbest soru alani yerine giris baglantisi gosterir', () => {
    render(<YerSorulari yerKimligi={84722} girisYapilmis={false} />)
    expect(screen.queryByLabelText('Kendi sorun')).toBeNull()
    expect(screen.getByRole('link').getAttribute('href')).toBe('/giris')
  })
  it('429 yanitinda sinir mesajini gosterir', async () => {
    vi.spyOn(api, 'hazirSoruSor').mockRejectedValue(new IstekHatasi('Limit', 429))
    render(<YerSorulari yerKimligi={84723} girisYapilmis={false} />)
    fireEvent.click(screen.getAllByRole('button')[0])
    expect((await screen.findByRole('alert')).textContent).toMatch(/fazla soru/)
  })
})
