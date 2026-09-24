import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Konum, RotaBacagi } from '../api/tipler'
import { kategoriAdi } from '../api/kategoriler'
import type { SiraliNokta } from '../rota/duraklar'
import { gitBaglantisi, tumRotaBaglantisi } from '../rota/haritaBaglantilari'
import { saatMetni } from '../rota/zamanCizelgesi'
import { acilisSaatiYukle, uyariMetni } from '../rota/acilisSaati'

interface YolculukPlani {
  noktalar: SiraliNokta[]
  bacaklar: RotaBacagi[]
  kalkisSaati: string
  varis: Konum
  baslangic: string
  sira: number
}

function kayitliPlan(): YolculukPlani | null {
  try {
    const metin = localStorage.getItem('seyyah-yolculuk')
    if (!metin) return null
    const plan = JSON.parse(metin) as YolculukPlani
    return Array.isArray(plan.noktalar) && Array.isArray(plan.bacaklar) && Number.isInteger(plan.sira) ? plan : null
  } catch { return null }
}

function uzaklikMetre(ilk: Pick<Konum, 'enlem' | 'boylam'>, ikinci: Pick<Konum, 'enlem' | 'boylam'>): number {
  const radyan = Math.PI / 180
  const enlemFarki = (ikinci.enlem - ilk.enlem) * radyan
  const boylamFarki = (ikinci.boylam - ilk.boylam) * radyan
  const aci = Math.sin(enlemFarki / 2) ** 2 + Math.cos(ilk.enlem * radyan) * Math.cos(ikinci.enlem * radyan) * Math.sin(boylamFarki / 2) ** 2
  return 6371000 * 2 * Math.atan2(Math.sqrt(aci), Math.sqrt(1 - aci))
}

export function YolculukSayfasi() {
  const yonlendir = useNavigate()
  const [plan, planAyarla] = useState<YolculukPlani | null>(() => kayitliPlan())
  const [konum, konumAyarla] = useState<Pick<Konum, 'enlem' | 'boylam'> | null>(null)
  const [, acilisSaatiSayaciAyarla] = useState(0)
  const siradaki = plan?.noktalar[plan.sira]
  const mesafe = siradaki && konum ? uzaklikMetre(konum, siradaki) : null

  useEffect(() => {
    if (!plan?.noktalar.some((nokta) => nokta.durak?.yer.calismaSaatleri?.trim())) return
    let etkin = true
    void acilisSaatiYukle().then(() => { if (etkin) acilisSaatiSayaciAyarla((sayi) => sayi + 1) })
    return () => { etkin = false }
  }, [plan])

  useEffect(() => {
    if (!navigator.geolocation || !siradaki) return
    const izleme = navigator.geolocation.watchPosition(
      (yer) => konumAyarla({ enlem: yer.coords.latitude, boylam: yer.coords.longitude }),
      () => konumAyarla(null), { enableHighAccuracy: false },
    )
    return () => navigator.geolocation.clearWatch(izleme)
  }, [siradaki?.enlem, siradaki?.boylam])

  function ilerle(vardi: boolean) {
    if (!plan || !siradaki) return
    const sonraki = { ...plan, sira: plan.sira + 1 }
    if (vardi) sonraki.baslangic = new Date(Date.now() + (siradaki.durak?.kalisDakika ?? 0) * 60000).toISOString()
    else sonraki.baslangic = new Date().toISOString()
    localStorage.setItem('seyyah-yolculuk', JSON.stringify(sonraki))
    planAyarla(sonraki)
  }

  function bitir() {
    localStorage.removeItem('seyyah-yolculuk')
    yonlendir('/')
  }

  function varisZamani(sira: number): Date {
    if (!plan) return new Date(NaN)
    let zaman = new Date(plan.baslangic).getTime()
    for (let konumSirasi = plan.sira; konumSirasi <= sira; konumSirasi++) {
      zaman += (plan.bacaklar[konumSirasi]?.sureSn ?? 0) * 1000
      if (konumSirasi < sira) zaman += (plan.noktalar[konumSirasi]?.durak?.kalisDakika ?? 0) * 60000
    }
    return new Date(zaman)
  }

  function durakUyarisi(sira: number): string | null {
    if (!plan) return null
    const durak = plan.noktalar[sira]?.durak
    return durak ? uyariMetni(durak.yer.calismaSaatleri, varisZamani(sira), durak.kalisDakika) : null
  }

  if (!plan) return <main className="yolculuk-sayfasi"><h1>Aktif yolculuk yok</h1><button className="birincil" onClick={() => yonlendir('/')}>Ana sayfaya dön</button></main>
  if (!siradaki) return <main className="yolculuk-sayfasi"><h1>Yolculuk tamamlandı</h1><button className="birincil" onClick={bitir}>Yolculuğu bitir</button></main>
  const toplamDurak = plan.noktalar.length
  return <main className="yolculuk-sayfasi">
    <header><p className="ust-etiket">YOLCULUK MODU</p><h1>Yoldasın.</h1><span>İlerleme: {plan.sira + 1}/{toplamDurak}</span></header>
    <section className={`siradaki-kart${mesafe !== null && mesafe <= 300 ? ' yaklasti' : ''}`}>
      <p>{mesafe !== null && mesafe <= 300 ? 'Yaklaştın' : 'Sıradaki durak'}</p>
      <h2>{siradaki.ad}</h2>
      <span>{siradaki.durak ? kategoriAdi(siradaki.durak.yer.kategori) : 'Varış noktası'}</span>
      <div className="yolculuk-bilgileri"><span>Tahmini varış <strong>{saatMetni(varisZamani(plan.sira))}</strong></span>
        <span>Kalış <strong>{siradaki.durak?.kalisDakika ?? 0} dk</strong></span></div>
      {durakUyarisi(plan.sira) && <p className="acilis-uyarisi">{durakUyarisi(plan.sira)}</p>}
      {mesafe !== null && <p>{mesafe < 1000 ? `${Math.round(mesafe)} m` : `${(mesafe / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km`} kuş uçuşu</p>}
      <a className="birincil" href={gitBaglantisi(siradaki, navigator.userAgent)} target="_blank" rel="noopener noreferrer">Git ↗</a>
      <div className="yolculuk-eylemler"><button type="button" onClick={() => ilerle(true)}>Vardım</button>
        <button type="button" onClick={() => ilerle(false)}>Bu durağı atla</button></div>
    </section>
    <section className="kalan-duraklar"><h2>Kalan duraklar</h2><ol start={plan.sira + 2}>{plan.noktalar.slice(plan.sira + 1).map((nokta, sira) =>
      <li key={`${nokta.ad}-${sira}`}><strong>{nokta.ad}</strong><span>{saatMetni(varisZamani(plan.sira + sira + 1))}</span>
        {durakUyarisi(plan.sira + sira + 1) && <small className="acilis-uyarisi">{durakUyarisi(plan.sira + sira + 1)}</small>}</li>)}</ol></section>
    <a className="tum-rota-baglantisi" href={tumRotaBaglantisi(plan.noktalar.slice(plan.sira).filter((nokta) => nokta.durak), plan.varis)} target="_blank" rel="noopener noreferrer">Tüm rotayı haritada aç ↗</a>
    <button className="bitir-dugmesi" type="button" onClick={bitir}>Yolculuğu bitir</button>
  </main>
}
