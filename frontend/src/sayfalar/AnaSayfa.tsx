import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/istemci'
import { kategoriAdi } from '../api/kategoriler'
import type { Konum, KoridorYeri, RotaYaniti, YerTuru } from '../api/tipler'
import { KonumAlani } from '../bilesenler/KonumAlani'
import { RotaHaritasi } from '../harita/RotaHaritasi'
import { useOturum } from '../oturum/Oturum'

const turler: { kimlik: YerTuru; ad: string }[] = [
  { kimlik: 'gezi', ad: 'Gezi' }, { kimlik: 'mola', ad: 'Mola' }, { kimlik: 'destek', ad: 'Destek' },
]

function sorgudanKonum(parametreler: URLSearchParams, onEk: string): Konum | null {
  const ad = parametreler.get(`${onEk}Ad`)
  const enlem = Number(parametreler.get(`${onEk}Enlem`))
  const boylam = Number(parametreler.get(`${onEk}Boylam`))
  if (!ad || !parametreler.has(`${onEk}Enlem`) || !parametreler.has(`${onEk}Boylam`)
    || !Number.isFinite(enlem) || !Number.isFinite(boylam)
    || Math.abs(enlem) > 90 || Math.abs(boylam) > 180) return null
  return { ad, enlem, boylam }
}

function sureMetni(saniye: number) {
  const dakika = Math.round(saniye / 60)
  return `${Math.floor(dakika / 60)} sa ${dakika % 60} dk`
}

function guvenliSite(adres: string | null) {
  if (!adres) return null
  try {
    const baglanti = new URL(adres)
    return ['http:', 'https:'].includes(baglanti.protocol) ? baglanti.href : null
  } catch {
    return null
  }
}

