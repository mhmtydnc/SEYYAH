import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/istemci'
import { guvenliSite } from '../api/baglanti'
import { kategoriAdi } from '../api/kategoriler'
import type { DurakliRota, KayitliRota, Konum, KoridorYeri, RotaYaniti, YerTuru } from '../api/tipler'
import { KonumAlani } from '../bilesenler/KonumAlani'
import { YerDetayi } from '../bilesenler/YerDetayi'
import { RotaHaritasi } from '../harita/RotaHaritasi'
import { useOturum } from '../oturum/Oturum'
import { mesafeFarkiMetni, sureFarkiMetni, sureMetni } from './rotaBicimi'
import { duraklariSirala, rotaNoktalari, type SeciliDurak } from '../rota/duraklar'
import { varsayilanKalisSuresi } from '../rota/kalisSureleri'
import { kalkisZamani, saatMetni, yuvarlanmisKalkis, zamanCizelgesi } from '../rota/zamanCizelgesi'
import { sanalNoktalariBirlestir } from '../rota/yolculuk'
import { uyariMetni } from '../rota/acilisSaati'

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

export function AnaSayfa() {
  const yonlendir = useNavigate()
  const { kullanici } = useOturum()
  const [kalkis, kalkisAyarla] = useState<Konum | null>(null)
  const [varis, varisAyarla] = useState<Konum | null>(null)
  const [yaricap, yaricapAyarla] = useState(5000)
  const [rota, rotaAyarla] = useState<RotaYaniti | null>(null)
  const [seciliRotaSirasi, seciliRotaSirasiAyarla] = useState(0)
  const [etkinTur, etkinTurAyarla] = useState<YerTuru>('gezi')
  const [seciliYer, seciliYerAyarla] = useState<KoridorYeri | null>(null)
  const [detayYeri, detayYeriAyarla] = useState<KoridorYeri | null>(null)
  const [yukleniyor, yukleniyorAyarla] = useState(false)
  const [kaydediliyor, kaydediliyorAyarla] = useState(false)
  const [hata, hataAyarla] = useState('')
  const [bildirim, bildirimAyarla] = useState('')
  const [duraklar, duraklarAyarla] = useState<SeciliDurak[]>([])
  const [durakliRota, durakliRotaAyarla] = useState<DurakliRota | null>(null)
  const [durakHatasi, durakHatasiAyarla] = useState('')
  const [durakYukleniyor, durakYukleniyorAyarla] = useState(false)
  const [kalkisSaati, kalkisSaatiAyarla] = useState(() => yuvarlanmisKalkis())
  const durakIstegi = useRef(0)
  const eklenmeSirasi = useRef(0)
  const istekSirasi = useRef(0)
  const ilkSorguYapildi = useRef(false)
  const haritaKutusu = useRef<HTMLDivElement>(null)
  const listeSayfaKonumu = useRef(0)
  const seciliRota = rota?.rotalar.find((secenek) => secenek.sira === seciliRotaSirasi) ?? null
  const anaRota = rota?.rotalar.find((secenek) => secenek.sira === 0) ?? null
  const siraliNoktalar = useMemo(() => seciliRota ? duraklariSirala(duraklar, seciliRota) : [], [duraklar, seciliRota])
  const haritaDuraklari = useMemo(() => siraliNoktalar.filter((nokta) => nokta.durak), [siraliNoktalar])
  const planNoktalari = [...siraliNoktalar, ...(varis ? [{ ...varis }] : [])]
  const bacaklar = durakliRota?.bacaklar ?? []
  const cizelge = zamanCizelgesi(kalkisZamani(kalkisSaati), planNoktalari, bacaklar)
  const toplamKalis = duraklar.reduce((toplam, durak) => toplam + durak.kalisDakika, 0)

  function durakEklenebilir(yer: KoridorYeri): boolean {
    if (duraklar.some((durak) => durak.yer.id === yer.id)) return true
    return duraklar.length < 10
  }

  function durakDegistir(yer: KoridorYeri) {
    duraklarAyarla((eskiler) => {
      if (eskiler.some((durak) => durak.yer.id === yer.id)) return eskiler.filter((durak) => durak.yer.id !== yer.id)
      if (!durakEklenebilir(yer)) return eskiler
      return [...eskiler, { yer, kalisDakika: varsayilanKalisSuresi(yer.kategori), eklenmeSirasi: eklenmeSirasi.current++ }]
    })
  }

  useEffect(() => {
    durakIstegi.current++
    durakliRotaAyarla(null)
    durakHatasiAyarla('')
    durakYukleniyorAyarla(false)
    if (!seciliRota || !kalkis || !varis || !duraklar.length) return
    if (duraklariSirala(duraklar, seciliRota).length > 10) {
      durakHatasiAyarla('Bu alternatifte ara şehir de rota noktasıdır. Duraklı rotayı hesaplamak için bir durağı çıkarın.')
      return
    }
    const sira = durakIstegi.current
    const denetleyici = new AbortController()
    const zamanlayici = window.setTimeout(() => {
      durakYukleniyorAyarla(true)
      api.durakliRotaOlustur(rotaNoktalari(kalkis, varis, duraklariSirala(duraklar, seciliRota)), denetleyici.signal)
        .then((gelen) => { if (sira === durakIstegi.current) durakliRotaAyarla(gelen) })
        .catch((neden: unknown) => { if (sira === durakIstegi.current && !(neden instanceof Error && neden.name === 'AbortError')) durakHatasiAyarla(neden instanceof Error ? neden.message : 'Duraklı rota oluşturulamadı.') })
        .finally(() => { if (sira === durakIstegi.current) durakYukleniyorAyarla(false) })
    }, 400)
    return () => { window.clearTimeout(zamanlayici); denetleyici.abort() }
  }, [seciliRota, kalkis, varis, duraklar])

  function rotaSec(sira: number) {
    seciliRotaSirasiAyarla(sira)
    seciliYerAyarla(null)
    detayYeriAyarla(null)
  }

  function detayiAc(yer: KoridorYeri) {
    if (!detayYeri) listeSayfaKonumu.current = window.scrollY
    seciliYerAyarla(yer)
    detayYeriAyarla(yer)
    if (window.innerWidth < 768) haritaKutusu.current?.scrollIntoView({ block: 'start' })
  }

  function detayiKapat() {
    detayYeriAyarla(null)
    if (window.innerWidth < 768) window.requestAnimationFrame(() => window.scrollTo(0, listeSayfaKonumu.current))
  }

  async function rotaGetir(baslangic: Konum, bitis: Konum, yaricapM: number, kayit?: KayitliRota) {
    const sira = ++istekSirasi.current
    yukleniyorAyarla(true)
    hataAyarla('')
    bildirimAyarla('')
    rotaAyarla(null)
    seciliRotaSirasiAyarla(0)
    seciliYerAyarla(null)
    detayYeriAyarla(null)
    try {
      const gelen = await api.rotaOlustur(baslangic, bitis, yaricapM)
      if (sira === istekSirasi.current) {
        seciliRotaSirasiAyarla(gelen.rotalar.find((secenek) => secenek.uzerinden === (kayit?.uzerinden?.ad ?? null))?.sira ?? 0)
        rotaAyarla(gelen)
        if (kayit) {
          duraklarAyarla((kayit.duraklar ?? []).map((yer, sira) => ({ yer, kalisDakika: varsayilanKalisSuresi(yer.kategori), eklenmeSirasi: sira })))
          eklenmeSirasi.current = kayit.duraklar?.length ?? 0
        }
      }
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
    const kayitli = parametreler.get('kayitli')
    if (kayitli) {
      const saklanan = sessionStorage.getItem('seyyah-kayitli-rota')
      if (!saklanan) { hataAyarla('Kayıtlı rota bu sekmede bulunamadı. Rotalarım sayfasından yeniden açın.'); return }
      try {
        const kayit = JSON.parse(saklanan) as KayitliRota
        if (String(kayit.id) !== kayitli) throw new Error('Kayıtlı rota eşleşmiyor.')
        sessionStorage.removeItem('seyyah-kayitli-rota')
        kalkisAyarla(kayit.kalkis)
        varisAyarla(kayit.varis)
        void rotaGetir(kayit.kalkis, kayit.varis, 5000, kayit)
      } catch (neden) { hataAyarla(neden instanceof Error ? neden.message : 'Kayıtlı rota açılamadı.') }
      return
    }
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
    seciliRotaSirasiAyarla(0)
    seciliYerAyarla(null)
    detayYeriAyarla(null)
    duraklarAyarla([])
    bildirimAyarla('')
  }

  async function kaydet() {
    if (!rota || !kalkis || !varis) return
    kaydediliyorAyarla(true)
    hataAyarla('')
    try {
      const ek = seciliRota?.sira !== 0 && seciliRota?.uzerinden ? ` (${seciliRota.uzerinden} üzerinden)` : ''
      await api.rotaKaydet({ baslik: `${kalkis.ad} - ${varis.ad}${ek}`, kalkis, varis,
        uzerinden: seciliRota?.araNokta ?? null,
        duraklar: siraliNoktalar.filter((nokta) => nokta.durak).map((nokta) => {
          const { id, ad, kategori, enlem, boylam } = nokta.durak!.yer
          return { id, ad, kategori, enlem, boylam }
        }) })
      bildirimAyarla('Rota kaydedildi.')
    } catch (neden) {
      hataAyarla(neden instanceof Error ? neden.message : 'Rota kaydedilemedi.')
    } finally {
      kaydediliyorAyarla(false)
    }
  }

  function yolculugaBasla() {
    if (!varis || !durakliRota || !duraklar.length) return
    const { noktalar, bacaklar } = sanalNoktalariBirlestir(planNoktalari, durakliRota.bacaklar)
    localStorage.setItem('seyyah-yolculuk', JSON.stringify({
      noktalar, bacaklar, kalkisSaati,
      varis, baslangic: kalkisZamani(kalkisSaati).toISOString(), sira: 0,
    }))
    yonlendir('/yolculuk')
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
    {durakHatasi && <div className="uyari hata" role="alert">{durakHatasi}</div>}

    {seciliRota && <div className={rota && rota.rotalar.length > 1 ? 'rota-secim-alani' : 'rota-ozeti'}>
      {rota && rota.rotalar.length > 1 ? <div className="rota-kartlari" aria-label="Alternatif rotalar">
        {rota.rotalar.map((secenek) => {
          const sureFarki = anaRota && secenek.sira !== 0 ? sureFarkiMetni(secenek.sureSn - anaRota.sureSn) : null
          const mesafeFarki = anaRota && secenek.sira !== 0 ? mesafeFarkiMetni(secenek.mesafeM - anaRota.mesafeM) : null
          return <button key={secenek.sira} type="button" className={`rota-karti${secenek.sira === seciliRotaSirasi ? ' secili' : ''}`}
            aria-pressed={secenek.sira === seciliRotaSirasi} onClick={() => rotaSec(secenek.sira)}>
            <strong>{secenek.ad}</strong>
            <span>{(secenek.mesafeM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km · {sureMetni(secenek.sureSn)}</span>
            {(sureFarki || mesafeFarki) && <small>{[sureFarki, mesafeFarki].filter(Boolean).join(' · ')}</small>}
          </button>
        })}
      </div> : <>
        <div><span>Toplam mesafe</span><strong>{(seciliRota.mesafeM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</strong></div>
        <div><span>Tahmini süre</span><strong>{sureMetni(seciliRota.sureSn)}</strong></div>
      </>}
      <div className="kaydet-alani">{kullanici
        ? <button type="button" onClick={() => void kaydet()} disabled={kaydediliyor}>{kaydediliyor ? 'Kaydediliyor…' : 'Rotayı kaydet'}</button>
        : <Link to="/giris">Rotayı kaydetmek için giriş yap</Link>}</div>
    </div>}

    {seciliRota && duraklar.length > 0 && <section className="durak-ozeti" aria-label="Duraklı rota özeti">
      <div><span>Toplam mesafe</span><strong>{((durakliRota?.mesafeM ?? seciliRota.mesafeM) / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</strong></div>
      <div><span>Yol süresi</span><strong>{sureMetni(durakliRota?.sureSn ?? seciliRota.sureSn)}</strong></div>
      <div><span>Sapma</span><strong>{durakliRota ? `${sureFarkiMetni(durakliRota.sureSn - seciliRota.sureSn)} yol` : 'Hesaplanıyor…'}</strong></div>
      <div><span>Toplam kalış</span><strong>{toplamKalis} dk</strong></div>
      <button className="birincil" type="button" onClick={yolculugaBasla} disabled={!durakliRota || durakYukleniyor}>Yolculuğa başla</button>
    </section>}

    <section className="sonuc-alani" aria-label="Rota ve yerler">
      <div className="harita-kutusu" ref={haritaKutusu}><RotaHaritasi rota={rota} seciliRota={seciliRota} rotaSec={rotaSec} kalkis={kalkis} varis={varis} seciliYer={seciliYer} detayiAc={detayiAc}
        durakliRota={durakliRota} siraliDuraklar={haritaDuraklari} duraklar={duraklar} durakDegistir={durakDegistir} durakEklenebilir={durakEklenebilir} /></div>
      <aside className={`yer-paneli${detayYeri ? ' detay-acik' : ''}`}>
        {detayYeri && <YerDetayi key={detayYeri.id} yer={detayYeri} kapat={detayiKapat}
          durakMi={duraklar.some((durak) => durak.yer.id === detayYeri.id)} eklenebilir={durakEklenebilir(detayYeri)}
          durakDegistir={() => durakDegistir(detayYeri)} />}
        <div className="yer-panel-liste" hidden={!!detayYeri}>
        <div className="panel-baslik"><h2>Yol üstünde</h2><span>{rota ? 'Rotandaki noktalar' : 'Önce bir rota oluştur'}</span></div>
        {duraklar.length > 0 && <section className="duraklarim" aria-label="Duraklarım">
          <h3>Duraklarım</h3>
          <label className="kalkis-saati">Kalkış saati <input type="time" value={kalkisSaati} onChange={(olay) => kalkisSaatiAyarla(olay.target.value)} /></label>
          <ol>{siraliNoktalar.filter((nokta) => nokta.durak).map((nokta, sira) => {
            const zaman = cizelge.find((satir) => satir.nokta === nokta)?.varis
            const uyari = durakliRota && zaman ? uyariMetni(nokta.durak!.yer.calismaSaatleri, zaman, nokta.durak!.kalisDakika) : null
            return <li key={nokta.durak!.yer.id}>
              <div><strong>{sira + 1}. {nokta.ad}</strong><small>Varış: {durakliRota && zaman ? saatMetni(zaman) : 'Hesaplanıyor…'}</small>
                {nokta.durak!.yer.calismaSaatleri && <small>Çalışma saatleri: {nokta.durak!.yer.calismaSaatleri}</small>}
                {uyari && <small className="acilis-uyarisi">{uyari}</small>}</div>
              <label>Kalış (dk)<input type="number" min="0" max="1440" value={nokta.durak!.kalisDakika}
                onChange={(olay) => duraklarAyarla((eskiler) => eskiler.map((durak) => durak.yer.id === nokta.durak!.yer.id
                  ? { ...durak, kalisDakika: Math.max(0, Math.min(1440, Number(olay.target.value) || 0)) } : durak))} /></label>
              <button type="button" onClick={() => duraklarAyarla((eskiler) => eskiler.filter((durak) => durak.yer.id !== nokta.durak!.yer.id))}>Çıkar</button>
            </li>
          })}</ol>
          <p className="varis-saati">Varış: {varis?.ad} · {durakliRota && cizelge.length ? saatMetni(cizelge[cizelge.length - 1].varis) : 'Hesaplanıyor…'}</p>
        </section>}
        <div className="sekmeler" role="tablist" aria-label="Yer türü">
          {turler.map((tur) => <button key={tur.kimlik} type="button" role="tab" aria-selected={etkinTur === tur.kimlik}
            className={etkinTur === tur.kimlik ? 'etkin' : ''} onClick={() => etkinTurAyarla(tur.kimlik)}>
            {tur.ad} <span>{seciliRota?.yerler[tur.kimlik].length ?? 0}</span>
          </button>)}
        </div>
        <div className="yer-listesi" role="tabpanel">
          {!seciliRota ? <p className="bos-metin">Seçtiğin rota boyunca keşfedilecek yerler burada görünecek.</p>
            : seciliRota.yerler[etkinTur].length === 0 ? <p className="bos-metin">Bu türde yer bulunamadı.</p>
              : seciliRota.yerler[etkinTur].map((yer) => <div className="yer-karti" key={yer.id}>
                <button type="button" className="yer-sec" onClick={() => detayiAc(yer)}>
                  <strong>{yer.ad}</strong><span>{kategoriAdi(yer.kategori)} · Yoldan {(yer.yolaUzaklikM / 1000).toLocaleString('tr-TR', { maximumFractionDigits: 1 })} km</span>
                </button>
                {yer.calismaSaatleri && <small>Çalışma saatleri: {yer.calismaSaatleri}</small>}
                {yer.ucret != null && <small>Ücret: {yer.ucret}</small>}
                {guvenliSite(yer.website) && <a href={guvenliSite(yer.website)!} target="_blank" rel="noopener noreferrer">Web sitesi ↗</a>}
                <button type="button" className="durak-dugmesi" disabled={!durakEklenebilir(yer)}
                  onClick={() => durakDegistir(yer)}>{duraklar.some((durak) => durak.yer.id === yer.id) ? 'Duraktan çıkar' : '+ Durak ekle'}</button>
              </div>)}
        </div>
        </div>
      </aside>
    </section>
  </main>
}
