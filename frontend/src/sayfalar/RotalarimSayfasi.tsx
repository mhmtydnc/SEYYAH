import { useEffect, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api } from '../api/istemci'
import type { KayitliRota } from '../api/tipler'
import { useOturum } from '../oturum/Oturum'

export function RotalarimSayfasi() {
  const yonlendir = useNavigate()
  const { kullanici, hazir } = useOturum()
  const [rotalar, rotalarAyarla] = useState<KayitliRota[]>([])
  const [yukleniyor, yukleniyorAyarla] = useState(true)
  const [hata, hataAyarla] = useState('')

  useEffect(() => {
    if (!kullanici) return
    let etkin = true
    api.rotalarim().then((gelen) => { if (etkin) rotalarAyarla(gelen) })
      .catch((neden: unknown) => { if (etkin) hataAyarla(neden instanceof Error ? neden.message : 'Rotalar yüklenemedi.') })
      .finally(() => { if (etkin) yukleniyorAyarla(false) })
    return () => { etkin = false }
  }, [kullanici])

  async function sil(rota: KayitliRota) {
    if (!window.confirm(`“${rota.baslik}” rotasını silmek istiyor musunuz?`)) return
    hataAyarla('')
    try {
      await api.rotaSil(rota.id)
      rotalarAyarla((eskiler) => eskiler.filter((eski) => eski.id !== rota.id))
    } catch (neden) {
      hataAyarla(neden instanceof Error ? neden.message : 'Rota silinemedi.')
    }
  }

  function ac(rota: KayitliRota) {
    sessionStorage.setItem('seyyah-kayitli-rota', JSON.stringify(rota))
    yonlendir(`/?kayitli=${rota.id}`)
  }

  if (!hazir) return <main className="icerik-sayfasi"><p>Oturum doğrulanıyor…</p></main>
  if (!kullanici) return <Navigate to="/giris" replace />
  return <main className="icerik-sayfasi">
    <p className="ust-etiket">KAYITLI YOLCULUKLAR</p><h1>Rotalarım</h1>
    {hata && <div className="uyari hata" role="alert">{hata}</div>}
    {yukleniyor ? <p>Rotalar yükleniyor…</p> : rotalar.length === 0 ? <div className="bos-kart">
      <p>Henüz kayıtlı rotan yok.</p><Link to="/">Yeni bir rota oluştur</Link>
    </div> : <ul className="kayitli-liste">
      {rotalar.map((rota) => <li key={rota.id} className="kayitli-kart">
        <div><h2>{rota.baslik}</h2><p>{rota.kalkis.ad} → {rota.varis.ad}</p>
          <small>{new Date(rota.olusturulma).toLocaleDateString('tr-TR')}</small></div>
        <div className="kayitli-eylemler"><button type="button" onClick={() => ac(rota)}>Aç</button>
          <button type="button" onClick={() => void sil(rota)}>Sil</button></div>
      </li>)}
    </ul>}
  </main>
}
