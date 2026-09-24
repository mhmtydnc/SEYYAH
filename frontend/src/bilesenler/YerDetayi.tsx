import { useEffect, useRef, useState } from 'react'
import { api } from '../api/istemci'
import { IstekHatasi } from '../api/istemci'
import { guvenliSite } from '../api/baglanti'
import { kategoriAdi } from '../api/kategoriler'
import type { HazirSoruTuru, KoridorYeri, YerDetayi as YerDetayiTipi } from '../api/tipler'
import { useOturum } from '../oturum/Oturum'

const detayIstegi = new Map<number, Promise<YerDetayiTipi>>()
const hazirYanitiOnbellegi = new Map<string, string>()

const hazirSorular: { tur: HazirSoruTuru; metin: string }[] = [
  { tur: 'deger', metin: 'Görmeye değer mi?' },
  { tur: 'sure', metin: 'Ne kadar zaman ayırmalıyım?' },
  { tur: 'cocuk', metin: 'Çocuklarla uygun mu?' },
  { tur: 'ipucu', metin: 'Ziyaret için ipuçları' },
]

function soruHatasi(neden: unknown) {
  if (neden instanceof IstekHatasi) {
    if (neden.durum === 429) return 'Çok fazla soru sordun, biraz sonra tekrar dene'
    if (neden.durum === 503) return 'Yapay zekâ şu an yanıt veremiyor'
    return neden.message
  }
  return neden instanceof Error ? neden.message : 'Soru yanıtlanamadı.'
}

export function YerSorulari({ yerKimligi, girisYapilmis }: { yerKimligi: number; girisYapilmis: boolean }) {
  const [yanitlar, yanitlarAyarla] = useState<{ soru: string; cevap: string }[]>([])
  const [bekleyen, bekleyenAyarla] = useState<string | null>(null)
  const [metin, metinAyarla] = useState('')
  const [hata, hataAyarla] = useState('')

  async function hazirSoruSor(tur: HazirSoruTuru, soru: string) {
    const anahtar = `${yerKimligi}:${tur}`
    const onbellek = hazirYanitiOnbellegi.get(anahtar)
    if (onbellek) {
      yanitlarAyarla((onceki) => [{ soru, cevap: onbellek }, ...onceki])
      return
    }
    bekleyenAyarla(tur)
    hataAyarla('')
    try {
      const yanit = await api.hazirSoruSor(yerKimligi, tur)
      hazirYanitiOnbellegi.set(anahtar, yanit.cevap)
      yanitlarAyarla((onceki) => [{ soru, cevap: yanit.cevap }, ...onceki])
    } catch (neden) {
      hataAyarla(soruHatasi(neden))
    } finally {
      bekleyenAyarla(null)
    }
  }

  async function serbestSoruGonder(event: React.FormEvent) {
    event.preventDefault()
    const soru = metin.trim()
    if (soru.length < 3 || soru.length > 200) return
    bekleyenAyarla('serbest')
    hataAyarla('')
    try {
      const yanit = await api.yerHakkindaSor(yerKimligi, soru)
      yanitlarAyarla((onceki) => [{ soru, cevap: yanit.cevap }, ...onceki])
      metinAyarla('')
    } catch (neden) {
      hataAyarla(soruHatasi(neden))
    } finally {
      bekleyenAyarla(null)
    }
  }

  return <section className="yer-sorulari" aria-label="Bu yer hakkında sor">
    <h3>Bu yer hakkında sor</h3>
    <div className="yer-soru-cipleri">{hazirSorular.map(({ tur, metin: soru }) => <button key={tur} type="button"
      disabled={bekleyen !== null} onClick={() => void hazirSoruSor(tur, soru)}>
      {bekleyen === tur ? 'Düşünüyor…' : soru}
    </button>)}</div>
    {girisYapilmis ? <form className="yer-serbest-soru" onSubmit={(event) => void serbestSoruGonder(event)}>
      <label htmlFor="yer-soru-metni">Kendi sorun</label>
      <textarea id="yer-soru-metni" value={metin} maxLength={200} onChange={(event) => metinAyarla(event.target.value)} />
      <div className="yer-soru-form-alt"><small>{metin.length}/200</small>
        <button type="submit" className="birincil" disabled={bekleyen !== null || metin.trim().length < 3}>
          {bekleyen === 'serbest' ? 'Düşünüyor…' : 'Sor'}
        </button>
      </div>
    </form> : <a href="/giris">Kendi sorunu sormak için giriş yap</a>}
    {hata && <p className="yer-soru-hata" role="alert">{hata}</p>}
    {!!yanitlar.length && <ol className="yer-soru-yanitlari">{yanitlar.map((yanit, sira) => <li key={`${sira}-${yanit.soru}`}>
      <strong>{yanit.soru}</strong><p>{yanit.cevap}</p>
    </li>)}</ol>}
    <small className="yer-soru-notu">Yanıtlar yapay zekâ (Google Gemini) tarafından üretilir, hata içerebilir. Sorun Google'a iletilir.</small>
  </section>
}

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

