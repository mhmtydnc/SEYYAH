import { useEffect, useState } from 'react'
import { api } from '../api/istemci'
import { guvenliSite } from '../api/baglanti'
import { kategoriAdi } from '../api/kategoriler'
import type { KoridorYeri, YerDetayi as YerDetayiTipi } from '../api/tipler'

const detayIstegi = new Map<number, Promise<YerDetayiTipi>>()

function detayGetir(kimlik: number) {
  let istek = detayIstegi.get(kimlik)
  if (!istek) {
    istek = api.yerDetayi(kimlik)
    detayIstegi.set(kimlik, istek)
  }
  return istek
}

function ilkHarfBuyuk(metin: string) {
  return metin.charAt(0).toLocaleUpperCase('tr-TR') + metin.slice(1)
}

export function YerDetayiIcerigi({ yer, detay, hata, durakMi, eklenebilir, durakDegistir }: {
  yer: KoridorYeri; detay: YerDetayiTipi | null; hata: boolean; durakMi: boolean
  eklenebilir: boolean; durakDegistir: () => void
}) {
  const gorsel = detay?.wikidata?.gorsel
  const google = detay?.google
  const site = guvenliSite(detay?.website ?? yer.website)
  const vikipedi = guvenliSite(detay?.wikidata?.vikipedi)
  const gorselUrl = guvenliSite(gorsel?.url)
  const gorselSayfasi = guvenliSite(gorsel?.sayfa)
  const haritaBaglantisi = guvenliSite(google?.haritaBaglantisi)
  return <div className="yer-detay-icerigi">
    {hata && <p className="yer-detay-hata" role="alert">Yer detayı alınamadı. Bilinen bilgiler gösteriliyor.</p>}
    {gorsel && gorselUrl && <figure className="yer-detay-gorsel">
      <img src={gorselUrl} alt={detay?.ad ?? yer.ad} loading="lazy" />
      {(gorsel.yazar || gorsel.lisans) && <figcaption>
        {gorselSayfasi ? <a href={gorselSayfasi} target="_blank" rel="noopener noreferrer">Fotoğraf: {[gorsel.yazar, gorsel.lisans].filter(Boolean).join(' · ')}</a>
          : <>Fotoğraf: {[gorsel.yazar, gorsel.lisans].filter(Boolean).join(' · ')}</>}
      </figcaption>}
    </figure>}
    <h2>{detay?.ad ?? yer.ad}</h2>
    <p className="yer-detay-ozet">{kategoriAdi(detay?.kategori ?? yer.kategori)} · Yoldan {(yer.yolaUzaklikM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</p>
    {google && <div className="yer-detay-google">
      <span aria-label="Puan">★ {google.puan.toLocaleString('tr-TR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}</span>
      <span>({google.yorumSayisi.toLocaleString('tr-TR')} değerlendirme)</span>
      <strong>Google</strong>
      {haritaBaglantisi && <a href={haritaBaglantisi} target="_blank" rel="noopener noreferrer">Google Haritalar'da gör ↗</a>}
    </div>}
    {detay?.wikidata?.aciklama && <p className="yer-detay-aciklama">{ilkHarfBuyuk(detay.wikidata.aciklama)}</p>}
    {(detay?.calismaSaatleri ?? yer.calismaSaatleri) && <p><strong>Çalışma saatleri:</strong> {detay?.calismaSaatleri ?? yer.calismaSaatleri}</p>}
    {(detay?.ucret ?? yer.ucret) != null && <p><strong>Ücret:</strong> {detay?.ucret ?? yer.ucret}</p>}
    {site && <a href={site} target="_blank" rel="noopener noreferrer">Web sitesi ↗</a>}
    {vikipedi && <a href={vikipedi} target="_blank" rel="noopener noreferrer">Vikipedi'de oku ↗</a>}
    <button type="button" className="durak-dugmesi" disabled={!eklenebilir} onClick={durakDegistir}>{durakMi ? 'Duraktan çıkar' : '+ Durak ekle'}</button>
  </div>
}

export function YerDetayi({ yer, durakMi, eklenebilir, durakDegistir, kapat }: {
  yer: KoridorYeri; durakMi: boolean; eklenebilir: boolean; durakDegistir: () => void; kapat: () => void
}) {
  const [detay, detayAyarla] = useState<YerDetayiTipi | null>(null)
  const [yukleniyor, yukleniyorAyarla] = useState(true)
  const [hata, hataAyarla] = useState(false)
  useEffect(() => {
    let gecerli = true
    detayGetir(yer.id).then((gelen) => { if (gecerli) detayAyarla(gelen) })
      .catch(() => { if (gecerli) hataAyarla(true) })
      .finally(() => { if (gecerli) yukleniyorAyarla(false) })
    return () => { gecerli = false }
  }, [yer.id])
  return <section id="yer-detayi" className="yer-detayi" aria-label={`${yer.ad} detayı`}>
    <div className="yer-detay-ust">
      <button type="button" className="listeye-don" onClick={kapat}>← Listeye dön</button>
      <button type="button" className="detay-kapat" onClick={kapat} aria-label="Detayı kapat"><span /></button>
    </div>
    {yukleniyor ? <div className="yer-detay-iskelet" role="status" aria-label="Yer detayı yükleniyor">
      <div /><div /><div /><div />
    </div> : <YerDetayiIcerigi yer={yer} detay={detay} hata={hata} durakMi={durakMi} eklenebilir={eklenebilir} durakDegistir={durakDegistir} />}
  </section>
}