export function AnaSayfa() {
  const { kullanici } = useOturum()
  const [kalkis, kalkisAyarla] = useState<Konum | null>(null)
  const [varis, varisAyarla] = useState<Konum | null>(null)
  const [yaricap, yaricapAyarla] = useState(5000)
  const [rota, rotaAyarla] = useState<RotaYaniti | null>(null)
  const [etkinTur, etkinTurAyarla] = useState<YerTuru>('gezi')
  const [seciliYer, seciliYerAyarla] = useState<KoridorYeri | null>(null)
  const [yukleniyor, yukleniyorAyarla] = useState(false)
  const [kaydediliyor, kaydediliyorAyarla] = useState(false)
  const [hata, hataAyarla] = useState('')
  const [bildirim, bildirimAyarla] = useState('')
  const istekSirasi = useRef(0)
  const ilkSorguYapildi = useRef(false)

  async function rotaGetir(baslangic: Konum, bitis: Konum, yaricapM: number) {
    const sira = ++istekSirasi.current
    yukleniyorAyarla(true)
    hataAyarla('')
    bildirimAyarla('')
    rotaAyarla(null)
    seciliYerAyarla(null)
    try {
      const gelen = await api.rotaOlustur(baslangic, bitis, yaricapM)
      if (sira === istekSirasi.current) rotaAyarla(gelen)
    } catch (neden) {
      if (sira === istekSirasi.current) hataAyarla(neden instanceof Error ? neden.message : 'Rota oluşturulamadı.')
    } finally {
      if (sira === istekSirasi.current) yukleniyorAyarla(false)
    }
  }

  useEffect(() => {
    if (ilkSorguYapildi.current) return
    ilkSorguYapildi.current = true
    const parametreler = new URLSearchParams(window.location.search)
    const baslangic = sorgudanKonum(parametreler, 'kalkis')
    const bitis = sorgudanKonum(parametreler, 'varis')
    if (!baslangic || !bitis) return
    const yaricapM = Number(parametreler.get('yaricap'))
    const secilenYaricap = [1000, 5000, 10000, 20000].includes(yaricapM) ? yaricapM : 5000
    kalkisAyarla(baslangic)
    varisAyarla(bitis)
    yaricapAyarla(secilenYaricap)
    void rotaGetir(baslangic, bitis, secilenYaricap)
  }, [])

  function konumDegistir(tur: 'kalkis' | 'varis', konum: Konum | null) {
    istekSirasi.current++
    yukleniyorAyarla(false)
    if (tur === 'kalkis') kalkisAyarla(konum)
    else varisAyarla(konum)
    rotaAyarla(null)
    seciliYerAyarla(null)
    bildirimAyarla('')
  }

  async function kaydet() {
    if (!rota || !kalkis || !varis) return
    kaydediliyorAyarla(true)
    hataAyarla('')
    try {
      await api.rotaKaydet({ baslik: `${kalkis.ad} - ${varis.ad}`, kalkis, varis })
      bildirimAyarla('Rota kaydedildi.')
    } catch (neden) {
      hataAyarla(neden instanceof Error ? neden.message : 'Rota kaydedilemedi.')
    } finally {
      kaydediliyorAyarla(false)
    }
  }

  return <main className="ana-sayfa">
    <section className="giris-bolumu">
      <div>
        <p className="ust-etiket">YOLCULUĞUNU PLANLA</p>
        <h1>Yol boyunca keşfet.</h1>
        <p>Rotanı oluştur, yol üstündeki gezi, mola ve destek noktalarını gör.</p>
      </div>
      <form className="rota-formu" onSubmit={(event) => { event.preventDefault(); if (kalkis && varis) void rotaGetir(kalkis, varis, yaricap) }}>
        <KonumAlani etiket="Kalkış" konum={kalkis} onSec={(konum) => konumDegistir('kalkis', konum)} />
        <KonumAlani etiket="Varış" konum={varis} onSec={(konum) => konumDegistir('varis', konum)} />
        <div className="yaricap-alani"><label htmlFor="yaricap">Rota çevresi</label>
          <select id="yaricap" value={yaricap} onChange={(event) => { istekSirasi.current++; yaricapAyarla(Number(event.target.value)); rotaAyarla(null); yukleniyorAyarla(false) }}>
            {[1, 5, 10, 20].map((km) => <option value={km * 1000} key={km}>{km} km</option>)}
          </select>
        </div>
        <button className="birincil" disabled={!kalkis || !varis || yukleniyor} type="submit">
          {yukleniyor ? 'Rota oluşturuluyor…' : 'Rota oluştur'}
        </button>
      </form>
    </section>

    {hata && <div className="uyari hata" role="alert">{hata}</div>}
    {bildirim && <div className="uyari basari" role="status">{bildirim}</div>}

    {rota && <div className="rota-ozeti">
      <div><span>Toplam mesafe</span><strong>{(rota.mesafeM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</strong></div>
      <div><span>Tahmini süre</span><strong>{sureMetni(rota.sureSn)}</strong></div>
      <div className="kaydet-alani">{kullanici
        ? <button type="button" onClick={() => void kaydet()} disabled={kaydediliyor}>{kaydediliyor ? 'Kaydediliyor…' : 'Rotayı kaydet'}</button>
        : <Link to="/giris">Rotayı kaydetmek için giriş yap</Link>}</div>
    </div>}

    <section className="sonuc-alani" aria-label="Rota ve yerler">
      <div className="harita-kutusu"><RotaHaritasi rota={rota} kalkis={kalkis} varis={varis} seciliYer={seciliYer} /></div>
      <aside className="yer-paneli">
        <div className="panel-baslik"><h2>Yol üstünde</h2><span>{rota ? 'Rotandaki noktalar' : 'Önce bir rota oluştur'}</span></div>
        <div className="sekmeler" role="tablist" aria-label="Yer türü">
          {turler.map((tur) => <button key={tur.kimlik} type="button" role="tab" aria-selected={etkinTur === tur.kimlik}
            className={etkinTur === tur.kimlik ? 'etkin' : ''} onClick={() => etkinTurAyarla(tur.kimlik)}>
            {tur.ad} <span>{rota?.yerler[tur.kimlik].length ?? 0}</span>
          </button>)}
        </div>
        <div className="yer-listesi" role="tabpanel">
          {!rota ? <p className="bos-metin">Seçtiğin rota boyunca keşfedilecek yerler burada görünecek.</p>
            : rota.yerler[etkinTur].length === 0 ? <p className="bos-metin">Bu türde yer bulunamadı.</p>
              : rota.yerler[etkinTur].map((yer) => <div className="yer-karti" key={yer.id}>
                <button type="button" className="yer-sec" onClick={() => seciliYerAyarla(yer)}>
                  <strong>{yer.ad}</strong><span>{kategoriAdi(yer.kategori)} · Yoldan {(yer.yolaUzaklikM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</span>
                </button>
                {yer.calismaSaatleri && <small>Çalışma saatleri: {yer.calismaSaatleri}</small>}
                {yer.ucret != null && <small>Ücret: {yer.ucret}</small>}
                {guvenliSite(yer.website) && <a href={guvenliSite(yer.website)!} target="_blank" rel="noopener noreferrer">Web sitesi ↗</a>}
              </div>)}
        </div>
      </aside>
    </section>
  </main>
}