function YorumKarti({ yorum }: { yorum: NonNullable<NonNullable<YerDetayiTipi['google']>['yorumlar']>[number] }) {
  const [acik, acikAyarla] = useState(false)
  const [uzun, uzunAyarla] = useState(false)
  const metinAlani = useRef<HTMLParagraphElement>(null)
  const yazarBaglantisi = guvenliSite(yorum.yazarBaglantisi)
  useEffect(() => {
    const alan = metinAlani.current
    if (!alan) return
    const olc = () => uzunAyarla(alan.scrollHeight > alan.clientHeight + 1)
    olc()
    const gozlemci = new ResizeObserver(olc)
    gozlemci.observe(alan)
    return () => gozlemci.disconnect()
  }, [yorum.metin])
  return <article className="yer-detay-yorum">
    <div className="yer-detay-yorum-ust">
      <strong>{yazarBaglantisi ? <a href={yazarBaglantisi} target="_blank" rel="noopener noreferrer">{yorum.yazar}</a> : yorum.yazar}</strong>
      <small>{yorum.zaman}</small>
    </div>
    <span className="yer-detay-yildiz" aria-label={`${yorum.puan} yıldız`}>{'★'.repeat(Math.max(0, Math.min(5, Math.round(yorum.puan))))}</span>
    <p ref={metinAlani} className={acik ? '' : 'yer-detay-yorum-kisa'}>{yorum.metin}</p>
    {(uzun || acik) && <button type="button" className="yer-detay-devami" onClick={() => acikAyarla(!acik)}>{acik ? 'Daha az' : 'Devamı'}</button>}
  </article>
}

export function YerDetayiIcerigi({ yer, detay, hata, durakMi, eklenebilir, durakDegistir, girisYapilmis = false }: {
  yer: KoridorYeri; detay: YerDetayiTipi | null; hata: boolean; durakMi: boolean
  eklenebilir: boolean; durakDegistir: () => void; girisYapilmis?: boolean
}) {
  const gorsel = detay?.wikidata?.gorsel
  const google = detay?.google
  const site = guvenliSite(detay?.website ?? yer.website)
  const vikipedi = guvenliSite(detay?.wikidata?.vikipedi)
  const gorselUrl = guvenliSite(gorsel?.url)
  const gorselSayfasi = guvenliSite(gorsel?.sayfa)
  const haritaBaglantisi = guvenliSite(google?.haritaBaglantisi)
  const ozetBaglantisi = guvenliSite(detay?.ozet?.vikipedi)
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
    {!!google?.yorumlar?.length && <section className="yer-detay-yorumlar" aria-label="Google yorumları">
      <h3>Google yorumları</h3>
      {google.yorumlar.slice(0, 3).map((yorum, sira) => <YorumKarti key={`${yorum.yazar}-${sira}`} yorum={yorum} />)}
    </section>}
    {detay?.ozet ? <div className="yer-detay-aciklama">
      <p>{detay.ozet.metin}</p>
      <small>Yapay zekâ ile Vikipedi'den özetlendi{ozetBaglantisi && <> · <a href={ozetBaglantisi} target="_blank" rel="noopener noreferrer">Vikipedi</a></>}</small>
    </div> : detay?.wikidata?.aciklama && <p className="yer-detay-aciklama">{ilkHarfBuyuk(detay.wikidata.aciklama)}</p>}
    <YerSorulari key={yer.id} yerKimligi={yer.id} girisYapilmis={girisYapilmis} />
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
  const { kullanici } = useOturum()
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
    </div> : <YerDetayiIcerigi yer={yer} detay={detay} hata={hata} durakMi={durakMi} eklenebilir={eklenebilir} durakDegistir={durakDegistir} girisYapilmis={!!kullanici} />}
  </section>
}
